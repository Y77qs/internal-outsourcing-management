# JMeter Week3 压测材料记录

## 压测目标

验证 Week3 调整后的应用在多人登录、核心读链路和有状态写入链路下能够正常返回统一业务响应，并为正式验收生成 `.jtl` 结果提供可直接运行的模板。

## 压测脚本

| 文件 | 场景 |
| --- | --- |
| `docs/implementation/jmeter-login-concurrency.jmx` | 20 个线程、10 秒 ramp-up、每线程 5 次登录 |
| `docs/implementation/jmeter-week3-core-business.jmx` | setup 管理员登录后并发查询工作日志、绩效列表、操作日志关键词检索 |
| `docs/implementation/jmeter-week3-write-chain.jmx` | 参数化注册外包人员、登录、提交上岗申请、审批、提交工作日志、新增/修改绩效、查询操作日志 |

默认请求：

```http
POST /api/auth/login
Content-Type: application/json

{"username":"admin","password":"Admin@123456"}
```

断言：响应体包含 `00000`。

## 闭环命令

```bash
scripts/run-week3-jmeter.sh
```

脚本默认使用 Apache JMeter 5.6.3，下载到 `target/apache-jmeter-5.6.3`，结果输出到 `target/jmeter-results/`。如果当前网络访问 Apache 官方分发较慢，可覆盖下载源：

```bash
JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz \
  scripts/run-week3-jmeter.sh
```

保留原生 JMeter CLI 运行方式时，三个模板都通过 `${__P(NAME,default)}` 读取命令行 `-J` 属性。

## 当前结论

本仓库已提供登录并发、Week3 核心读链路和 Week3 写入链路 `.jmx` 模板，并通过 `scripts/run-week3-jmeter.sh` 在本机完成实测闭环。脚本会生成 `.jtl`、HTML dashboard 和 `target/jmeter-results/week3-jmeter-summary.md`。

写入链路模板每个线程/循环都会生成唯一外包人员用户名、手机号和邮箱，并动态填充当前工作日期与绩效周期，避免同一用户同一项目重复提交上岗申请，也避免同一绩效维度当前记录唯一约束冲突。本次闭环已基于实际 JMeter 输出记录样本量、线程数、错误率、平均耗时、p95 和吞吐量。

三个模板都通过 `${__P(NAME,default)}` 读取命令行 `-J` 属性，便于验收环境覆盖 host、port、线程数和循环次数；写入链路还可覆盖管理员账号、测试密码、部门和项目 ID。

2026-08-17 09:21 CST Week4 Elasticsearch 日志检索闭环复测结果：

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 82.07 | 91 | 641 | 10.16 |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 36.40 | 67 | 1409 | 29.64 |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 37.35 | 110 | 152 | 5.32 |
