# 内部测试外包人员管理系统最终交付报告

> 交付日期：2026-08-21。本文是整个项目的最终交付报告，不再只描述 Week4 收尾工作。文件名沿用 `week4-final-delivery-report.md`，用于兼容 README 和历史交接文档中的既有链接。

## 1. 交付结论

本项目已完成“内部测试外包人员管理系统”的核心设计、开发、测试、部署验证和文档整理，具备本地验收、复测和后续评审条件。

系统围绕测试外包人员入场管理展开，已覆盖需求梳理、模块拆分、ER/UML/架构图设计、后端业务实现、前端验收页面、数据库脚本、接口文档、部署手册、单元测试、JMeter 压测、操作日志审计和最终复盘总结。

本次最终交付按“本地 Docker Compose 验收”口径完成，未做远程 SSH 云主机部署；最终总结以本文档和 `docs/implementation/` 下的 Markdown 文档为准。

## 2. 需求与模块拆分

项目根据 PRD 和需求文档，将系统拆分为以下核心模块：

| 模块 | 交付内容 |
| --- | --- |
| 认证登录 | 用户注册、登录、退出、JWT 签发和当前登录用户查询 |
| 权限管理 | 用户、角色、权限、RBAC 接口访问控制 |
| 基础资料 | 部门、项目下拉数据和初始化资料 |
| 上岗申请 | 外包人员提交申请、查询个人申请、查看详情、撤回待审批申请 |
| 领导审批 | 待审批分页、审批通过、驳回、批量处理 |
| 通知消息 | 审批相关通知落库、RabbitMQ 投递、消费状态更新和失败记录 |
| 工作日志 | 外包人员提交/修改个人日志，领导和管理员按条件查询 |
| 绩效管理 | 月度、季度、项目周期绩效评定，历史版本保留和并发修改控制 |
| 操作日志 | AOP 自动记录关键操作，参数脱敏，MySQL 权威存储 + ES 候选检索 |
| 部署监控 | Docker Compose、Actuator、Prometheus、Grafana、健康检查 |
| 测试压测 | JUnit/Mockito 单元测试、Checkstyle、JMeter 并发压测和结果汇总 |

## 3. 技术架构

系统采用 Spring Boot 3.x 分层架构：

- 前端展示层：Thymeleaf + Bootstrap 5 + Bootstrap Icons。
- 接口接入层：Controller 提供 REST API，统一返回 `ResultVO<T>`。
- 认证授权层：JWT 鉴权、Redis 登录态校验、RBAC 权限控制。
- 业务服务层：认证、用户权限、申请审批、通知、工作日志、绩效、审计日志。
- 数据访问层：MyBatis-Plus Mapper、分页查询、条件查询。
- 存储与中间件层：MySQL、Redis、RabbitMQ、Elasticsearch。
- 工程与监控层：Docker Compose、Actuator、Prometheus、Grafana、JMeter、Checkstyle、Log4j2。

相关设计文件：

```text
/Users/baozhashizitou/Documents/pta/week1/PRD.md
/Users/baozhashizitou/Documents/pta/week1/需求文档.md
/Users/baozhashizitou/Documents/pta/week1/ER.png
/Users/baozhashizitou/Documents/pta/week1/UML.png
/Users/baozhashizitou/Documents/pta/week1/系统架构.png
```

## 4. 功能交付情况

已交付的核心业务闭环包括：

- 外包人员公开注册并自动登录。
- 管理员创建内部账号并分配角色。
- JWT + Redis 登录态控制，支持退出登录。
- RBAC 权限校验，按角色限制接口访问。
- 外包人员提交上岗申请，避免重复待审批申请。
- 领导或管理员审批申请，支持通过、驳回和批量处理。
- 审批相关通知落库，并通过 RabbitMQ 完成异步通知状态流转。
- 外包人员提交和修改工作日志，领导/管理员可按人员、项目、日期查询。
- 领导/管理员维护绩效记录，支持 A/B/C 等级、周期规则、历史版本和修改原因。
- 操作日志通过 AOP 自动记录成功/失败操作，并对密码、Token 等敏感字段脱敏。
- 操作日志查询支持 Elasticsearch 候选检索 + MySQL 权威兜底，避免审计漏查。
- 提供 Thymeleaf 页面用于人工验收核心业务流程。
- 提供 Swagger UI / Knife4j 在线接口文档入口。

