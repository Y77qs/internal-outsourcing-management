# 内部测试外包人员管理系统数据库说明

## 核心表

| 表名 | 用途 |
| --- | --- |
| `sys_user` | 用户账号、密码密文、部门归属、状态 |
| `sys_role` | 管理员、上级领导、测试外包人员，包含角色描述 |
| `sys_permission` | 接口权限点，包含权限类型、接口路径和请求方法 |
| `sys_user_role` | 用户与角色多对多关系 |
| `sys_role_permission` | 角色与权限多对多关系 |
| `sys_department` | 部门基础资料 |
| `project` | 项目基础资料，包含项目编码、周期和说明 |
| `onboarding_application` | 上岗申请，包含提交时间、撤回时间和状态 |
| `approval_record` | 审批记录，第一阶段一条申请最多一条最终审批记录 |
| `notification_message` | MQ 通知消息状态 |
| `operation_log` | 关键操作审计日志 |
| `work_log` | 测试外包人员工作日志，记录项目、日期、内容、问题和完成情况 |
| `performance_record` | 绩效记录，保存 A/B/C 等级、周期、当前有效标记和历史修改原因 |

## 关键约束

- `sys_user.username` 唯一，密码只保存 `password_hash`；公开注册账号默认绑定 `OUTSOURCER`，内部账号由管理员创建后写入 `sys_user_role`。
- `sys_user_role` 和 `sys_role_permission` 使用关联表表达多对多关系，避免多值字段。
- 同一用户同一项目不可重复提交待审批申请，该规则由 Service 层查询 `PENDING` 状态控制。
- `approval_record.application_id` 唯一，保证单级审批模型下一个申请只有一个最终审批记录。
- `notification_message.event_id` 唯一，用于 MQ 消息幂等更新。
- 操作日志参数会做密码、Token 脱敏，避免保存敏感明文。
- `work_log` 通过 `idx_work_log_user_date`、`idx_work_log_project_date` 支持按人员、项目和日期范围查询。
- `performance_record` 通过 `idx_performance_current` 支持“同一人员 + 项目 + 周期”的当前有效记录查询；重复新增同周期当前绩效会被拒绝，绩效调整必须走修改接口，将旧记录置为历史并保留修改原因。
- 绩效修改使用 Redis 锁 `pta:performance:lock:{userId}:{projectId}:{periodType}:{periodValue}` 控制并发，避免多人同时覆盖同一周期绩效。
- `schema.sql` 末尾包含条件补列脚本，用于兼容已经启动过的本地 MySQL 数据卷。

## 绩效周期字段

| 字段 | 规则 |
| --- | --- |
| `period_type` | `MONTH`、`QUARTER`、`PROJECT` |
| `period_value` | 月度为 `yyyy-MM`，季度为 `yyyy-Qn`，项目周期由服务端生成 `PROJECT-{projectId}` |
| `is_current` | `1` 表示当前有效记录，`0` 表示历史版本 |
| `modification_reason` | 修改绩效时必填，用于说明当前版本或历史版本的调整原因 |

## 日志检索存储

`operation_log` 仍是审计日志的权威数据源。服务写入 MySQL 成功后会 best-effort 同步到 Elasticsearch 索引 `pta-operation-logs`，用于后续检索增强预留；当前查询接口始终以 MySQL 权威日志执行条件查询和关键词多字段模糊查询，避免 Elasticsearch 索引缺口造成审计漏查。

## 待优化候选

- `sys_permission.request_method/path` 与 `http_method/api_path` 语义重复；本轮门控测试中功能无变化，但性能数据存在环境波动，暂不移除，留待后续更稳定的压测环境确认。

## 初始化数据

`src/main/resources/db/data.sql` 初始化：

- 三类角色：`ADMIN`、`LEADER`、`OUTSOURCER`。
- 系统所需权限点：用户管理、角色查询、基础资料查询、申请提交/查询/撤回、审批查询/处理、通知查询、操作日志查询、工作日志、绩效管理。
- 默认账号：`admin/Admin@123456` 是种子管理员；`leader/Leader@123456` 是演示审批账号。后续领导或管理员账号通过 `/api/users` 或 `/ui/users` 创建。
- 示例部门和项目：`QA_CENTER`、`TEST_PLATFORM`、`PTA-OUTSOURCING`。

## SQL 脚本运行方式

应用启动时通过 Spring SQL Init 自动执行：

- `classpath:db/schema.sql`
- `classpath:db/data.sql`

Docker Compose 会创建数据库 `pta_outsourcing`，应用连接后完成建表和初始化数据。

如需完全重置人工验收数据，可在当前工程目录执行：

```bash
docker compose down -v
docker compose up -d
```
