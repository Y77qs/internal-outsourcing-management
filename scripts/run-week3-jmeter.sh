#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JMETER_VERSION="${JMETER_VERSION:-5.6.3}"
JMETER_ARCHIVE="apache-jmeter-${JMETER_VERSION}.tgz"
JMETER_URL="${JMETER_URL:-https://dlcdn.apache.org/jmeter/binaries/${JMETER_ARCHIVE}}"
JMETER_FALLBACK_URL="${JMETER_FALLBACK_URL:-https://archive.apache.org/dist/jmeter/binaries/${JMETER_ARCHIVE}}"
JMETER_HOME="${JMETER_HOME:-${ROOT_DIR}/target/apache-jmeter-${JMETER_VERSION}}"
JMETER_BIN="${JMETER_HOME}/bin/jmeter"
JMETER_CACHE_DIR="${JMETER_CACHE_DIR:-${ROOT_DIR}/target/jmeter-cache}"
OUT_DIR="${JMETER_OUT_DIR:-${ROOT_DIR}/target/jmeter-results}"

HOST="${HOST:-localhost}"
PORT="${PORT:-8080}"
BASE_URL="http://${HOST}:${PORT}"
READINESS_URL="${READINESS_URL:-${BASE_URL}/actuator/health/readiness}"

LOGIN_THREADS="${LOGIN_THREADS:-20}"
LOGIN_LOOPS="${LOGIN_LOOPS:-5}"
CORE_THREADS="${CORE_THREADS:-20}"
CORE_LOOPS="${CORE_LOOPS:-5}"
WRITE_THREADS="${WRITE_THREADS:-5}"
WRITE_LOOPS="${WRITE_LOOPS:-1}"

ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
OUTSOURCER_PASSWORD="${OUTSOURCER_PASSWORD:-Tester@123456}"
DEPARTMENT_ID="${DEPARTMENT_ID:-2}"
PROJECT_ID="${PROJECT_ID:-1}"

ensure_jmeter() {
  if [[ -x "${JMETER_BIN}" ]]; then
    return
  fi

  mkdir -p "${JMETER_CACHE_DIR}" "${ROOT_DIR}/target"
  local archive_path="${JMETER_CACHE_DIR}/${JMETER_ARCHIVE}"
  local checksum_path="${archive_path}.sha512"

  download_with_fallback "${JMETER_URL}" "${JMETER_FALLBACK_URL}" "${archive_path}"
  download_with_fallback "${JMETER_URL}.sha512" "${JMETER_FALLBACK_URL}.sha512" "${checksum_path}"

  if ! (cd "${JMETER_CACHE_DIR}" && shasum -a 512 -c "$(basename "${checksum_path}")"); then
    rm -f "${archive_path}" "${checksum_path}"
    download_with_fallback "${JMETER_URL}" "${JMETER_FALLBACK_URL}" "${archive_path}"
    download_with_fallback "${JMETER_URL}.sha512" "${JMETER_FALLBACK_URL}.sha512" "${checksum_path}"
    (cd "${JMETER_CACHE_DIR}" && shasum -a 512 -c "$(basename "${checksum_path}")")
  fi

  rm -rf "${JMETER_HOME}"
  tar -xzf "${archive_path}" -C "${ROOT_DIR}/target"
}

download_with_fallback() {
  local primary_url="$1"
  local fallback_url="$2"
  local destination="$3"

  if [[ -s "${destination}" ]]; then
    return
  fi

  if ! curl -fL --retry 3 --connect-timeout 10 -C - "${primary_url}" -o "${destination}"; then
    rm -f "${destination}"
    curl -fL --retry 3 --connect-timeout 10 -C - "${fallback_url}" -o "${destination}"
  fi
}

ensure_app_ready() {
  curl -fsS "${READINESS_URL}" >/dev/null
}

run_plan() {
  local plan_name="$1"
  local plan_path="$2"
  local threads="$3"
  local loops="$4"
  local jtl_path="${OUT_DIR}/${plan_name}.jtl"
  local html_dir="${OUT_DIR}/${plan_name}-html"

  rm -f "${jtl_path}"
  rm -rf "${html_dir}"
  mkdir -p "${html_dir}"

  "${JMETER_BIN}" -n \
    -t "${plan_path}" \
    -JHOST="${HOST}" \
    -JPORT="${PORT}" \
    -JTHREADS="${threads}" \
    -JLOOPS="${loops}" \
    -JADMIN_USERNAME="${ADMIN_USERNAME}" \
    -JADMIN_PASSWORD="${ADMIN_PASSWORD}" \
    -JOUTSOURCER_PASSWORD="${OUTSOURCER_PASSWORD}" \
    -JDEPARTMENT_ID="${DEPARTMENT_ID}" \
    -JPROJECT_ID="${PROJECT_ID}" \
    -l "${jtl_path}" \
    -e -o "${html_dir}"
}

