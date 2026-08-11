# 内部测试外包人员管理系统测试记录

## 已执行命令

以下命令在当前工程目录执行：

```bash
./mvnw -q test
./mvnw -q clean test
./mvnw -q checkstyle:check
./mvnw -q verify
node --check src/main/resources/static/js/app.js
xmllint --noout docs/jmeter-login-concurrency.jmx docs/jmeter-week3-core-business.jmx docs/jmeter-week3-write-chain.jmx
JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh
docker compose config --quiet
docker compose build app
docker compose up -d --build
docker compose ps
curl -fsS http://localhost:8080/actuator/health/readiness
curl -I http://localhost:8080/doc.html
curl -I http://localhost:8080/ui/work-logs
curl -I http://localhost:8080/ui/performances
./mvnw -q spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

说明：已修复 Docker 构建阶段缺少 `unzip` 和 readiness 健康检查鉴权白名单问题。2026-08-10 重新执行 Docker 验收时，先完成 `docker pull maven:3.9.11-eclipse-temurin-21`，随后 `docker compose build app` 从当前源码执行 Maven package 并成功生成 `pta-app:latest`；`docker compose up -d --build` 复用缓存完成重建启动，`docker compose ps` 显示 `pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 均为 healthy，Prometheus 与 Grafana 正常运行。

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
| 单元测试 | 工作日志提交时绑定当前登录用户 |
| 单元测试 | 工作日志只能由本人修改 |
| 单元测试 | 工作日志支持按人员、项目和日期分页查询 |
| 单元测试 | 绩效新增遇到同周期当前记录时拒绝创建，要求走修改接口填写原因 |
| 单元测试 | 绩效 Redis 锁被占用时拒绝修改 |
| 单元测试 | 绩效修改必须填写修改原因 |
| 单元测试 | 绩效修改生成新的当前版本 |
| 单元测试 | 绩效周期格式校验 |
| 单元测试 | 绩效人员姓名模糊搜索返回多个同名人员 ID |
| 单元测试 | 绩效人员 ID 精确搜索返回对应人员 |
| 单元测试 | 绩效列表支持多个被评价人员 ID 一次查询 |
| 单元测试 | 操作日志密码和 Token 脱敏 |
| 单元测试 | 操作日志关键词检索始终以 MySQL 权威日志为准 |
| 单元测试 | 操作日志 MySQL 权威写入失败会暴露异常且跳过 ES 索引 |
| 单元测试 | 操作日志 ES 索引失败不影响已写入的 MySQL 审计日志 |
| 单元测试 | 业务异常与失败审计写入异常同时发生时，AOP 保留原业务异常并附加审计异常 |
| 单元测试 | 绩效人员搜索接口带有 `@OperationLog` 审计注解 |
| 规范检查 | Checkstyle 基础命名、未使用导入、必需大括号 |
| 覆盖率门控 | JaCoCo `verify` 对 Week3 增量核心类覆盖率执行 80% 门槛，不代表全项目覆盖率 |
| 前端静态检查 | `node --check` 验证 `app.js` 语法 |
| JMeter 材料校验 | 登录并发、核心读链路、写入链路三个 `.jmx` 文件通过 XML 结构校验 |
| JMeter 写入链路材料 | `jmeter-week3-write-chain.jmx` 使用唯一外包人员变量覆盖注册/登录、提交申请、审批、工作日志、绩效新增/修改和操作日志查询 |
| JMeter 实测闭环 | `scripts/run-week3-jmeter.sh` 生成三组 `.jtl` 和 HTML dashboard，三组 error rate 均为 0.00% |
| 启动验证 | MySQL 条件补列脚本执行成功，`department_id`、`api_path`、`submitted_at` 等字段存在 |
| 页面验证 | `/ui/login`、`/ui/dashboard`、`/js/app.js` 返回 200 |
| 文档验证 | `/doc.html` 返回 200，`/swagger-ui.html` 可跳转访问 |
| 日志验证 | 登录操作通过 AOP 写入 `operation_log`，密码字段显示为 `******` |

## 手工联调结果

为避免占用默认端口，本次 smoke test 使用 `18080` 验证。链路结果：

