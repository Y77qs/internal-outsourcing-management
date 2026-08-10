# Week3 部署、监控与性能优化说明

## 部署拓扑

`docker-compose.yml` 已扩展为一套本地可验收环境：

| 服务 | 端口 | 用途 |
| --- | --- | --- |
| `app` | `8080` | Spring Boot 应用、Thymeleaf 页面、REST API、Actuator |
| `mysql` | `3306` | 业务数据、审计日志权威存储 |
| `redis` | `6379` | 登录态缓存、绩效并发修改锁 |
| `rabbitmq` | `5672` / `15672` | 审批通知 MQ 和管理后台 |
| `elasticsearch` | `9200` | 操作日志 best-effort 索引同步 |
| `prometheus` | `9090` | 采集 `/actuator/prometheus` 指标 |
| `grafana` | `3001`（可用 `GRAFANA_PORT` 覆盖） | 可视化看板，默认账号 `admin/admin` |

启动方式：

```bash
docker compose up -d --build
```

常用入口：

```text
http://localhost:8080/ui/login
http://localhost:8080/doc.html
http://localhost:8080/actuator/prometheus
http://localhost:9090
http://localhost:3001
http://localhost:15672
```

## Elasticsearch 日志索引

操作日志仍先写入 MySQL `operation_log`，写库成功后 best-effort 同步到 Elasticsearch 索引 `pta-operation-logs`。如果 Elasticsearch 未启动、网络异常或索引写入失败：

- 业务操作不失败。
- MySQL 审计日志仍然保留。
- `/api/operation-logs?keyword=...` 始终以 MySQL 权威日志执行多字段模糊查询，避免 ES 索引缺口造成审计漏查。

相关配置：

```yaml
app:
  elasticsearch:
    enabled: true
    url: http://localhost:9200
    index-name: pta-operation-logs
```

容器内应用通过 `ELASTICSEARCH_URL=http://elasticsearch:9200` 访问 ES。

## Prometheus / Grafana

新增依赖：

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

应用暴露：

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/metrics`
- `/actuator/prometheus`

Prometheus 抓取配置位于 `ops/prometheus/prometheus.yml`。Grafana 预置 Prometheus 数据源和 `PTA Backend Overview` 看板，覆盖 HTTP 请求吞吐、平均响应时间和 JVM 堆内存使用。

## Tomcat 性能参数

`application.yml` 已支持通过环境变量调整线程池：

| 环境变量 | 默认值 | 说明 |
| --- | ---: | --- |
| `TOMCAT_MAX_THREADS` | `200` | 最大工作线程 |
| `TOMCAT_MIN_SPARE_THREADS` | `20` | 最小空闲线程 |
| `TOMCAT_ACCEPT_COUNT` | `100` | 连接等待队列 |

Compose 中应用容器使用 `300/30/200`，用于模拟企业内部较高并发验收。

## JMeter 压测

压测模板：

```text
docs/jmeter-login-concurrency.jmx
docs/jmeter-week3-core-business.jmx
docs/jmeter-week3-write-chain.jmx
```

命令行示例：

```bash
scripts/run-week3-jmeter.sh
```

登录模板覆盖多人并发登录 `POST /api/auth/login`；核心读链路模板在 setup 阶段管理员登录一次，再并发查询工作日志、绩效和操作日志，避免同账号重复登录导致旧 token 失效；写入链路模板使用唯一外包人员变量覆盖注册/登录、提交上岗申请、审批、提交工作日志、新增和修改绩效、查询操作日志。三个模板都断言业务响应码包含 `00000`。

2026-08-10 19:46 CST 在本机 Docker Compose `8080` 端口完成实测，结果位于 `target/jmeter-results/`，摘要见 `docs/jmeter-week3-run-report.md`：

| Plan | Samples | Errors | Error rate | Avg ms | P95 ms | Max ms | Throughput/s |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `jmeter-login-concurrency` | 100 | 0 | 0.00% | 78.20 | 85 | 571 | 10.18 |
| `jmeter-week3-core-business` | 301 | 0 | 0.00% | 16.55 | 34 | 148 | 29.75 |
| `jmeter-week3-write-chain` | 46 | 0 | 0.00% | 31.72 | 106 | 126 | 5.37 |

## CI/CD 模拟

GitHub Actions 风格流水线位于 `.github/workflows/week3-ci.yml`，步骤包括：

1. Checkout。
2. 设置 JDK 21。
3. 运行 `./mvnw -q test`。
4. 运行 `./mvnw -q checkstyle:check`。
5. 打包 `./mvnw -q -DskipTests package`。
6. 执行 `docker build -t pta/internal-outsourcing-management:ci .`。

本地可用同样命令模拟企业内部 CI/CD 发布前门控。
