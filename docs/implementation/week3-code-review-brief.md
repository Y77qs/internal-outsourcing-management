# Week3 代码审查交接说明

这份文档用于交给一个完全没有上下文的新对话或代码审查 agent。请让对方以代码审查视角检查当前工程改动是否满足 Week3 要求，并重点发现 bug、权限问题、业务规则偏差、测试缺口和部署风险。

## 可直接复制给新对话的提示词

请审查本地项目 `/Users/baozhashizitou/Documents/pta/week2` 的当前未提交改动。你没有前置上下文，请先阅读本说明，再自行查看代码、运行必要检查，并按“发现优先”的代码审查格式输出结果。

审查目标：确认 Week3 高级功能与性能优化是否按要求实现，不能只看能否编译。请重点检查行为正确性、权限边界、前后端一致性、数据库/接口契约、并发控制、Elasticsearch 索引增强、Docker/监控配置和测试覆盖。

输出格式要求：
- 先列 Findings，按严重程度排序。
- 每条问题必须包含文件路径和行号、影响、触发条件、建议修复。
- 如果没有发现问题，请明确说“未发现阻塞问题”，并列出剩余风险或测试缺口。
- 不要把普通总结放在 Findings 前面。

## 项目与背景

- 可运行源码根目录：`/Users/baozhashizitou/Documents/pta/week2`
- 需求参考：
  - `/Users/baozhashizitou/Documents/pta/HANOFF.md`
  - `/Users/baozhashizitou/Documents/pta/docs/design/PRD.md`
  - `/Users/baozhashizitou/Documents/pta/docs/design/UML.png`
  - `/Users/baozhashizitou/Documents/pta/docs/design/ER.png`
  - `/Users/baozhashizitou/Documents/pta/docs/design/系统架构.png`
- 当前 Week3 改动大量文件仍处于未提交/未跟踪状态，请审查整个工作区，而不是只审查已跟踪 diff。

建议先执行：

```bash
cd /Users/baozhashizitou/Documents/pta/week2
git status --short
git diff --stat
```

## Week3 应满足的核心要求

1. 工作日志模块
   - 新增 `work_log` 表、实体、Mapper、Service、Controller、Thymeleaf 页面 `/ui/work-logs`。
   - 外包人员可新增、修改、查询个人日志。
   - 领导和管理员可按人员、项目、日期查询全部日志。

2. 绩效管理模块
   - 新增 `performance_record` 表、实体、Mapper、Service、Controller、页面 `/ui/performances`。
   - 支持等级 `A/B/C`，周期类型 `MONTH/QUARTER/PROJECT`。
   - 管理员/领导可创建、修改、查询当前绩效和历史。
   - 外包人员只能查看本人绩效。
   - 修改绩效时应保留历史，旧记录置为非当前，新记录为当前。
   - 并发控制使用 Redis 锁：`pta:performance:lock:{userId}:{projectId}:{periodType}:{periodValue}`，TTL 10 秒，释放时校验 token。
   - 锁冲突返回业务错误：“绩效正在被修改，请稍后重试”。

3. 绩效人员搜索改造
   - 新增 `GET /api/performances/user-options`，允许 `performance:read` 或 `performance:write` 使用。
   - 支持 `name` 按 `realName` 模糊搜索，`userId` 按 ID 精确搜索，最多 20 条，按 ID 升序。
   - 返回 `id`、`username`、`realName`、`status`。
   - 绩效列表支持 `evaluatedUserIds` 多 ID 查询；同时传 `evaluatedUserId` 和 `evaluatedUserIds` 时，单 ID 优先。
   - 页面候选栏位于“人员姓名”输入框下方，可点击候选项自动补齐人员 ID。
   - 列表筛选区有“全部匹配人员：ID ...”选项；新增绩效弹窗只能选择单个人员。

4. 新增绩效周期值选项化
   - 新增/编辑绩效弹窗中，`periodValue` 应是下拉选项，不是自由输入。
   - 仅生成当前年选项：
     - `MONTH`：当前年 12 个月，例如 `2026-01` 到 `2026-12`。
     - `QUARTER`：当前年 4 个季度，例如 `2026-Q1` 到 `2026-Q4`。
     - `PROJECT`：不让用户填写周期值，提交空值，由后端生成 `PROJECT-{projectId}`。
   - 切换周期类型时自动转换：
     - `MONTH -> QUARTER`：月份映射到所在季度，如 `2026-08 -> 2026-Q3`。
     - `QUARTER -> MONTH`：季度映射到首月，如 `2026-Q3 -> 2026-07`。
   - 编辑绩效时周期类型和值保持只读；历史值不在当前年时应能回显。

5. 操作日志增强
   - MySQL 仍是操作日志权威存储。
   - 新增 Elasticsearch 8.x 索引 `pta-operation-logs`。
   - 写日志后 best-effort 同步 ES，ES 不可用不影响业务。
   - `/api/operation-logs` 支持 `keyword` 搜索；查询始终以 MySQL 权威日志为准，ES 不作为唯一查询来源。
   - 敏感字段应脱敏，不能把密码、token 等原样写入日志。

