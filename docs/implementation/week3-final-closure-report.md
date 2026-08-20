# Week3 最终验收闭环说明

> 状态更新：本文保留 Week3 当时闭环口径。Week4 已升级为全项目生产代码 80% 行覆盖率门控，并完成本地最终验收，最新结论见 `week4-final-delivery-report.md`。

## 补齐的闭环点

- 新增 `docs/implementation/jmeter-week3-write-chain.jmx`，覆盖唯一外包人员注册/登录、提交上岗申请、管理员审批、提交工作日志、新增绩效、修改绩效、查询操作日志。
- 更新 `docs/implementation/jmeter-report.md`、`docs/implementation/test-record.md`、`docs/implementation/week3-backend-optimization-report.md`、`docs/implementation/deployment-monitoring.md`、`docs/implementation/README.md`、`docs/implementation/week3-code-review-brief.md`，把 JMeter 写入链路从保留项提升为已交付压测材料。
- 新增 `scripts/run-week3-jmeter.sh` 和 `docs/implementation/jmeter-week3-run-report.md`，把 JMeter 从模板材料推进到 `.jtl`、HTML dashboard 和指标汇总均已生成的实测闭环。
- 评估 Elasticsearch 查询增强：当前不补代码，继续坚持 MySQL 权威查询 + ES best-effort 索引同步。
- 评估 JaCoCo 覆盖率口径：Week3 当时保持核心类门控；Week4 已升级为全项目生产代码 80% 行覆盖率门控。

## 为什么这样优化

- JMeter 写入链路是验收材料的缺口，不是业务代码缺口；新增 `.jmx` 模板可以在不破坏现有功能的前提下把有状态链路串起来。
- 写入链路通过唯一用户名、手机号、邮箱，并结合当前年月绩效周期降低并发冲突，不依赖人工清库。
- 操作日志属于审计场景，不能因 ES best-effort 索引缺口漏查；MySQL 权威查询更符合“审计不漏”的验收解释。
- JaCoCo 在 Week3 继续约束核心逻辑 Module；Week4 通过补充测试和 Lombok 生成代码过滤后，已按全项目生产代码口径通过 80% 门控。

## 实现方式

- `docs/implementation/jmeter-week3-write-chain.jmx` 使用 JSR223 初始化唯一外包人员变量 `OUTSOURCER_USERNAME`、`OUTSOURCER_PHONE`、`OUTSOURCER_EMAIL`，并动态填充 `WORK_DATE`、`PERIOD_VALUE`。
- 三个 JMeter 模板统一通过 `${__P(NAME,default)}` 读取命令行 `-J` 属性，避免验收环境覆盖 host、port、线程数或循环次数时仍误用默认值。
- 核心读链路和写入链路使用 `SetupThreadGroup` 管理员登录一次并发布 `WEEK3_ADMIN_TOKEN`，避免同一 `admin` 并发重复登录导致旧 token 被 Redis 最新 jti 判定失效。
- JMeter 通过 JSON Extractor 提取 `EVALUATED_USER_ID`、`OUTSOURCER_TOKEN`、`APPLICATION_ID`、`SETUP_ADMIN_TOKEN`、`PERFORMANCE_ID`，后续请求通过 Bearer Token 串联。
- 每个关键 HTTP Sampler 都断言响应体包含统一成功码 `00000`，操作日志查询额外断言返回体包含本轮唯一用户名，避免空结果误判为成功。
- 文档中明确：ES 当前只做索引增强预留；若未来补 hybrid search，ES 只能做候选增强，最终仍必须由 MySQL 权威结果兜底。
- 文档中明确：Week3 门控是核心类覆盖率；Week4 最终报告已记录全项目生产代码覆盖率。

## 保留点与理由

- 未在本轮新增 ES hybrid search 代码。理由：当前验收更关心审计不漏；MySQL 权威查询已经满足这一点，直接加入 ES 查询会增加索引缺口导致漏审计的风险。
- Week3 未扩展 JaCoCo 到全项目。Week4 已补齐 Controller、安全、异常、配置、上岗申请和基础设施测试，并升级为全项目生产代码覆盖率门控。
- JMeter 本轮已在本机 Docker Compose 环境跑通；`target/jmeter-results/` 属于构建产物不纳入 git，仓库内保留可复现脚本和 `docs/implementation/jmeter-week3-run-report.md` 指标摘要。

## 验证结果

已通过：

- `./mvnw -q test`
- `./mvnw -q checkstyle:check`
- `./mvnw -q verify`
- `node --check src/main/resources/static/js/app.js`
- `xmllint --noout docs/implementation/jmeter-login-concurrency.jmx docs/implementation/jmeter-week3-core-business.jmx docs/implementation/jmeter-week3-write-chain.jmx`
- `JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh`：登录并发 100 samples、核心读链路 301 samples、写入链路 46 samples，错误率均为 0.00%。
- `docker compose config -q`
- `docker compose up -d --build`：镜像构建使用缓存通过，`pta-app` 重建并启动到 healthy。
- `docker compose ps`：`pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 为 healthy，Prometheus 与 Grafana 运行中。
- `curl -fsS http://localhost:8080/actuator/health/readiness`：返回 `{"status":"UP"}`。
- `curl -I http://localhost:8080/doc.html`：返回 HTTP 200。
- `curl -I http://localhost:8080/ui/work-logs`：返回 HTTP 200。
- `curl -I http://localhost:8080/ui/performances`：返回 HTTP 200。

说明：Maven 测试输出中的 Mockito/ByteBuddy 动态 agent warning 不影响当前验证结果；操作日志测试里的 `db down`、`es down` 是单元测试刻意触发的失败路径。
