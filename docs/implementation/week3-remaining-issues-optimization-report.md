# Week3 剩余问题优化说明

## 修复了哪些问题

### P1：重复新增绩效绕过修改原因

- 修复 `PerformanceServiceImpl.create()` 的语义：创建接口只负责首次建档；如果同一被评价人、项目、周期已经存在当前绩效，直接返回业务错误。
- 同周期绩效调整必须走 `update()`，由 `PerformanceUpdateRequest.modificationReason` 强制填写修改原因，并生成新的当前版本、保留旧历史版本。
- 补充单元测试，覆盖重复 `create()` 被拒绝，且不会调用 `updateById()` 归档旧记录，也不会继续 `insert()` 新记录。

### P1：操作日志 MySQL 写入失败被吞

- 拆分 `OperationLogServiceImpl.record()` 的异常边界：MySQL 插入是权威审计写入，失败必须抛出异常；Elasticsearch 只是 best-effort 索引增强，失败只记录 warn。
- MySQL 插入失败时抛出 `IllegalStateException("权威操作日志写入失败")`，并跳过 ES 索引。
- ES 索引失败时不影响已经写入的 MySQL 审计日志，也不影响业务返回。
- `OperationLogSearchService` 不再内部吞掉 ES 异常，best-effort 降级统一由 `OperationLogServiceImpl` 管理。
- 业务方法已抛异常时，如果失败审计写入也失败，AOP 会保留原业务异常，并把审计异常加入 suppressed。
- 补充两条单元测试分别覆盖 MySQL insert 失败和 ES index 失败。

### P2：代码与文档不一致

- `OperationLogController` JavaDoc 改为 MySQL 权威关键词查询。
- `docs/implementation/api.md`、`docs/implementation/deployment-monitoring.md`、`docs/implementation/README.md`、`docs/implementation/problem-list.md`、`docs/implementation/test-record.md`、`docs/implementation/week3-code-review-brief.md` 统一改为“MySQL 权威日志查询，ES best-effort 索引同步”。
- 保留 handoff 文件中的旧描述作为历史输入资料，不把它改写成结论。

### P2：绩效人员搜索审计缺口

- 为 `GET /api/performances/user-options` 增加 `@OperationLog(moduleName = "绩效管理", operationType = "搜索绩效人员")`。
- 新增 `PerformanceControllerAuditTest`，通过反射验证该接口方法必须带审计注解。

### P2：Docker 验收记录矛盾

- 重新执行 `docker compose build app`、`docker compose up -d --build`、`docker compose ps`。
- 文档统一更新为真实结果：先完成 `docker pull maven:3.9.11-eclipse-temurin-21`，随后 `docker compose build app` 从当前源码成功构建 `pta-app:latest`；`docker compose up -d --build` 重建启动成功，`docker compose ps` 显示 app 和核心中间件 healthy。
- 保留 Dockerfile 从当前源码构建镜像的设计，不回退为复制本地 jar。

### P2：未跟踪文件交付风险

- 当前 Week3 新增文件在本轮最终交付前通过 `git add README.md .github week2` 纳入索引，避免后续提交遗漏新增源码、测试、Docker、CI、监控和文档材料。

## 优化逻辑

- 把业务入口语义收紧：`create()` 是首次创建，`update()` 是版本替换和修改原因留痕，避免同一能力从两个入口绕过审计字段。
- 把审计写入分层：MySQL 是强一致审计边界，ES 是可失败的增强边界，查询也以 MySQL 权威数据为准。
- AOP 失败路径保留原业务异常优先级，审计异常作为 suppressed 信息辅助排查。
- 用小测试固定关键契约：服务测试验证业务状态变化和外部依赖调用边界，控制器反射测试验证 AOP 可见的注解元数据。
- 文档跟随当前架构，而不是跟随旧设想：所有面向交付和审查的文档都明确 ES 不再承担查询权威来源。
- Docker 验收只记录真实状态：外部镜像仓库超时归类为环境阻塞，不把已有容器健康状态误写成重新构建成功。

## 代码是怎么实现的

- `PerformanceServiceImpl.create()` 在获得 Redis 锁后先查当前绩效；发现已有当前记录时抛出 `BizException(ErrorCode.BUSINESS_ERROR, "该周期已有当前绩效，请使用修改功能并填写修改原因")`。
- `OperationLogServiceImpl.record()` 先构造并脱敏 `OperationLog` 实体，再单独执行 `operationLogMapper.insert()`；该步骤失败会抛出 `IllegalStateException`。随后单独调用 `operationLogSearchService.index()`，该步骤失败只记录 warn。
- `OperationLogSearchService.index()` 只负责执行 ES 写入，不再内部吞异常；`OperationLogAspect` 在失败审计也失败时通过 `Throwable.addSuppressed()` 保留审计异常。
- `PerformanceController.searchUserOptions()` 增加 `@OperationLog`，让现有 `OperationLogAspect` 自动采集搜索操作。
- 文档修改集中在 API、部署监控、测试记录、问题清单、Week3 优化报告和代码审查 brief。

## 哪些问题保留以及理由

- Docker 镜像重新构建已在本机完成。此前失败原因是 `maven:3.9.11-eclipse-temurin-21` 基础镜像拉取阶段请求 Docker Hub anonymous token 超时；本轮先手动拉取该基础镜像后，`docker compose build app` 和 `docker compose up -d --build` 已通过。
- Elasticsearch 查询增强未实现为查询路径。当前选择 MySQL 权威查询、ES best-effort 索引同步，是为了避免 ES 索引缺口导致审计漏查；如果后续验收强制要求 ES 参与查询，需要另做“不漏日志”的候选增强策略，而不能把 ES 作为唯一结果源。

## 验证命令和结果

已通过：

- `./mvnw -q -Dtest=PerformanceServiceImplTest,OperationLogServiceImplTest test`
- `./mvnw -q -Dtest=PerformanceControllerAuditTest test`
- `./mvnw -q test`
- `./mvnw -q clean test`
- `./mvnw -q checkstyle:check`
- `./mvnw -q verify`
- `node --check src/main/resources/static/js/app.js`
- `xmllint --noout docs/implementation/jmeter-login-concurrency.jmx docs/implementation/jmeter-week3-core-business.jmx`
- `docker compose config -q`
- `docker compose build app`：通过，`pta-app:latest` 从当前源码构建完成。
- `docker compose up -d --build`：通过，`pta-app` 重建启动完成。
- `docker compose ps`：`pta-app`、MySQL、Redis、RabbitMQ、Elasticsearch 为 healthy，Prometheus 与 Grafana 运行中。
- 调试残留扫描：`rg -n '\x5BDEBUG-' src docs` 无匹配。
- `git diff --check`
- `git add README.md .github week2`

说明：Maven 测试输出中的 Mockito/ByteBuddy 动态 agent warning 不影响当前验证结果；操作日志测试中的 `db down` 和 `es down` 栈信息来自单元测试刻意触发的 mock 异常，命令退出码均为 0。
