# 内部测试外包人员管理系统接口说明

统一响应格式：

```json
{
  "code": "00000",
  "message": "成功",
  "data": {},
  "traceId": null,
  "timestamp": "2026-08-02T11:00:00"
}
```

## 文档与页面入口

| 路径 | 说明 |
| --- | --- |
| `/doc.html` | Knife4j 接口文档 |
| `/swagger-ui.html` | Swagger UI |
| `/ui/login` | Thymeleaf + Bootstrap 登录页 |
| `/ui/register` | 测试外包人员注册页，成功后自动登录进入工作台 |
| `/ui/applications` | 上岗申请页，使用 Modal 新建申请和部门/项目下拉 |
| `/ui/approvals` | 领导审批页，支持选中后批量处理和驳回意见 Modal |
| `/ui/users` | 用户管理页，管理员可通过 Modal 创建内部账号并分配角色 |
| `/ui/notifications` | 通知页 |
| `/ui/operation-logs` | 操作日志页 |

## 认证

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 公开 | 注册测试外包人员账号，默认分配 `OUTSOURCER`；页面注册成功后会自动登录 |
| POST | `/api/auth/login` | 公开 | 登录并返回 JWT |
| POST | `/api/auth/logout` | 登录 | 删除 Redis 登录态 |
| GET | `/api/auth/me` | 登录 | 查询当前用户、角色、权限 |

## 用户与权限

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/users` | `user:write` | 管理员创建内部账号，可分配 `OUTSOURCER`、`LEADER`、`ADMIN` 等已有角色 |
| GET | `/api/users` | `user:read` | 分页查询用户 |
| GET | `/api/users/{id}` | `user:read` | 查询用户详情 |
| PUT | `/api/users/{id}/status` | `user:write` | 启用或禁用用户 |
| PUT | `/api/users/{id}/roles` | `user:write` | 分配角色 |
| GET | `/api/roles` | `role:read` | 查询角色列表 |
| GET | `/api/permissions` | `role:read` | 查询权限列表 |

## 基础资料

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/departments` | `basic:read` | 查询启用部门下拉选项 |
| GET | `/api/projects` | `basic:read` | 查询启用项目下拉选项，可传 `departmentId` 过滤 |

管理员创建领导账号示例：

```json
{
  "username": "leader02",
  "password": "Leader@123456",
  "phone": "13800000003",
  "email": "leader02@example.com",
  "realName": "审批领导二号",
  "departmentId": 2,
  "status": "ENABLED",
  "roleIds": [2]
}
```

公开注册不允许选择角色；第一个 `admin/Admin@123456` 是 `data.sql` 初始化的种子管理员，用于创建后续领导和管理员账号。

## 上岗申请

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/onboarding/applications` | `application:create` | 提交上岗申请 |
| GET | `/api/onboarding/applications/mine` | `application:read:self` | 查询个人申请 |
| GET | `/api/onboarding/applications/{id}` | 申请人或审批/用户查询权限 | 查询申请详情 |
| POST | `/api/onboarding/applications/{id}/withdraw` | `application:withdraw` | 撤回待审批申请 |

提交申请示例：

```json
{
  "departmentId": 2,
  "projectId": 1,
  "positionType": "功能测试",
  "applicationReason": "参与内部测试外包人员管理系统功能测试"
}
```

## 领导审批

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/approvals/pending` | `approval:read` | 分页查询待审批申请 |
| POST | `/api/approvals/{applicationId}/approve` | `approval:write` | 审批通过 |
| POST | `/api/approvals/{applicationId}/reject` | `approval:write` | 驳回申请，意见必填 |
| POST | `/api/approvals/batch` | `approval:write` | 批量审批；全选时传当前页申请 ID 列表 |

批量审批示例：

```json
{
  "applicationIds": [1, 2, 3],
  "result": "APPROVED",
  "opinion": "资料完整，同意上岗"
}
```

## 通知

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/notifications` | `notification:read` | 查询通知消息；管理员和领导可看全部，普通用户看自己的 |

申请提交、撤回、审批通过、审批驳回都会写入 `notification_message`，并发送 RabbitMQ 消息。

## 操作日志

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/operation-logs` | `operation:read` | 按操作人、模块、时间范围分页查询操作日志 |

查询参数：

| 参数 | 说明 |
| --- | --- |
| `operatorId` | 操作人 ID，可选 |
| `moduleName` | 模块名称，支持模糊匹配 |
| `startTime` | 开始时间，格式如 `2026-08-03T10:00:00` |
| `endTime` | 结束时间，格式如 `2026-08-03T18:00:00` |
| `pageNo` / `pageSize` | 分页参数 |
