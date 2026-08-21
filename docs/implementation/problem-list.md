# 内部测试外包人员管理系统问题 List

本文记录项目推进过程中遇到的问题、归类和处理结果，便于复盘、验收和后续迭代。

## 问题分类总览

| 分类 | 数量 | 关注点 |
| --- | ---: | --- |
| 本地环境与工具链 | 3 | Maven、数据库中间件、JMeter 环境 |
| 框架版本与依赖兼容 | 2 | Spring Boot 版本、MyBatis-Plus 依赖拆分 |
| 业务功能与页面交互 | 3 | 审批交互、人工验收页面、工作日志与绩效 |
| 权限边界与业务闭环 | 3 | 后端权限校验、审批状态流转、跨模块联动 |
| 消息通知与事务一致性 | 2 | MQ 失败留痕、事务提交后投递 |
| 数据库与数据演进 | 1 | 已有数据卷字段补齐 |
| 操作审计与搜索 | 3 | AOP 日志、ES 候选检索与 MySQL 权威查询 |
| 监控、CI/CD 与压测 | 4 | Prometheus/Grafana、Docker Compose、GitHub Actions、JMeter |
| 测试覆盖与验收文档 | 2 | 覆盖率门控、JMeter 路径一致性 |
| 接口文档与验收入口 | 3 | Knife4j、Thymeleaf 页面入口、企业后台使用体验 |

## 本地环境与工具链

| 问题 | 处理结果 |
| --- | --- |
| 本机没有 `mvn` 命令 | 增加轻量 Maven Wrapper，`./mvnw` 自动下载 Maven 3.9.11 |
| 本机没有 MySQL/Redis CLI | 使用 Docker Compose 启动 MySQL、Redis、RabbitMQ |
| JMeter 环境不一定预装 | 新增 `scripts/run-week3-jmeter.sh` 自动下载并缓存 JMeter 5.6.3，三组压测已生成 `.jtl`、HTML dashboard 和指标摘要 |

## 框架版本与依赖兼容

| 问题 | 处理结果 |
| --- | --- |
| Spring Initializr 当前默认 Spring Boot 4.x | 手写 `pom.xml` 并固定 Spring Boot 3.5.x，符合 PRD 的 Spring Boot 3.x |
| MyBatis-Plus 3.5.17 分页拦截器拆包 | 增加 `mybatis-plus-jsqlparser` 依赖 |

## 业务功能与页面交互

| 问题 | 处理结果 |
| --- | --- |
| 批量/全选审批如何表达 | 后端接口接收当前页选中的申请 ID 列表，由前端全选后提交 |
| Week3 要求工作日志与绩效管理 | 新增 `work_log`、`performance_record` 表、后端接口和 `/ui/work-logs`、`/ui/performances` 页面 |
| 绩效多人同时修改可能覆盖数据 | 使用 Redis 分布式锁控制同一人员、项目、周期绩效的并发修改，并保留历史版本 |

## 权限边界与业务闭环

| 问题 | 处理结果 |
| --- | --- |
| 注册登录、上岗申请、审批、通知和操作日志不是孤立功能，需要串成稳定业务闭环 | 提交申请时校验重复提交、保存申请、记录操作日志并触发通知；审批时校验状态、写入审批记录、发送审批结果通知并保留审计链路 |
| 权限控制不能只依赖前端隐藏按钮 | 后端通过 Spring Security、JWT 和 RBAC 校验角色权限，限制外包人员、领导、管理员各自可访问的接口和操作 |
| 审批状态必须由后端兜底，避免重复审批或处理已撤回申请 | 审批、撤回等接口统一校验当前状态，只允许合法状态流转，防止已审批或已撤回数据被继续处理 |

## 消息通知与事务一致性

| 问题 | 处理结果 |
| --- | --- |
| MQ 通知失败如何留痕 | 使用 RabbitMQ 重试与 DLQ，正常消费更新 `SENT`，死信消费更新 `FAILED` |
| 审批事务未提交前发送 MQ，消费者可能查不到通知记录 | 通知表写入后注册事务 `afterCommit` 回调，事务提交成功后再投递 MQ |

## 数据库与数据演进

| 问题 | 处理结果 |
| --- | --- |
| 已有 MySQL 数据卷缺少本轮新增字段 | 在 `schema.sql` 增加条件补列脚本，启动时自动补齐 |

## 操作审计与搜索

| 问题 | 处理结果 |
| --- | --- |
| 架构图要求 AOP 自动记录操作日志 | 增加 `@OperationLog` 与 `OperationLogAspect`，记录成功/失败并脱敏 |
| 操作日志属于审计数据，不能因为 ES 写入失败、索引延迟或服务不可用而漏查 | 操作日志先写入 MySQL 作为权威审计记录，再尽力同步到 Elasticsearch；查询时由 ES 提供候选 ID，最终回到 MySQL 做权威过滤、排序和分页 |
| 操作日志关键词检索要接 ES，但不能影响主业务 | MySQL 仍做权威存储；关键词查询采用 ES 候选 ID + MySQL 外层过滤和 LIKE 兜底的 hybrid search，ES 不作为唯一查询来源 |

## 监控、CI/CD 与压测

| 问题 | 处理结果 |
| --- | --- |
| 监控组件本地搭建复杂 | 使用 Actuator + Prometheus + Grafana，并在 Docker Compose 中提供一键启动配置 |
| 项目需要从“本地能运行”推进到更接近真实交付 | 增加 Dockerfile 和 Docker Compose，把应用、MySQL、Redis、RabbitMQ、Elasticsearch、Prometheus、Grafana 统一编排启动，并配置健康检查 |
| 多个中间件组件需要明确职责边界和协同方式 | MySQL 负责权威数据，Redis 负责缓存和分布式锁，RabbitMQ 负责异步通知，Elasticsearch 负责日志检索增强，Prometheus/Grafana 负责监控展示 |
| CI/CD 需要模拟企业流程 | 新增 `.github/workflows/week3-ci.yml`，覆盖测试、Checkstyle、打包和 Docker build |

## 测试覆盖与验收文档

| 问题 | 处理结果 |
| --- | --- |
| Week4 要求全项目 80% 覆盖率 | 移除 JaCoCo 核心类 include，新增 `lombok.config` 排除 Lombok 生成代码，并补充上岗申请、Controller、安全、异常、配置、Redis 和 ES 索引单测；`clean verify` 行覆盖率 88.19% |
| JMeter 文档路径与真实目录不一致 | 统一脚本和文档为 `docs/implementation/*.jmx`，避免验收按旧 `docs/*.jmx` 路径执行失败 |

## 接口文档与验收入口

| 问题 | 处理结果 |
| --- | --- |
| 截图和 PRD 要求 Swagger/Knife4j | 保留 Swagger UI，并增加 Knife4j `/doc.html` |
| 人工审核需要页面入口 | 增加 Thymeleaf + Bootstrap 5 页面，不引入 Node/Vue/React |
| 页面不能只满足功能验收，还需要接近企业内部工具体验 | 根据前端原型优化登录、工作台、申请、审批、工作日志、绩效、用户管理、通知和操作日志页面的布局、按钮、表格和弹窗 |
