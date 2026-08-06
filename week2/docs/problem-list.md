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

## 后续待扩展

- 第三周可补工作日志、绩效评定、ES 日志检索、Prometheus/Grafana 监控。
- 可补 JMeter 压测脚本，覆盖登录、申请提交、审批处理。
- 可把 RabbitMQ 通知从模拟消费升级为真实邮件、短信或站内信。