6. 监控、部署、工程化
   - 加入 Actuator、Prometheus registry、JaCoCo。
   - 提供 `Dockerfile`、扩展 `docker-compose.yml`，包含 app、MySQL、Redis、RabbitMQ、Elasticsearch、Prometheus、Grafana。
   - Prometheus 抓取 `/actuator/prometheus`。
   - app healthcheck 访问 `/actuator/health/readiness`，安全白名单应允许匿名访问。
   - 提供 GitHub Actions 风格 CI、JMeter 登录并发、核心读链路和写入链路压测材料、部署监控说明。

## 重点审查路径

后端核心：
- `src/main/java/com/pta/outsourcing/controller/WorkLogController.java`
- `src/main/java/com/pta/outsourcing/service/impl/WorkLogServiceImpl.java`
- `src/main/java/com/pta/outsourcing/controller/PerformanceController.java`
- `src/main/java/com/pta/outsourcing/service/impl/PerformanceServiceImpl.java`
- `src/main/java/com/pta/outsourcing/service/RedisLockService.java`
- `src/main/java/com/pta/outsourcing/service/OperationLogSearchService.java`
- `src/main/java/com/pta/outsourcing/service/impl/OperationLogServiceImpl.java`
- `src/main/java/com/pta/outsourcing/config/SecurityConfig.java`

数据与权限：
- `src/main/resources/db/schema.sql`
- `src/main/resources/db/data.sql`
- `src/main/resources/application.yml`

前端：
- `src/main/resources/templates/work-logs.html`
- `src/main/resources/templates/performances.html`
- `src/main/resources/templates/operation-logs.html`
- `src/main/resources/static/js/app.js`
- `src/main/resources/static/css/app.css`

部署与测试：
- `Dockerfile`
- `docker-compose.yml`
- `ops/prometheus/prometheus.yml`
- `ops/grafana/`
- `.github/workflows/week3-ci.yml`
- `docs/implementation/jmeter-login-concurrency.jmx`
- `docs/implementation/jmeter-week3-core-business.jmx`
- `docs/implementation/jmeter-week3-write-chain.jmx`
- `scripts/run-week3-jmeter.sh`
- `docs/implementation/jmeter-week3-run-report.md`
- `src/test/java/com/pta/outsourcing/service/WorkLogServiceImplTest.java`
- `src/test/java/com/pta/outsourcing/service/PerformanceServiceImplTest.java`
- `src/test/java/com/pta/outsourcing/service/OperationLogServiceImplTest.java`

## 建议验证命令

```bash
cd /Users/baozhashizitou/Documents/pta/week2
node --check src/main/resources/static/js/app.js
./mvnw -q test
./mvnw -q checkstyle:check
./mvnw -q verify
xmllint --noout docs/implementation/jmeter-login-concurrency.jmx docs/implementation/jmeter-week3-core-business.jmx docs/implementation/jmeter-week3-write-chain.jmx
JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh
docker compose up -d --build
docker compose ps
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/actuator/prometheus | head
curl -I http://localhost:8080/doc.html
curl -I http://localhost:8080/ui/work-logs
curl -I http://localhost:8080/ui/performances
```

注意：测试输出中如果只出现 Mockito/ByteBuddy 关于未来 JDK 动态 agent 的 warning，但命令 exit code 为 0，不视为当前失败。

## 审查时特别留意

- RBAC 是否真的按角色隔离，尤其是外包人员不能查他人工作日志/绩效。
- `performance:read/write` 是否足以使用绩效人员搜索，是否错误依赖 `user:read`。
- 新增绩效时 `PROJECT` 周期是否不会因为空 `periodValue` 被前端或后端误拒。
- 绩效历史是否不会被覆盖，当前记录唯一性是否合理。
- Redis 锁释放是否校验 token，锁失败是否有明确业务错误。
- ES 索引写入失败时是否完全不影响 MySQL 审计写入和主业务查询。
- MySQL 权威关键词查询是否覆盖必要字段，是否存在 SQL 注入或查询过宽风险。
- 操作日志脱敏是否覆盖密码、token、authorization 等字段。
- Docker 构建是否依赖本地已有 jar，CI 环境是否也能从源码构建。
- `Dockerfile` builder/runtime 阶段工具是否足够，healthcheck 是否能通过安全配置。
- JMeter 写入链路是否使用唯一用户/变量避免并发写入冲突，是否覆盖申请、审批、工作日志、绩效和操作日志。
- JMeter 核心读链路和写入链路是否避免同一管理员多线程重复登录互相踢 token；实测结果是否包含 `.jtl`、HTML dashboard 和指标摘要。
- 前端候选栏、周期值下拉、禁用字段与提交 payload 是否一致。
- 当前 Week3 新增文件已纳入暂存区；审查结论中请确认最终提交是否包含这些新增源码、测试、Docker、CI、监控和文档材料。

## 已知最近验证状态

最近一次实现后已通过：
- `node --check src/main/resources/static/js/app.js`
- `./mvnw -q test`
- `./mvnw -q checkstyle:check`
- `./mvnw -q verify`
- `xmllint --noout docs/implementation/jmeter-login-concurrency.jmx docs/implementation/jmeter-week3-core-business.jmx docs/implementation/jmeter-week3-write-chain.jmx`
- `JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh`
- `docker compose config -q`
- `docker compose build app`
- `docker compose up -d --build`
- `docker compose ps` 显示 `pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 为 healthy，Prometheus 与 Grafana 运行中
- JMeter 实测：登录并发 100 samples、核心读链路 301 samples、写入链路 46 samples，错误率均为 0.00%

代码审查仍需独立复核，不要仅依据这份验证状态下结论。
