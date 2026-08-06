#!/usr/bin/env sh
set -eu

MAVEN_VERSION="${MAVEN_VERSION:-3.9.11}"
MAVEN_CACHE_ROOT="${MAVEN_CACHE_ROOT:-${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists}"
MAVEN_HOME="$MAVEN_CACHE_ROOT/apache-maven-$MAVEN_VERSION"
ARCHIVE="$MAVEN_CACHE_ROOT/apache-maven-$MAVEN_VERSION-bin.zip"
URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$MAVEN_CACHE_ROOT"
  if [ ! -f "$ARCHIVE" ]; then
    echo "Downloading Apache Maven $MAVEN_VERSION..."
    curl -fsSL "$URL" -o "$ARCHIVE"
  fi
  unzip -q "$ARCHIVE" -d "$MAVEN_CACHE_ROOT"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
