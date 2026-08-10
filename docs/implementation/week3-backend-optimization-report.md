# Week3 后端优化说明

## 修复范围

本轮按 P1/P2 优先级修复 Week3 后端问题，重点覆盖操作日志安全、操作日志写入权威性、绩效业务边界、分页保护、绩效当前记录唯一性、Docker/CI/JMeter 工程化材料。

## 已修复问题

### P1 操作日志敏感信息泄露风险

- 新增 `OperationLogSanitizer`，统一负责操作日志参数脱敏和截断。
- `OperationLogAspect` 在参数仍是完整 JSON 时先脱敏，再截断，避免长 password/token 被截断后破坏 JSON 结构。
- `OperationLogServiceImpl` 写库前再次调用 sanitizer 兜底，错误信息也统一走同一规则。
- 补充长 password、长 token、Authorization Bearer 的单元测试。

### P1 操作日志查询权威性

- MySQL 作为操作日志权威存储，keyword 查询统一走 MySQL 模糊查询。
- `OperationLogSearchService` 简化为 best-effort ES 索引同步，不再提供会因索引缺口漏数据的查询路径。
- 保留 ES 写入增强能力；ES 不可用只影响后续外部增强检索，不影响业务查询。

### P1 操作日志写入异常边界

- `OperationLogServiceImpl.record()` 先写 MySQL 权威审计日志，再 best-effort 同步 Elasticsearch。
- MySQL 插入失败时抛出 `IllegalStateException("权威操作日志写入失败")`，不再静默吞掉审计写入失败。
- Elasticsearch 索引失败只记录 warn，不影响已经落库的 MySQL 审计日志。
- `OperationLogSearchService` 不再内部吞 ES 异常，best-effort 边界统一收敛在 `OperationLogServiceImpl`。
- `OperationLogAspect` 在业务方法已抛异常时，如果失败审计写入也失败，会把审计异常作为 suppressed 异常挂到原业务异常上，避免覆盖真实业务失败原因。
- 补充 MySQL insert 失败和 ES index 失败两条单元测试。

### P1 重复绩效创建绕过修改原因

- `PerformanceServiceImpl.create()` 发现同人员、项目、周期已有当前绩效时直接返回业务错误。
- 同周期绩效调整统一走 `update()`，由 `PerformanceUpdateRequest.modificationReason` 强制记录修改原因，并保留历史版本。
- 数据库唯一索引继续作为并发兜底，避免竞态下出现两条当前记录。
- 补充“重复 create 被拒绝且不 insert、不 update 旧记录”的单元测试。

### P1 绩效人员范围限制

- `PerformanceServiceImpl` 创建/更新绩效前校验被评定人必须拥有 `OUTSOURCER` 角色。
- `/api/performances/user-options` 查询通过角色子查询和服务层二次过滤，只返回测试外包人员。
- 补充管理员/领导不能作为被评定人、外包人员可以作为被评定人的测试。

### P2 分页参数缺少上限

- 新增 `PageQuery`，统一归一分页参数。
- `pageNo < 1` 归一为 `1`，`pageSize < 1` 归一为 `10`，`pageSize > 100` 归一为 `100`。
- 工作日志、绩效、操作日志三个分页入口都通过 Service seam 统一限制。

### P2 当前有效绩效唯一性

- `schema.sql` 为 `performance_record` 新增生成列 `current_unique_key`。
- 新增唯一索引 `uk_performance_current_active`，仅约束 `is_current = 1` 的当前记录，历史记录生成列为 `NULL`，不互相冲突。
- 迁移前自动归档重复当前记录，仅保留同维度最大 ID 为当前。
- Service 捕获 `DuplicateKeyException`，返回稳定业务错误。

### P2 Docker/CI/JMeter/JaCoCo

- `Dockerfile` 改为多阶段构建，从当前源码执行 Maven package，不再复用本地旧 jar。
- `.dockerignore` 不再放行 `target/*.jar`。
- `docker-compose.yml` 解除 app 对 Elasticsearch healthy 的硬依赖，保留 ES/Prometheus/Grafana 增强服务。
- CI workflow 位于仓库根 `.github/workflows/week3-ci.yml`，远端发布版直接在仓库根目录执行。
- JaCoCo 注释和测试文档明确当前 80% 门控是 Week3 增量核心类口径，不冒充全项目覆盖率。
- 新增 `jmeter-week3-core-business.jmx`，覆盖登录后工作日志、绩效、操作日志查询链路。
- 新增 `jmeter-week3-write-chain.jmx`，用唯一外包人员变量覆盖注册/登录、提交申请、审批、工作日志、绩效新增/修改和操作日志查询。
- 新增 `scripts/run-week3-jmeter.sh` 和 `jmeter-week3-run-report.md`，在本机 Docker Compose 环境完成三组 JMeter 实测闭环。
- 2026-08-10 重新执行 Docker 构建验收，先完成 Maven 基础镜像拉取，随后 `docker compose build app` 与 `docker compose up -d --build` 均通过，`pta-app` 重建后为 healthy。

