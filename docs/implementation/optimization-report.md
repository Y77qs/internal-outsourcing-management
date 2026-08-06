# 后端严格测试门控优化说明

## 结论

本轮优化遵循“零功能变化、性能不退化、后续开发价值优先”的门控规则。最终保留的变更只有本地交付产物清理、Maven Wrapper 缓存目录收敛和文档说明更新；两个代码级删除候选虽然功能测试均通过，但性能数据无法证明全量不退化，因此已按规则撤回。

## 测试环境

- 测试时间：2026-08-04，Asia/Shanghai。
- 应用端口：`18080`。
- 中间件：`docker compose up -d` 启动 MySQL 8.4、Redis 7.4、RabbitMQ 3.13。
- 功能基线命令：
  - `./mvnw clean test`
  - `./mvnw checkstyle:check`
- Smoke 覆盖：健康检查、注册、登录、退出、当前用户、用户管理、RBAC 403、上岗申请、审批、通知、操作日志。
- 性能样本：每个核心接口 8 次请求，记录 avg、median、p95、吞吐量、HTTP 错误、业务错误。
- 数据口径：性能测试前清理 `gate_` 前缀测试用户、申请、审批、通知和操作日志，避免测试数据持续增长影响分页/count 接口。

## 删除前基线

功能基线：

| 项目 | 结果 |
| --- | --- |
| `./mvnw clean test` | 通过，9 tests，0 failures，0 errors |
| `./mvnw checkstyle:check` | 通过，0 violations |
| Smoke test | 通过，包含 `401/A0401` 和 `403/A0403` 验证 |

规范化性能基线：

| 接口 | avg(ms) | median(ms) | p95(ms) | RPS | 错误 |
| --- | ---: | ---: | ---: | ---: | ---: |
| GET `/api/health` | 2.38 | 2.22 | 3.34 | 416.51 | 0 |
| POST `/api/auth/login` | 91.44 | 81.74 | 158.90 | 10.93 | 0 |
| GET `/api/auth/me` | 11.91 | 11.47 | 18.71 | 83.67 | 0 |
| POST `/api/auth/logout` | 11.04 | 10.86 | 12.69 | 11.03 | 0 |
| GET `/api/users` | 15.96 | 14.37 | 26.80 | 62.47 | 0 |
| GET `/api/roles` | 6.42 | 6.41 | 6.80 | 154.88 | 0 |
| GET `/api/permissions` | 6.57 | 6.62 | 7.46 | 151.36 | 0 |
| GET `/api/onboarding/applications/mine` | 9.99 | 9.73 | 12.20 | 99.69 | 0 |
| GET `/api/approvals/pending` | 9.03 | 8.93 | 10.64 | 110.32 | 0 |
| GET `/api/notifications` | 8.28 | 8.20 | 9.08 | 120.22 | 0 |
| GET `/api/operation-logs` | 8.68 | 8.57 | 10.43 | 114.66 | 0 |
| POST `/api/onboarding/applications` | 13.39 | 12.59 | 19.91 | 74.34 | 0 |

## 批次结果

### 批次 1：本地产物清理

处理内容：

- 删除 `.DS_Store`。
- 删除 `target/`。
- 删除 `.mvn/apache-maven-3.9.11` 和 `.mvn/apache-maven-3.9.11-bin.zip`。
- 保留 Maven Wrapper 文件，后续仍可通过 `./mvnw` 自动使用 Maven。
- 将 `./mvnw` 下载和解压 Maven 的目录从项目内 `.mvn/` 调整到 `${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists`，避免测试后反复生成本地解压包。

结果：

| 项目 | 结果 |
| --- | --- |
| 功能测试 | 通过 |
| Checkstyle | 通过 |
| Smoke/perf | 0 HTTP 错误、0 业务错误 |
| 是否保留 | 保留 |

理由：这些文件是本机/构建产物，不属于后端运行逻辑，也不提供后续开发接口价值。删除后 `./mvnw clean test` 会重新生成 `target/`，但该目录已被 `.gitignore` 排除，不应作为交付内容。Maven Wrapper 改为用户级缓存后，后续开发仍可用相同 Maven 版本构建，同时不会把 Maven 发行包写回项目目录。

