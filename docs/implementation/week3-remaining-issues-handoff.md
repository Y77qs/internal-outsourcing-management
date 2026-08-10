# Week3 剩余问题优化交接文档

本文档用于交给一个没有上下文的新对话或代码优化 agent。请让对方先阅读本文件，再基于当前工程 `/Users/baozhashizitou/Documents/pta/week2` 修复问题并重新验证。

状态更新：本文记录的是当时剩余问题清单。后续优化已完成，最新结果以 `docs/week3-remaining-issues-optimization-report.md`、`docs/week3-backend-optimization-report.md` 和 `docs/test-record.md` 为准；其中 Docker 构建验收已在 2026-08-10 重新执行并通过。

## 背景与审查口径

- 仓库根目录：`/Users/baozhashizitou/Documents/pta`
- 后端工程目录：`/Users/baozhashizitou/Documents/pta/week2`
- 固定对比点：`main = 05578f3313560164c7ddd308427201142a2bac04`
- 优化说明：`/Users/baozhashizitou/Documents/pta/docs/implementation/week3-backend-optimization-report.md`
- 需求依据：`/Users/baozhashizitou/Documents/pta/docs/design/PRD.md`、`UML.png`、`ER.png`、`系统架构.png`

当前代码已通过：

```bash
cd /Users/baozhashizitou/Documents/pta/week2
./mvnw -q test
./mvnw -q checkstyle:check
./mvnw -q verify
node --check src/main/resources/static/js/app.js
xmllint --noout docs/jmeter-login-concurrency.jmx docs/jmeter-week3-core-business.jmx
docker compose config -q
```

但仍存在以下需要优化或交付前说明的问题。

## P1：新增绩效绕过“修改必须填写原因”的业务约束

相关文件：

- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/PerformanceServiceImpl.java:73`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/PerformanceServiceImpl.java:81`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/PerformanceServiceImpl.java:88`

问题：

`create()` 遇到同一外包人员、同一项目、同一周期已有当前绩效时，会先把旧记录 `is_current=false`，再插入一条新的当前记录。但新记录的 `modificationReason` 传入 `null`，等于通过“新增绩效”绕过了 `update()` 中“修改绩效必须填写修改原因”的约束。

影响：

- 历史绩效可追溯性变弱。
- Mentor 可能认为当前实现不符合“修改保留历史且必须说明修改原因”的要求。

建议修复：

- 推荐方案：`create()` 如果发现同维度已有当前绩效，直接抛出业务异常，例如“该周期已有当前绩效，请使用修改功能并填写修改原因”。
- 保留 `update()` 作为唯一替换当前绩效的入口。
- 数据库唯一索引 `uk_performance_current_active` 继续保留，作为并发兜底。

建议补充测试：

- 新增 `create()` 同维度已有 current 时应失败，不应调用 `updateById()` 归档旧记录。
- `update()` 仍要求 `modificationReason` 非空。
- 并发/重复插入时仍能通过唯一索引返回稳定业务错误。

## P1：MySQL 审计日志写入失败被静默吞掉

相关文件：

- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/OperationLogServiceImpl.java:42`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/OperationLogServiceImpl.java:52`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/OperationLogServiceImpl.java:54`

问题：

`record()` 把 MySQL 审计写入和 Elasticsearch 索引同步包在同一个 `try` 里，并在 `catch` 中吞掉所有异常。优化说明中明确“操作日志仍先写入 MySQL，MySQL 是权威日志库，ES 是 best-effort”，因此 MySQL 插入失败不应和 ES 写入失败一样被静默忽略。

影响：

- 业务操作可能成功，但权威审计日志缺失。
- 不满足“记录所有平台操作日志”的验收口径。

建议修复：

- 拆分异常边界：
  - MySQL `operationLogMapper.insert(logEntity)` 失败：至少 `log.error` 并抛出明确异常，不能静默吞掉。
  - ES `operationLogSearchService.index(logEntity)` 失败：允许降级，不影响业务。
- 若担心审计失败影响用户操作，需要在文档中明确说明“审计失败只报警不阻断业务”，并补充监控/告警；否则按审计权威要求让 MySQL 写入失败暴露出来。

建议补充测试：

- MySQL insert 抛异常时，`record()` 不应静默成功，且不应调用 ES index。
- ES index 抛异常时，MySQL 已写入，`record()` 不抛业务异常。

## P2：操作日志检索文档与实际实现不一致

相关文件：

- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/controller/OperationLogController.java:33`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/api.md:29`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/api.md:201`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/OperationLogServiceImpl.java:69`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/OperationLogSearchService.java:31`

问题：

文档和 JavaDoc 仍写“优先使用 Elasticsearch 检索，ES 不可用时降级 MySQL”，但当前实现已经改为 MySQL 权威查询，`OperationLogSearchService` 只负责 best-effort 写 ES 索引，不再提供 ES 查询。

影响：

- Mentor 看接口文档时会认为代码没有实现文档承诺。
- 讲解系统架构时容易被追问“为什么没有走 ES 查询”。

建议修复：

- 如果当前设计坚持 MySQL 权威查询：把 `api.md`、`OperationLogController` JavaDoc、部署说明统一改成“查询以 MySQL 为权威，ES 仅做 best-effort 索引同步/后续检索增强预留”。
- 如果必须满足“ES 检索增强”：实现 ES 查询，但需要保证不会因 ES 索引缺口漏日志，例如先查 MySQL 权威结果，ES 只作为候选加速或增强，不可作为唯一结果源。

## P2：绩效人员搜索没有记录操作日志

相关文件：

- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/controller/PerformanceController.java:118`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/controller/PerformanceController.java:119`

问题：

`GET /api/performances/user-options` 没有 `@OperationLog`。其他多数查询接口已经接入操作日志，但该绩效人员搜索接口没有留痕。

影响：

- 如果按“记录所有平台操作日志”严格验收，该接口存在审计覆盖缺口。

建议修复：

- 在 `searchUserOptions()` 上增加：

```java
@OperationLog(moduleName = "绩效管理", operationType = "搜索绩效人员")
```

建议补充测试：

- 可通过 AOP 测试或接口 smoke test 验证调用该接口会生成操作日志。

## P2：Docker 构建验收记录互相矛盾

相关文件：

- `/Users/baozhashizitou/Documents/pta/docs/implementation/week3-backend-optimization-report.md:61`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/test-record.md:14`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/test-record.md:19`

问题：

优化说明写“Docker 镜像重新构建未在本机完成”，但测试记录又写 `docker compose build app`、`docker compose up -d --build` 已通过，且 app、MySQL、Redis、RabbitMQ、Elasticsearch healthy。

影响：

- 验收材料自相矛盾，mentor 会追问真实执行结果。

建议修复：

- 重新运行并确认真实状态：

```bash
cd /Users/baozhashizitou/Documents/pta/week2
docker compose build app
docker compose up -d --build
docker compose ps
```

- 如果成功：更新 `week3-backend-optimization-report.md`，删除“未完成”的说法。
- 如果失败：更新 `test-record.md`，不要写 build 已通过，并保留失败原因。

## P2：Week3 新增文件仍未跟踪，交付可能缺文件

相关状态：

当前 `git status --short` 显示大量 Week3 新增文件仍为 `??`，例如：

- `/Users/baozhashizitou/Documents/pta/.github/workflows/week3-ci.yml`
- `/Users/baozhashizitou/Documents/pta/Dockerfile`
- `/Users/baozhashizitou/Documents/pta/.dockerignore`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/common/PageQuery.java`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/controller/PerformanceController.java`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/controller/WorkLogController.java`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/PerformanceServiceImpl.java`
- `/Users/baozhashizitou/Documents/pta/src/main/java/com/pta/outsourcing/service/impl/WorkLogServiceImpl.java`
- `/Users/baozhashizitou/Documents/pta/src/test/java/com/pta/outsourcing/service/PerformanceServiceImplTest.java`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/week3-backend-optimization-report.md`
- `/Users/baozhashizitou/Documents/pta/docs/implementation/jmeter-week3-core-business.jmx`

问题：

如果通过 git commit/branch 交付，未跟踪文件不会进入提交，优化清单会变成“文档写了，但代码没有交付”。

建议修复：

- 如果交付方式是 git：把 Week3 相关新增文件全部加入版本控制。
- 如果交付方式是压缩整个目录：可以不提交，但需要确认压缩包包含这些未跟踪文件。

建议检查：

```bash
git status --short
git diff --stat
```

## 已知保留项，不建议误判为代码 bug

以下内容已在优化说明中明确作为保留或口径说明，不应在没有新需求的情况下当成阻塞 bug：

1. JMeter 压测已闭环。
   - 当前已有登录并发、Week3 核心读链路、Week3 写入链路三个模板。
   - `scripts/run-week3-jmeter.sh` 已在本机 Docker Compose 环境生成三组 `.jtl`、HTML dashboard 和指标摘要；原始结果位于 `target/jmeter-results/`，作为构建产物不纳入 git。

2. JaCoCo 当前是 Week3 增量核心类覆盖率口径。
   - 当前 `pom.xml` 只 include `OperationLogServiceImpl`、`WorkLogServiceImpl`、`PerformanceServiceImpl`、`JwtTokenProvider`。
   - 如果 mentor 要求全项目 80%，需要另行取消 include 并补测试。

3. Elasticsearch 当前仅保留 best-effort 索引同步。
   - 如果验收口径接受“MySQL 权威查询，ES 后续增强预留”，只需统一文档。
   - 如果验收口径要求“必须用 ES 检索”，需要另行实现不漏日志的 ES 查询策略。

## 修复后必须重新执行的验证

```bash
cd /Users/baozhashizitou/Documents/pta/week2
./mvnw -q test
./mvnw -q checkstyle:check
./mvnw -q verify
node --check src/main/resources/static/js/app.js
xmllint --noout docs/jmeter-login-concurrency.jmx docs/jmeter-week3-core-business.jmx
docker compose config -q
```

如修改 Docker 文档或 Dockerfile，还需要运行：

```bash
docker compose build app
docker compose up -d --build
docker compose ps
```

## 建议交付结论模板

修复完成后，最终说明建议写成：

- 已修复重复绩效创建绕过修改原因的问题。
- 已拆分 MySQL 审计写入与 ES best-effort 索引的异常边界。
- 已统一操作日志检索文档口径。
- 已补齐绩效人员搜索操作日志。
- 已统一 Docker 构建验收记录。
- 已确认 Week3 新增文件被纳入交付。
- 测试命令全部通过。
