# Week3 最终验收闭环说明

## 补齐的闭环点

- 新增 `docs/jmeter-week3-write-chain.jmx`，覆盖唯一外包人员注册/登录、提交上岗申请、管理员审批、提交工作日志、新增绩效、修改绩效、查询操作日志。
- 更新 `docs/jmeter-report.md`、`docs/test-record.md`、`docs/week3-backend-optimization-report.md`、`docs/deployment-monitoring.md`、`docs/README.md`、`docs/week3-code-review-brief.md`，把 JMeter 写入链路从保留项提升为已交付压测材料。
- 新增 `scripts/run-week3-jmeter.sh` 和 `docs/jmeter-week3-run-report.md`，把 JMeter 从模板材料推进到 `.jtl`、HTML dashboard 和指标汇总均已生成的实测闭环。
- 评估 Elasticsearch 查询增强：当前不补代码，继续坚持 MySQL 权威查询 + ES best-effort 索引同步。
- 评估 JaCoCo 覆盖率口径：当前不扩展为全项目 80%，保持 Week3 增量核心类门控，并明确不纳入 entity/dto/vo/config/启动类。

## 为什么这样优化

- JMeter 写入链路是验收材料的缺口，不是业务代码缺口；新增 `.jmx` 模板可以在不破坏现有功能的前提下把有状态链路串起来。
- 写入链路通过唯一用户名、手机号、邮箱，并结合当前年月绩效周期降低并发冲突，不依赖人工清库。
- 操作日志属于审计场景，不能因 ES best-effort 索引缺口漏查；MySQL 权威查询更符合“审计不漏”的验收解释。
- JaCoCo 继续约束 Week3 核心逻辑 Module，避免把浅数据对象纳入门控制造噪声，也避免对外宣称全项目覆盖率达到 80%。

## 实现方式

- `docs/jmeter-week3-write-chain.jmx` 使用 JSR223 初始化唯一外包人员变量 `OUTSOURCER_USERNAME`、`OUTSOURCER_PHONE`、`OUTSOURCER_EMAIL`，并动态填充 `WORK_DATE`、`PERIOD_VALUE`。
- 三个 JMeter 模板统一通过 `${__P(NAME,default)}` 读取命令行 `-J` 属性，避免验收环境覆盖 host、port、线程数或循环次数时仍误用默认值。
- 核心读链路和写入链路使用 `SetupThreadGroup` 管理员登录一次并发布 `WEEK3_ADMIN_TOKEN`，避免同一 `admin` 并发重复登录导致旧 token 被 Redis 最新 jti 判定失效。
- JMeter 通过 JSON Extractor 提取 `EVALUATED_USER_ID`、`OUTSOURCER_TOKEN`、`APPLICATION_ID`、`SETUP_ADMIN_TOKEN`、`PERFORMANCE_ID`，后续请求通过 Bearer Token 串联。
- 每个关键 HTTP Sampler 都断言响应体包含统一成功码 `00000`，操作日志查询额外断言返回体包含本轮唯一用户名，避免空结果误判为成功。
- 文档中明确：ES 当前只做索引增强预留；若未来补 hybrid search，ES 只能做候选增强，最终仍必须由 MySQL 权威结果兜底。
- 文档中明确：JaCoCo 门控是 Week3 增量核心类覆盖率，不冒充全项目覆盖率。

## 保留点与理由

- 未在本轮新增 ES hybrid search 代码。理由：当前验收更关心审计不漏；MySQL 权威查询已经满足这一点，直接加入 ES 查询会增加索引缺口导致漏审计的风险。
- 未扩展 JaCoCo 到全项目。理由：大量 entity/dto/vo/config/启动类不是核心逻辑 Module，纳入门控会稀释覆盖率信号；当前 Week3 增量核心类门控更可解释。
- JMeter 本轮已在本机 Docker Compose 环境跑通；`target/jmeter-results/` 属于构建产物不纳入 git，仓库内保留可复现脚本和 `docs/jmeter-week3-run-report.md` 指标摘要。

## 验证结果

已通过：

- `./mvnw -q test`
- `./mvnw -q checkstyle:check`
- `./mvnw -q verify`
- `node --check src/main/resources/static/js/app.js`
- `xmllint --noout docs/jmeter-login-concurrency.jmx docs/jmeter-week3-core-business.jmx docs/jmeter-week3-write-chain.jmx`
- `JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh`：登录并发 100 samples、核心读链路 301 samples、写入链路 46 samples，错误率均为 0.00%。
- `docker compose config -q`
- `docker compose up -d --build`：镜像构建使用缓存通过，`pta-app` 重建并启动到 healthy。
- `docker compose ps`：`pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 为 healthy，Prometheus 与 Grafana 运行中。
- `curl -fsS http://localhost:8080/actuator/health/readiness`：返回 `{"status":"UP"}`。
- `curl -I http://localhost:8080/doc.html`：返回 HTTP 200。
- `curl -I http://localhost:8080/ui/work-logs`：返回 HTTP 200。
- `curl -I http://localhost:8080/ui/performances`：返回 HTTP 200。

说明：Maven 测试输出中的 Mockito/ByteBuddy 动态 agent warning 不影响当前验证结果；操作日志测试里的 `db down`、`es down` 是单元测试刻意触发的失败路径。
