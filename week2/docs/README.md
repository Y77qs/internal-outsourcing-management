# 内部测试外包人员管理系统实现说明

## 交付范围

系统交付以验收项为准，技术栈遵循 `week1/PRD.md`：

- Spring Boot 3.x 基础工程。
- MyBatis-Plus + MySQL 8.x。
- 统一 `ResultVO<T>` 返回、统一异常处理、分页返回。
- 用户注册、登录、退出；公开注册只创建测试外包人员，内部账号由管理员创建。
- JWT 鉴权、RBAC 权限控制、Redis 登录状态缓存。
- 测试外包人员上岗申请：提交、个人列表、详情、撤回。
- 领导审批：待审批分页、通过、驳回、批量/全选处理。
- RabbitMQ 异步通知闭环：消息落库、发送、消费成功更新、失败进入死信队列。
- Thymeleaf + Bootstrap 5 + Bootstrap Icons 页面，用于人工验收注册登录、申请、审批、通知和审计日志。
- Swagger/OpenAPI + Knife4j、JUnit/Mockito、Checkstyle、SLF4J 日志。
- `@OperationLog` + AOP 自动采集关键操作成功/失败日志，并对密码、Token 脱敏。

## 启动步骤

进入当前工程目录后执行：

```bash
# 当前工程目录
```

1. 启动中间件：

```bash
docker compose up -d
```

2. 运行测试：

```bash
./mvnw clean test
```

3. 启动后端：

```bash
./mvnw spring-boot:run
```

4. 访问系统页面与接口文档：

```text
http://localhost:8080/ui/login
http://localhost:8080/doc.html
http://localhost:8080/swagger-ui.html
```

## 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123456` | 系统管理员 |
| `leader` | `Leader@123456` | 上级领导 |

`admin` 是系统首次启动时由 `data.sql` 初始化的种子管理员，用于进入用户管理创建领导、管理员等内部账号。公开注册接口新建用户会默认分配 `OUTSOURCER` 测试外包人员角色，注册成功后页面会自动登录并进入工作台。

## 页面入口

| 路径 | 说明 |
| --- | --- |
| `/ui/login` | 登录页 |
| `/ui/register` | 测试外包人员注册页，注册成功后自动登录 |
| `/ui/dashboard` | 企业后台工作台 |
| `/ui/applications` | 上岗申请列表、新建申请 Modal、撤回 |
| `/ui/approvals` | 领导审批列表、选中后批量处理、驳回意见 Modal |
| `/ui/users` | 用户列表、创建账号 Modal、角色分配 Modal |
| `/ui/notifications` | MQ 通知消息查询 |
| `/ui/operation-logs` | 管理员操作日志筛选查询 |

## 目录说明

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/pta/outsourcing/controller` | REST 接口层 |
| `src/main/java/com/pta/outsourcing/service` | 业务接口与实现 |
| `src/main/java/com/pta/outsourcing/aspect` | AOP 操作日志采集 |
| `src/main/java/com/pta/outsourcing/security` | JWT、Redis 登录态、当前用户上下文 |
| `src/main/resources/templates` | Thymeleaf 页面 |
| `src/main/resources/static` | Bootstrap 页面补充样式和 JS |
| `src/main/resources/db/schema.sql` | MySQL 建表脚本 |
| `src/main/resources/db/data.sql` | 初始化角色、权限、账号、部门和项目 |
| `docs/api.md` | 接口说明 |
| `docs/database.md` | 数据库设计说明 |
| `docs/optimization-report.md` | 严格测试门控优化说明 |
| `docs/test-record.md` | 测试记录 |
| `docs/problem-list.md` | 问题 list |
| `docs/postman_collection.json` | Postman 联调集合 |