metric_from_jtl() {
  local jtl_path="$1"
  local metric="$2"
  awk -F, -v metric="${metric}" '
    NR == 1 {
      for (i = 1; i <= NF; i++) {
        column[$i] = i
      }
      next
    }
    {
      samples++
      elapsed = $(column["elapsed"]) + 0
      timestamp = $(column["timeStamp"]) + 0
      success = $(column["success"])
      sum += elapsed
      if (elapsed > max) {
        max = elapsed
      }
      if (min_start == 0 || timestamp < min_start) {
        min_start = timestamp
      }
      end_time = timestamp + elapsed
      if (end_time > max_end) {
        max_end = end_time
      }
      if (success != "true") {
        errors++
      }
    }
    END {
      duration = max_end - min_start
      if (duration <= 0) {
        duration = 1
      }
      if (metric == "samples") {
        print samples + 0
      } else if (metric == "errors") {
        print errors + 0
      } else if (metric == "avg") {
        printf "%.2f", samples ? sum / samples : 0
      } else if (metric == "max") {
        print max + 0
      } else if (metric == "throughput") {
        printf "%.2f", samples * 1000 / duration
      }
    }
  ' "${jtl_path}"
}

p95_from_jtl() {
  local jtl_path="$1"
  local samples="$2"
  if [[ "${samples}" -eq 0 ]]; then
    echo "0"
    return
  fi
  awk -F, '
    NR == 1 {
      for (i = 1; i <= NF; i++) {
        if ($i == "elapsed") {
          elapsed = i
        }
      }
      next
    }
    {
      print $elapsed
    }
  ' "${jtl_path}" \
    | sort -n \
    | awk -v samples="${samples}" '
      BEGIN {
        p95_index = int((samples * 95 + 99) / 100)
        if (p95_index < 1) {
          p95_index = 1
        }
      }
      NR == p95_index {
        print
        found = 1
        exit
      }
      END {
        if (!found) {
          print 0
        }
      }
    '
}

append_summary_row() {
  local summary_path="$1"
  local plan_name="$2"
  local jtl_path="${OUT_DIR}/${plan_name}.jtl"
  local html_dir="${OUT_DIR}/${plan_name}-html"
  local samples errors avg max throughput p95 error_rate

  samples="$(metric_from_jtl "${jtl_path}" samples)"
  errors="$(metric_from_jtl "${jtl_path}" errors)"
  avg="$(metric_from_jtl "${jtl_path}" avg)"
  max="$(metric_from_jtl "${jtl_path}" max)"
  throughput="$(metric_from_jtl "${jtl_path}" throughput)"
  p95="$(p95_from_jtl "${jtl_path}" "${samples}")"
  error_rate="$(awk -v errors="${errors}" -v samples="${samples}" 'BEGIN { printf "%.2f", samples ? errors * 100 / samples : 0 }')"

  printf '| `%s` | %s | %s | %s%% | %s | %s | %s | %s | `%s` | `%s` |\n' \
    "${plan_name}" "${samples}" "${errors}" "${error_rate}" "${avg}" "${p95}" "${max}" "${throughput}" \
    "${jtl_path#${ROOT_DIR}/}" "${html_dir#${ROOT_DIR}/}" >> "${summary_path}"

  if [[ "${samples}" -eq 0 ]]; then
    echo "JMeter plan ${plan_name} produced zero samples" >&2
    return 1
  fi
  if [[ "${errors}" -ne 0 ]]; then
    echo "JMeter plan ${plan_name} produced ${errors} failed samples" >&2
    return 1
  fi
}

main() {
  ensure_jmeter
  ensure_app_ready

  mkdir -p "${OUT_DIR}"

  run_plan "jmeter-login-concurrency" "${ROOT_DIR}/docs/jmeter-login-concurrency.jmx" \
    "${LOGIN_THREADS}" "${LOGIN_LOOPS}"
  run_plan "jmeter-week3-core-business" "${ROOT_DIR}/docs/jmeter-week3-core-business.jmx" \
    "${CORE_THREADS}" "${CORE_LOOPS}"
  run_plan "jmeter-week3-write-chain" "${ROOT_DIR}/docs/jmeter-week3-write-chain.jmx" \
    "${WRITE_THREADS}" "${WRITE_LOOPS}"

  local summary_path="${OUT_DIR}/week3-jmeter-summary.md"
  {
    echo "# Week3 JMeter Run Summary"
    echo
    echo "- Run time: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "- Base URL: ${BASE_URL}"
    echo "- JMeter version: ${JMETER_VERSION}"
    echo "- Readiness URL: ${READINESS_URL}"
    echo
    echo "| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s | JTL | HTML report |"
    echo "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |"
  } > "${summary_path}"

  local failed=0
  append_summary_row "${summary_path}" "jmeter-login-concurrency" || failed=1
  append_summary_row "${summary_path}" "jmeter-week3-core-business" || failed=1
  append_summary_row "${summary_path}" "jmeter-week3-write-chain" || failed=1

  cat "${summary_path}"
  return "${failed}"
}

main "$@"