## 优化逻辑

- 日志脱敏作为独立深 Module：调用方只关心 `sanitize(String)`，内部隐藏 JSON 结构脱敏、文本兜底和长度控制。
- 操作日志查询以权威数据源为准：ES 只承担可选索引同步，避免 best-effort 写入与查询权威性冲突。
- 操作日志写入拆分为两个异常边界：MySQL 写入是审计强一致边界，ES 索引是可丢弃增强边界。
- AOP 失败路径优先保护业务异常可诊断性：审计异常不覆盖原业务异常，但会作为 suppressed 信息保留。
- 绩效创建和修改拆成两个业务入口：创建只负责首次建档，修改负责版本替换和修改原因留痕。
- 分页限制作为公共 Module：三个业务 Service 共用一个归一规则，减少 Controller 分散校验。
- 绩效当前唯一性由数据库兜底：Redis 锁降低并发冲突，唯一索引保证最终一致性。
- Docker/CI 保持构建可复现：镜像内容必须由当前源码构建生成。

## 保留问题与理由

- Elasticsearch 查询增强不补代码：当前接口以 MySQL 权威日志查询，ES 仅做 best-effort 索引同步和后续检索增强预留。直接补 ES 查询会引入索引缺口漏审计的风险；若后续强制使用 ES，应采用“ES 候选 + MySQL 兜底补全”的 hybrid search，而不是让 ES 成为唯一结果源。
- JaCoCo 覆盖率口径不扩展为全项目 80%：当前门控聚焦 Week3 增量核心类，不把 entity/dto/vo/config/启动类纳入核心逻辑覆盖率，也不冒充全项目覆盖率。
- JMeter 原始 `.jtl` 与 HTML dashboard 位于 `target/jmeter-results/`，作为构建产物不提交；仓库提交脚本和 `docs/jmeter-week3-run-report.md` 指标摘要。

## 验证结果

已通过：

- `./mvnw -q test`
- `./mvnw -q checkstyle:check`
- `./mvnw -q verify`
- `docker compose config -q`
- `docker compose up -d --build`
- `docker compose ps`
- `node --check src/main/resources/static/js/app.js`
- `xmllint --noout docs/jmeter-login-concurrency.jmx docs/jmeter-week3-core-business.jmx docs/jmeter-week3-write-chain.jmx`
- `JMETER_URL=https://mirrors.ustc.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.tgz scripts/run-week3-jmeter.sh`
- 调试标记扫描覆盖 `src` 和 `docs`，无业务代码残留

Docker 构建验收：

- `docker compose build app`：通过，`pta-app:latest` 构建完成。
- `docker compose up -d --build`：通过，`pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 均为 healthy，Prometheus 与 Grafana 运行中。

当前源码 smoke test 使用 `18080` 临时端口通过：

- `/actuator/health/readiness` 返回 200
- `/actuator/prometheus` 可访问
- `/doc.html` 返回 200
- `/ui/work-logs`、`/ui/performances` 返回 200
- `/api/operation-logs?keyword=登录&pageNo=0&pageSize=101` 返回脱敏参数，分页归一为 `pageNo=1,pageSize=100`
- `/api/performances/user-options?userId=1` 返回空列表，管理员不进入绩效候选
- MySQL `performance_record.current_unique_key` 和 `uk_performance_current_active` 已存在

## 验收结论

本轮已满足 PRD 中操作日志敏感信息保护、外包人员绩效管理、列表分页保护、当前有效绩效唯一性、ES 不影响主业务查询、Docker/CI 可靠性和 JMeter 压测闭环方向的核心要求。Docker 本机重建验收已通过；JMeter 登录并发、核心读链路、写入链路均已生成 `.jtl`、HTML dashboard 和指标摘要，错误率均为 0.00%。