## 5. 数据库交付

数据库以 MySQL 8.x 为主，已完成核心表结构、约束、索引和初始化数据。

主要表包括：

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
sys_department
project
onboarding_application
approval_record
notification_message
operation_log
work_log
performance_record
```

数据库设计文档和 SQL 脚本：

```text
/Users/baozhashizitou/Documents/pta/docs/implementation/database.md
/Users/baozhashizitou/Documents/pta/src/main/resources/db/schema.sql
/Users/baozhashizitou/Documents/pta/src/main/resources/db/data.sql
```

应用启动时通过 Spring SQL Init 执行 `schema.sql` 和 `data.sql`，Docker Compose 创建数据库 `pta_outsourcing` 后可自动完成建表和初始化数据。

## 6. 接口与页面交付

接口文档：

```text
/Users/baozhashizitou/Documents/pta/docs/implementation/api.md
```

在线文档入口：

```text
http://localhost:8080/doc.html
http://localhost:8080/swagger-ui.html
```

主要页面入口：

```text
http://localhost:8080/ui/login
http://localhost:8080/ui/register
http://localhost:8080/ui/dashboard
http://localhost:8080/ui/applications
http://localhost:8080/ui/approvals
http://localhost:8080/ui/work-logs
http://localhost:8080/ui/performances
http://localhost:8080/ui/users
http://localhost:8080/ui/notifications
http://localhost:8080/ui/operation-logs
```

## 7. 测试与质量

最终验证环境：本机 Docker Compose，应用端口 `8080`。

| 类型 | 命令或入口 | 结果 |
| --- | --- | --- |
| 单元测试与覆盖率 | `./mvnw -q clean verify` | 88 tests，0 failures，0 errors，0 skipped |
| JaCoCo 行覆盖率 | `target/site/jacoco/index.html` | 89.38%，超过 80% 要求 |
| 代码规范 | `./mvnw -q checkstyle:check` | 通过 |
| 运行日志 | `spring-boot-starter-log4j2`、`src/main/resources/log4j2-spring.xml` | Log4j2 控制台日志和滚动文件日志已配置 |
| 前端静态检查 | `node --check src/main/resources/static/js/app.js` | 通过 |
| JMeter XML | `xmllint --noout docs/implementation/*.jmx` | 通过 |
| Compose 配置 | `docker compose config -q` | 通过 |
| Diff 空白检查 | `git diff --check` | 通过 |
| 本地部署 | `docker compose up -d --build` | 镜像构建成功，`pta-app` healthy |
| 健康检查 | `/api/health`、`/actuator/health/readiness` | 返回 UP |
| 文档与页面 | `/doc.html`、核心 UI 页面 | HTTP 200 |
| 监控指标 | `/actuator/prometheus` | HTTP 200 |

JaCoCo 结果：

| Counter | Covered | Total | Ratio |
| --- | ---: | ---: | ---: |
| Line | 1069 | 1196 | 89.38% |
| Instruction | 5242 | 5891 | 88.98% |
| Branch | 257 | 360 | 71.39% |

## 8. 性能测试

JMeter 压测文件：

```text
/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-login-concurrency.jmx
/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-week3-core-business.jmx
/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-week3-write-chain.jmx
/Users/baozhashizitou/Documents/pta/scripts/run-week3-jmeter.sh
```

最终实测结果：

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 82.07 | 91 | 641 | 10.16 |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 36.40 | 67 | 1409 | 29.64 |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 37.35 | 110 | 152 | 5.32 |

压测产物：

```text
/Users/baozhashizitou/Documents/pta/target/jmeter-results/week3-jmeter-summary.md
/Users/baozhashizitou/Documents/pta/target/jmeter-results/*.jtl
/Users/baozhashizitou/Documents/pta/target/jmeter-results/*-html/
```

Tomcat 线程池支持环境变量配置：

```text
TOMCAT_MAX_THREADS
TOMCAT_MIN_SPARE_THREADS
TOMCAT_ACCEPT_COUNT
```

Docker Compose 验收环境使用 `300/30/200`，用于本地较高并发验证。

## 9. 部署与监控

部署手册：

```text
/Users/baozhashizitou/Documents/pta/docs/implementation/deployment-monitoring.md
```

部署文件：

```text
/Users/baozhashizitou/Documents/pta/Dockerfile
/Users/baozhashizitou/Documents/pta/docker-compose.yml
/Users/baozhashizitou/Documents/pta/ops/prometheus/prometheus.yml
/Users/baozhashizitou/Documents/pta/ops/grafana/dashboards/pta-overview.json
```

启动命令：

```bash
docker compose up -d --build
```

本地服务：

| 服务 | 端口 | 用途 |
| --- | ---: | --- |
| app | 8080 | Spring Boot、REST API、页面、Actuator |
| mysql | 3306 | 业务数据和审计日志权威存储 |
| redis | 6379 | 登录态缓存、绩效并发锁 |
| rabbitmq | 5672 / 15672 | 审批通知 MQ 和管理后台 |
| elasticsearch | 9200 | 操作日志候选检索 |
| prometheus | 9090 | 指标采集 |
| grafana | 3001 | 监控看板 |

## 10. 文档交付清单

```text
/Users/baozhashizitou/Documents/pta/README.md
/Users/baozhashizitou/Documents/pta/docs/implementation/README.md
/Users/baozhashizitou/Documents/pta/docs/implementation/api.md
/Users/baozhashizitou/Documents/pta/docs/implementation/database.md
/Users/baozhashizitou/Documents/pta/docs/implementation/deployment-monitoring.md
/Users/baozhashizitou/Documents/pta/docs/implementation/test-record.md
/Users/baozhashizitou/Documents/pta/docs/implementation/problem-list.md
/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-report.md
/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-week3-run-report.md
/Users/baozhashizitou/Documents/pta/docs/implementation/week4-final-delivery-report.md
/Users/baozhashizitou/Documents/pta/docs/implementation/codex-work-summary.md
```

## 11. 难点与总结

项目中最有挑战的是操作日志检索与审计可靠性设计。

操作日志不是普通搜索数据，而是用于问题追踪、权限审查和责任定位的审计数据。如果直接把 Elasticsearch 作为唯一查询来源，虽然能满足“日志检索”的技术点，但一旦 ES 写入失败、索引延迟或服务不可用，就可能漏掉审计记录。

最终方案是：

- MySQL 作为操作日志权威存储。
- Elasticsearch 作为关键词候选检索增强。
- 查询时先由 ES 返回候选日志 ID。
- 最终仍回到 MySQL 做操作人、模块、时间范围、LIKE 兜底、排序和分页。
- ES 异常或无命中时，系统继续使用 MySQL 查询，保证审计完整性。

这个设计体现的不是简单接入一个中间件，而是在“满足验收技术要求”和“保证真实业务可靠性”之间做取舍。对于审计、权限、绩效这类敏感模块，正确性、可追溯性和异常降级能力比单纯的技术堆叠更重要。

## 12. 保留项与后续优化

- 本次未做远程服务器部署。若后续需要，可复用当前 Docker Compose 方案在云主机执行同样 smoke test，并追加服务器 IP、端口和截图验收记录。
- Grafana 当前以 JVM、HTTP 请求和基础服务指标为主，可继续补充待审批数量、通知失败数量、绩效修改次数等业务指标。
- Elasticsearch 已参与操作日志候选检索，但历史日志回补任务暂未实现；当前 MySQL LIKE 兜底已保证历史日志不会漏查。
- RabbitMQ 通知当前完成站内通知状态流转，可继续扩展为邮件、短信或企业 IM 通知。
