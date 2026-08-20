# Week3 JMeter 实测闭环报告

## 运行命令

```bash
cd /Users/baozhashizitou/Documents/pta
JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz \
  scripts/run-week3-jmeter.sh
```

脚本会在 `target/` 下下载并缓存 Apache JMeter 5.6.3，校验 sha512，依次运行登录并发、核心读链路和写入链路，并生成 `.jtl` 与 HTML dashboard。

## 本次结果

- 运行时间：2026-08-17 09:21:57 CST
- Base URL：`http://localhost:8080`
- Readiness：`http://localhost:8080/actuator/health/readiness`
- 原始汇总：`target/jmeter-results/week3-jmeter-summary.md`

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s | JTL | HTML report |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 82.07 | 91 | 641 | 10.16 | `target/jmeter-results/jmeter-login-concurrency.jtl` | `target/jmeter-results/jmeter-login-concurrency-html` |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 36.40 | 67 | 1409 | 29.64 | `target/jmeter-results/jmeter-week3-core-business.jtl` | `target/jmeter-results/jmeter-week3-core-business-html` |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 37.35 | 110 | 152 | 5.32 | `target/jmeter-results/jmeter-week3-write-chain.jtl` | `target/jmeter-results/jmeter-week3-write-chain-html` |

## 修复说明

- 初始闭环红线：`target/jmeter-results/*.jtl` 和 `week3-jmeter-summary.md` 不存在，说明只有模板没有实测结果。
- 首次完整运行发现核心读链路 2 个 401，原因是同一 `admin` 账号在多线程中反复登录，应用按最新 jti 判定当前会话，后登录会使旧 token 失效。
- `jmeter-week3-core-business.jmx` 改为 `SetupThreadGroup` 登录一次并发布 `WEEK3_ADMIN_TOKEN`，主线程组只并发查询工作日志、绩效和操作日志。
- `jmeter-week3-write-chain.jmx` 同样改为 setup 管理员登录，写入线程复用管理员 token，避免审批和绩效写入被同账号并发登录干扰。
- `scripts/run-week3-jmeter.sh` 对 JMeter sample error 执行非零退出，因此后续压测不再只停留在 XML 校验。