- `/api/health` 返回 `00000`。
- `/ui/login`、`/ui/dashboard`、`/js/app.js`、`/doc.html` 返回 HTTP 200。
- `/ui/work-logs`、`/ui/performances` 返回 HTTP 200，可作为 Week3 工作日志和绩效管理验收入口。
- `/actuator/prometheus` 暴露 Prometheus 指标。
- `/api/departments` 使用管理员 Token 查询成功，返回 2 个启用部门。
- `/api/projects` 使用管理员 Token 查询成功，返回 1 个启用项目。
- `/v3/api-docs` 包含 `/api/departments` 与 `/api/projects`，Knife4j 可展示基础资料接口。
- `/webjars/bootstrap-icons/1.13.1/font/bootstrap-icons.css` 返回 HTTP 200，页面按钮图标资源可访问。
- `admin/Admin@123456` 登录成功，返回 JWT。
- 管理员调用 `POST /api/users` 创建领导账号成功，返回 `00000`。
- 新建领导账号登录成功，访问 `/api/approvals/pending` 返回 `00000`。
- 新注册外包人员调用 `POST /api/users` 返回 HTTP 403。
- `/api/operation-logs` 使用管理员 Token 查询成功。
- `/api/operation-logs?keyword=登录` 以 MySQL 权威日志执行多字段模糊查询。
- 最新登录日志为 AOP 自动记录，参数为 `[{"username":"admin","password":"******"}]`。
- 最新创建用户日志为 AOP 自动记录，密码字段显示为 `******`。

当前 Docker Compose 端口 `8080` smoke test 通过：

- `/actuator/health/readiness` 返回 `{"status":"UP"}`。
- `/doc.html` 返回 HTTP 200。
- `/ui/work-logs` 返回 HTTP 200。
- `/ui/performances` 返回 HTTP 200。

当前截图验收已于 2026-08-11 基于 Docker Compose 端口 `8080` 的真实页面重新生成，截图文件位于 `docs/screenshots/`：

- `login.png`
- `dashboard.png`
- `applications.png`
- `approvals.png`
- `users.png`
- `work-logs.png`
- `performances.png`
- `operation-logs.png`
- `notifications.png`

当前 Docker Compose 端口 `8080` JMeter 实测通过：

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 78.20 | 85 | 571 | 10.18 |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 16.55 | 34 | 148 | 29.75 |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 31.72 | 106 | 126 | 5.37 |

压测原始文件位于 `target/jmeter-results/`，汇总报告位于 `docs/jmeter-week3-run-report.md`。

## 企业级前端体验验证

本轮按 PRD 要求继续使用 Thymeleaf + Bootstrap 5 + Bootstrap Icons，不引入 Vue、React 或 Node 构建链。浏览器验收使用临时端口 `18080`：

| 页面 | 桌面端 1280px 验证结果 |
| --- | --- |
| `/ui/dashboard` | 左侧导航常驻、顶部用户区和页面标题区正常，无页面级横向溢出 |
| `/ui/users` | 用户列表为主视图，创建账号和角色分配均为 Modal，主按钮与查询按钮层级清晰 |
| `/ui/applications` | 申请列表为主视图，新建申请为 Modal，部门/项目下拉可用，项目随部门过滤后默认可选 |
| `/ui/approvals` | 待审批列表为主视图，批量栏默认隐藏，驳回意见使用 Modal，不再使用 `window.prompt` |
| `/ui/work-logs` | 工作日志列表为主视图，支持项目/日期筛选、提交日志 Modal 和本人修改操作 |
| `/ui/performances` | 绩效列表为主视图，支持当前有效筛选、新增绩效 Modal、修改原因输入 |
| `/ui/notifications` | 标题、副标题、刷新按钮、状态色和空状态正常 |
| `/ui/operation-logs` | 筛选工具栏、关键词查询按钮、长文本截断和表格滚动正常 |

移动端按 `390px × 844px` 验证：

- 侧边栏自动隐藏，顶部菜单按钮可见。
- Bootstrap offcanvas 菜单可打开，包含 7 个导航入口。
- `/ui/users`、`/ui/applications`、`/ui/approvals`、`/ui/work-logs`、`/ui/performances`、`/ui/notifications`、`/ui/operation-logs` 均使用 `.table-responsive`，页面本身没有横向溢出。
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
9. 外包人员进入 `/ui/work-logs` 提交工作日志，并确认本人可修改。
10. 领导或管理员进入 `/ui/performances` 新增绩效，再修改一次并填写修改原因。
11. 查询 `/api/notifications`，确认通知状态从 `PENDING` 更新为 `SENT`。
12. 查询 `/api/operation-logs?keyword=登录`，确认关键词检索可用且密码字段显示为 `******`。
13. 打开 `/actuator/prometheus`、`http://localhost:9090`、`http://localhost:3001`，确认监控链路可访问。