### 批次 2：权限重复字段收敛

候选变更：

- 移除 `sys_permission.request_method/path`。
- 保留 `api_path/http_method` 作为权限接口表达。

结果：

| 项目 | 结果 |
| --- | --- |
| 功能测试 | 通过 |
| Checkstyle | 通过 |
| Smoke test | 通过 |
| 性能门控 | 未通过 |
| 是否保留 | 不保留，已回滚 |

未保留原因：第二轮复测仍有核心指标高于规范化基线，例如 POST `/api/onboarding/applications` p95 为 22.96ms，高于基线 19.91ms；GET `/api/notifications` p95 为 10.28ms，高于基线 9.08ms；GET `/api/operation-logs` p95 为 11.05ms，高于基线 10.43ms。按“无法证明不退化即不合并”的规则，该删除项撤回。

后续建议：该字段仍是合理的重构候选，但应在更稳定的 JMeter/独立数据库环境中重新验证后再删除。

### 批次 3：`logout` 无效参数移除

候选变更：

- 将 `AuthService.logout(String authorizationHeader)` 改为 `logout()`。
- Controller 不再向 Service 传递未使用的 Authorization Header。

结果：

| 项目 | 结果 |
| --- | --- |
| 功能测试 | 通过 |
| Checkstyle | 通过 |
| Smoke test | 通过，退出后旧 Token 仍返回 `401/A0401` |
| 性能门控 | 未通过 |
| 是否保留 | 不保留，已回滚 |

未保留原因：第二轮复测仍有核心指标高于规范化基线，例如 POST `/api/auth/login` median 为 82.32ms，高于基线 81.74ms；GET `/api/operation-logs` avg 为 9.22ms，高于基线 8.68ms；POST `/api/onboarding/applications` p95 为 22.64ms，高于基线 19.91ms。按严格门控规则，该删除项撤回。

## 未删除项说明

- 审批、RabbitMQ 通知、页面入口和操作日志：这些能力支撑 PRD 后续阶段，不作为冗余能力删除。
- `sys_permission.request_method/path`：语义上可收敛，但本轮性能证据不足，暂保留。
- `logout(String authorizationHeader)`：参数当前未被 Service 使用，但删除后未能证明性能不退化，暂保留。
- Testcontainers、Bootstrap/WebJars、AMQP 依赖：分别支撑后续集成测试、人工验收页面和 MQ 通知链路，暂不删除。

## 最终验证

最终验证时间：2026-08-04 17:12，Asia/Shanghai。

| 项目 | 结果 |
| --- | --- |
| `./mvnw clean test` | 通过，9 tests，0 failures，0 errors |
| `./mvnw checkstyle:check` | 通过，0 violations |
| 项目内 Maven 发行包检查 | 未发现项目内 `.mvn/apache-maven-3.9.11` 和 `.mvn/apache-maven-3.9.11-bin.zip` |
| 用户级 Maven 缓存检查 | 已生成在 `~/.m2/wrapper/dists/apache-maven-3.9.11` |

说明：最终验证后 `target/` 会由 Maven 构建重新生成，该目录是正常构建产物，已被 `.gitignore` 排除，最终交付前可安全清理。

## 最终结论

本轮优化严格执行“功能不能变化、性能不能退化”的要求。最终保留：

- 清理本地/构建产物，并将 Maven Wrapper 发行包迁移到用户级缓存，降低交付噪声。
- 在数据库文档中标注权限重复字段是待优化候选，避免 mentor 审核时误以为未识别该问题。

最终未保留：

- 权限重复字段删除。
- `logout` 无效参数删除。

这不是因为代码层优化方向错误，而是因为当前本机小样本性能测试无法提供足够强的“不退化”证据。后续如果使用独立数据库、固定数据快照、更大样本量和 JMeter 报告重新测试，再合并这两个候选会更稳。
