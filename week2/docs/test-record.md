# 内部测试外包人员管理系统测试记录

## 已执行命令

以下命令在当前工程目录执行：

```bash
./mvnw -q test
./mvnw -q clean test
./mvnw -q checkstyle:check
docker compose up -d
./mvnw -q spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

## 覆盖场景

| 类型 | 场景 |
| --- | --- |
| 单元测试 | JWT 生成与解析 |
| 单元测试 | 注册成功后默认分配测试外包人员角色 |
| 单元测试 | 用户名重复时注册失败 |
| 单元测试 | 登录成功后写入 Redis 登录态 |
| 单元测试 | 管理员创建内部账号时密码加密、部门校验并分配角色 |
| 单元测试 | 管理员创建内部账号时用户名重复失败 |
| 单元测试 | 角色集合为空或角色 ID 无效时分配失败 |
| 单元测试 | 非待审批申请不可审批 |
| 单元测试 | 驳回申请必须填写审批意见 |
| 单元测试 | 审批通过后更新申请、写审批记录、发送通知 |
| 单元测试 | 通知消息落库并发送 MQ |
| 单元测试 | 消费成功后通知状态更新为 `SENT` |
| 单元测试 | 基础资料查询只返回启用部门，并映射部门下拉 VO |
| 单元测试 | 基础资料查询支持按部门过滤启用项目，并映射项目下拉 VO |
| 规范检查 | Checkstyle 基础命名、未使用导入、必需大括号 |
| 启动验证 | MySQL 条件补列脚本执行成功，`department_id`、`api_path`、`submitted_at` 等字段存在 |
| 页面验证 | `/ui/login`、`/ui/dashboard`、`/js/app.js` 返回 200 |
| 文档验证 | `/doc.html` 返回 200，`/swagger-ui.html` 可跳转访问 |
| 日志验证 | 登录操作通过 AOP 写入 `operation_log`，密码字段显示为 `******` |

## 手工联调结果

为避免占用默认端口，本次 smoke test 使用 `18080` 验证。链路结果：

- `/api/health` 返回 `00000`。
- `/ui/login`、`/ui/dashboard`、`/js/app.js`、`/doc.html` 返回 HTTP 200。
- `/api/departments` 使用管理员 Token 查询成功，返回 2 个启用部门。
- `/api/projects` 使用管理员 Token 查询成功，返回 1 个启用项目。
- `/v3/api-docs` 包含 `/api/departments` 与 `/api/projects`，Knife4j 可展示基础资料接口。
- `/webjars/bootstrap-icons/1.13.1/font/bootstrap-icons.css` 返回 HTTP 200，页面按钮图标资源可访问。
- `admin/Admin@123456` 登录成功，返回 JWT。
- 管理员调用 `POST /api/users` 创建领导账号成功，返回 `00000`。
- 新建领导账号登录成功，访问 `/api/approvals/pending` 返回 `00000`。
- 新注册外包人员调用 `POST /api/users` 返回 HTTP 403。
- `/api/operation-logs` 使用管理员 Token 查询成功。
- 最新登录日志为 AOP 自动记录，参数为 `[{"username":"admin","password":"******"}]`。
- 最新创建用户日志为 AOP 自动记录，密码字段显示为 `******`。

## 企业级前端体验验证

本轮按 PRD 要求继续使用 Thymeleaf + Bootstrap 5 + Bootstrap Icons，不引入 Vue、React 或 Node 构建链。浏览器验收使用临时端口 `18080`：

| 页面 | 桌面端 1280px 验证结果 |
| --- | --- |
| `/ui/dashboard` | 左侧导航常驻、顶部用户区和页面标题区正常，无页面级横向溢出 |
| `/ui/users` | 用户列表为主视图，创建账号和角色分配均为 Modal，主按钮与查询按钮层级清晰 |
| `/ui/applications` | 申请列表为主视图，新建申请为 Modal，部门/项目下拉可用，项目随部门过滤后默认可选 |
| `/ui/approvals` | 待审批列表为主视图，批量栏默认隐藏，驳回意见使用 Modal，不再使用 `window.prompt` |
| `/ui/notifications` | 标题、副标题、刷新按钮、状态色和空状态正常 |
| `/ui/operation-logs` | 筛选工具栏、查询按钮、长文本截断和表格滚动正常 |

移动端按 `390px × 844px` 验证：

- 侧边栏自动隐藏，顶部菜单按钮可见。
- Bootstrap offcanvas 菜单可打开，包含 7 个导航入口。
- `/ui/users`、`/ui/applications`、`/ui/approvals`、`/ui/notifications`、`/ui/operation-logs` 均使用 `.table-responsive`，页面本身没有横向溢出。
- 可见按钮未出现文字挤压或溢出。
- 上岗申请 Modal 在 390px 宽度下完整显示，部门下拉 2 项、项目下拉 1 项。

## 手工联调建议

1. 启动依赖：`docker compose up -d`。
2. 启动后端：`./mvnw spring-boot:run`。
3. 打开页面：`http://localhost:8080/ui/login`。
4. 打开 Knife4j：`http://localhost:8080/doc.html`。
5. 使用 `admin/Admin@123456` 登录，确认用户、角色、操作日志接口可访问。
6. 在 `/ui/users` 创建内部领导账号并勾选 `LEADER` 角色，确认新领导可以登录并访问待审批列表。
7. 注册测试外包账号，确认注册成功后自动进入工作台，提交上岗申请。
8. 使用 `leader/Leader@123456` 或新建领导账号登录，查询待审批申请并通过或驳回。
9. 查询 `/api/notifications`，确认通知状态从 `PENDING` 更新为 `SENT`。
10. 查询 `/api/operation-logs`，确认创建账号和角色分配有日志，密码字段显示为 `******`。
