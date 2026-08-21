# 实现说明 / Implementation Notes

## 交付范围

系统交付以验收项为准，技术栈遵循 [PRD](../design/PRD.md)：

- Spring Boot 3.x 基础工程。
- MyBatis-Plus + MySQL 8.x。
- 统一 `ResultVO<T>` 返回、统一异常处理、分页返回。
- 用户注册、登录、退出；公开注册只创建测试外包人员，内部账号由管理员创建。
- JWT 鉴权、RBAC 权限控制、Redis 登录状态缓存。
- 测试外包人员上岗申请：提交、个人列表、详情、撤回。
- 领导审批：待审批分页、通过、驳回、批量/全选处理。
- RabbitMQ 异步通知闭环：消息落库、发送、消费成功更新、失败进入死信队列。
- 工作日志：外包人员提交/修改个人日志，领导和管理员按人员、项目和日期查询。
- 绩效管理：领导或管理员维护 A/B/C 绩效，支持月度、季度和项目周期，保留历史版本。
- 操作日志增强：MySQL 权威存储 + Elasticsearch 候选检索；关键词查询优先用 ES 获取候选 ID，再由 MySQL 做外层过滤、兜底匹配、排序和分页。
- Prometheus/Grafana 监控：Actuator 暴露健康检查、JVM、接口耗时和 Prometheus 指标。
- Docker/CI 工程化：Dockerfile、扩展 Compose、GitHub Actions 风格流水线、JMeter 登录、核心读链路和写入链路压测模板。
- Thymeleaf + Bootstrap 5 + Bootstrap Icons 页面，用于人工验收注册登录、申请、审批、工作日志、绩效、通知和审计日志。
- Swagger/OpenAPI + Knife4j、JUnit/Mockito、Checkstyle、SLF4J 日志。
- `@OperationLog` + AOP 自动采集关键操作成功/失败日志，并对密码、Token 脱敏。

## 启动步骤

在仓库根目录执行：

```bash
docker compose up -d --build
```

如果要用本地 Maven 启动应用，只启动依赖服务：

```bash
docker compose up -d mysql redis rabbitmq elasticsearch
./mvnw spring-boot:run
```

运行测试：

```bash
./mvnw clean test
./mvnw checkstyle:check
./mvnw verify
```

## 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123456` | 系统管理员 |
| `leader` | `Leader@123456` | 上级领导 |

## 页面入口

| 路径 | 说明 |
| --- | --- |
| `/ui/login` | 登录页 |
| `/ui/register` | 测试外包人员注册页，注册成功后自动登录 |
| `/ui/dashboard` | 企业后台工作台 |
| `/ui/applications` | 上岗申请列表、新建申请 Modal、撤回 |
| `/ui/approvals` | 领导审批列表、选中后批量处理、驳回意见 Modal |
| `/ui/work-logs` | 工作日志提交、修改、按人员/项目/日期查询 |
| `/ui/performances` | 绩效新增、修改、当前记录和历史版本查询 |
| `/ui/users` | 用户列表、创建账号 Modal、角色分配 Modal |
| `/ui/notifications` | MQ 通知消息查询 |
| `/ui/operation-logs` | 管理员操作日志筛选和关键词检索 |

## 文档索引

| 文档 | 说明 |
| --- | --- |
| [接口说明](api.md) | REST API、权限、请求示例和页面入口 |
| [数据库设计](database.md) | 表结构、关系、索引和初始化数据 |
| [部署与监控说明](deployment-monitoring.md) | Docker、Elasticsearch、Prometheus/Grafana、JMeter 和 CI/CD |
| [项目最终交付报告](week4-final-delivery-report.md) | 项目范围、模块拆分、功能交付、测试压测、部署监控和复盘总结 |
| [测试记录](test-record.md) | 单元测试、页面验证、接口 smoke test 和人工联调记录 |
| [问题清单](problem-list.md) | 项目推进过程中的问题、处理方式和结论 |
| [Week3 后端优化报告](week3-backend-optimization-report.md) | 工作日志、绩效、操作审计、监控和压测交付说明 |
| [JMeter 登录压测报告](jmeter-report.md) | 登录并发压测结果摘要 |
| [JMeter Week3 实测报告](jmeter-week3-run-report.md) | 核心读链路与写入链路压测结果摘要 |
| [Postman 集合](postman_collection.json) | 接口联调集合 |
