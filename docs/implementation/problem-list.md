# 内部测试外包人员管理系统问题 List

| 问题 | 处理结果 |
| --- | --- |
| 本机没有 `mvn` 命令 | 增加轻量 Maven Wrapper，`./mvnw` 自动下载 Maven 3.9.11 |
| 本机没有 MySQL/Redis CLI | 使用 Docker Compose 启动 MySQL、Redis、RabbitMQ |
| Spring Initializr 当前默认 Spring Boot 4.x | 手写 `pom.xml` 并固定 Spring Boot 3.5.x，符合 PRD 的 Spring Boot 3.x |
| MyBatis-Plus 3.5.17 分页拦截器拆包 | 增加 `mybatis-plus-jsqlparser` 依赖 |
| 批量/全选审批如何表达 | 后端接口接收当前页选中的申请 ID 列表，由前端全选后提交 |
| MQ 通知失败如何留痕 | 使用 RabbitMQ 重试与 DLQ，正常消费更新 `SENT`，死信消费更新 `FAILED` |
| 审批事务未提交前发送 MQ，消费者可能查不到通知记录 | 通知表写入后注册事务 `afterCommit` 回调，事务提交成功后再投递 MQ |
| 截图和 PRD 要求 Swagger/Knife4j | 保留 Swagger UI，并增加 Knife4j `/doc.html` |
| 架构图要求 AOP 自动记录操作日志 | 增加 `@OperationLog` 与 `OperationLogAspect`，记录成功/失败并脱敏 |
| 已有 MySQL 数据卷缺少本轮新增字段 | 在 `schema.sql` 增加条件补列脚本，启动时自动补齐 |
| 人工审核需要页面入口 | 增加 Thymeleaf + Bootstrap 5 页面，不引入 Node/Vue/React |
| Week3 要求工作日志与绩效管理 | 新增 `work_log`、`performance_record` 表、后端接口和 `/ui/work-logs`、`/ui/performances` 页面 |
| 绩效多人同时修改可能覆盖数据 | 使用 Redis 分布式锁控制同一人员、项目、周期绩效的并发修改，并保留历史版本 |
| 操作日志关键词检索要接 ES，但不能影响主业务 | MySQL 仍做权威存储和关键词查询，写库后 best-effort 同步 Elasticsearch 索引，ES 不作为查询权威来源 |
| 监控组件本地搭建复杂 | 使用 Actuator + Prometheus + Grafana，并在 Docker Compose 中提供一键启动配置 |
| CI/CD 需要模拟企业流程 | 新增 `.github/workflows/week3-ci.yml`，覆盖测试、Checkstyle、打包和 Docker build |
| JMeter 环境不一定预装 | 新增 `scripts/run-week3-jmeter.sh` 自动下载并缓存 JMeter 5.6.3，三组压测已生成 `.jtl`、HTML dashboard 和指标摘要 |

## 后续待扩展

- 可在 Grafana 中继续补充业务指标，如待审批数量、通知失败数量、绩效修改次数。
- 可为 Elasticsearch 增加历史 MySQL 日志回补任务，解决上线前旧日志未索引的问题。
- 可把 RabbitMQ 通知从模拟消费升级为真实邮件、短信或站内信。
