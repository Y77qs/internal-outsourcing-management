# Week4 最终交付报告

## 交付结论

本轮将 Week3 可运行系统收束为 Week4 最终验收包，范围包括全项目覆盖率门控、本地 Docker Compose 部署验收、接口 smoke test、JMeter 压测、Elasticsearch 操作日志检索闭环、文档路径修正和项目复盘总结。未新增业务模块、REST API、DTO、数据库表或前端交互。

本次按“本地验收”口径完成，不进行截图中 SSH 云主机远程部署；`杨青硕工作总结.docx` 不更新，最终总结以本 Markdown 文档为准。

## 关键变更

- JaCoCo 从 Week3 核心类 include 门控升级为全项目生产代码行覆盖率门控，阈值仍为 80%。
- 新增 `lombok.config`，让 Lombok 生成的 getter、setter、constructor 等字节码带 `@lombok.Generated`，避免把生成代码计入手写代码覆盖率。
- 补充 40 个左右聚焦测试用例，覆盖上岗申请 Service、Controller 委托、统一响应、异常处理、JWT 过滤器、Redis 登录态、Redis 锁、RBAC、通知、ES 索引与检索、ApplicationAssembler 和配置 Bean。
- 补齐操作日志 Elasticsearch 关键词检索路径：ES 负责候选日志 ID，MySQL 负责外层过滤、LIKE 兜底、`created_at desc, id desc` 排序和分页，避免索引缺口漏审计。
- 修正 JMeter 脚本和文档路径，统一使用 `docs/implementation/jmeter-login-concurrency.jmx`、`docs/implementation/jmeter-week3-core-business.jmx`、`docs/implementation/jmeter-week3-write-chain.jmx`。
- 更新 README、实现索引、测试记录、问题清单和 JMeter 报告，使 Week4 验收入口集中可查。

## 验证记录

验证时间：2026-08-17 09:19-09:21 CST。验证环境：本机 Docker Compose，应用端口 `8080`。

| 类型 | 命令或入口 | 结果 |
| --- | --- | --- |
| 单元测试与覆盖率 | `./mvnw -q clean verify` | 88 tests，0 failures，0 errors，0 skipped；JaCoCo line 89.46% |
| 代码规范 | `./mvnw -q checkstyle:check` | 通过 |
| 前端静态检查 | `node --check src/main/resources/static/js/app.js` | 通过 |
| JMeter XML | `xmllint --noout docs/implementation/jmeter-login-concurrency.jmx docs/implementation/jmeter-week3-core-business.jmx docs/implementation/jmeter-week3-write-chain.jmx` | 通过 |
| Compose 配置 | `docker compose config -q` | 通过 |
| Diff 空白检查 | `git diff --check` | 通过 |
| 本地部署 | `docker compose up -d --build` | 镜像构建成功，`pta-app` healthy |
| 健康检查 | `/api/health`、`/actuator/health/readiness` | 返回 UP |
| 文档与页面 | `/doc.html`、`/ui/login`、`/ui/work-logs`、`/ui/performances` | HTTP 200 |
| 监控指标 | `/actuator/prometheus` | HTTP 200 |

JaCoCo 结果：

| Counter | Covered | Total | Ratio |
| --- | ---: | ---: | ---: |
| Line | 1069 | 1195 | 89.46% |
| Instruction | 5242 | 5891 | 88.98% |
| Branch | 257 | 360 | 71.39% |

JMeter 最终实测：

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 82.07 | 91 | 641 | 10.16 |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 36.40 | 67 | 1409 | 29.64 |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 37.35 | 110 | 152 | 5.32 |

JMeter 原始结果仍作为构建产物保留在 `target/jmeter-results/`，包含 `.jtl`、HTML dashboard 和 `week3-jmeter-summary.md`。

## 复盘总结

已完成内容：

- 核心业务闭环已经覆盖用户注册登录、JWT/RBAC、用户管理、上岗申请、领导审批、通知、工作日志、绩效评定、操作审计、接口文档和监控部署。
- Week4 补齐了最终验收口径：全项目 80% 覆盖率、本地部署 smoke test、JMeter 实测和可读的最终报告。
- 文档入口从 README 到 `docs/implementation/README.md` 已形成完整索引，评审可以按 PRD、ER、UML、数据库、API、测试、部署和问题清单顺序检查。

未完成或保留内容：

- 未做远程服务器部署。本次按用户确认的“文档 + 本地验收”执行。
- Elasticsearch 已参与操作日志关键词候选检索，但不是唯一数据源。历史日志未索引、ES 命中为空或 ES 不可用时，MySQL 多字段 LIKE 兜底仍参与最终查询，因此不会影响审计完整性。
- Grafana 当前看板以 JVM、HTTP 请求和基础服务指标为主，业务指标仍可继续扩展。

优化方向：

- 如 mentor 强制要求远程部署，可复用当前 Docker Compose 方案在云主机执行同样 smoke test，并追加服务器 IP、端口和截图验收记录。
- 可为 Grafana 增加待审批数量、通知失败数量、绩效修改次数等业务指标。
- 可为 Elasticsearch 增加 MySQL 历史操作日志回补任务，提高旧日志 ES 候选命中率；当前 hybrid 查询已用 MySQL LIKE 兜底保证历史日志不会漏查。

个人收获：

- 覆盖率门控要先定义统计口径。本轮从核心类覆盖率升级到全项目手写代码覆盖率后，测试需要同时覆盖业务分支和工程基础设施。
- 压测材料不仅要有 `.jmx` 文件，还要有可复现脚本、错误率门控、`.jtl` 结果和指标摘要，才能算真正闭环。
- 审计类能力要优先保证不漏数据，因此 ES 只做候选增强，最终过滤、排序和分页仍由 MySQL 权威数据完成。
