# Backend Development Plan

## 1. Project Infrastructure

- [ ] **1.1** Package structure — `config / common / security / rbac / alert / topology / metric / rule / audit`
- [ ] **1.2** Configuration properties class (`AppProperties`) binding `app.*` from application.yml with `@ConfigurationProperties`
- [ ] **1.3** JPA audit aware — `AuditorAware` bean providing current username for `@CreatedBy` / `@LastModifiedBy`
- [ ] **1.4** JPA config — enable JPA auditing (`@EnableJpaAuditing`)

## 2. Common Layer — Response & Exception Handling

- [ ] **2.1** `ApiResponse<T>` generic envelope — `{ code, msg, data }` (code = HTTP status code)
- [ ] **2.2** `ApiResponseBuilder` / static factory methods — `ApiResponse.success(data)`, `ApiResponse.error(code, msg)`
- [ ] **2.3** Custom exception hierarchy — `BusinessException(ErrorCode)` base, `ResourceNotFoundException`, `BadRequestException`, `ForbiddenException`, `ConflictException`
- [ ] **2.4** `ErrorCode` enum — unique error codes per module (e.g. `AUTH_001`, `ALERT_001`)
- [ ] **2.5** `GlobalExceptionHandler` (`@RestControllerAdvice`) — map exceptions to `ApiResponse` with correct HTTP status
- [ ] **2.6** Validation error handler — extract `MethodArgumentNotValidException` / `ConstraintViolationException` to field-level error list
- [ ] **2.7** Request/response logging filter — log method, URI, status, duration (exclude health endpoints)

## 3. Security & RBAC

- [ ] **3.1** Spring Security config — `SecurityFilterChain` with stateless session, permit actuator health, secure everything else
- [ ] **3.2** `UserDetailsServiceImpl` — load user by username from `users` table (JOIN roles/permissions)
- [ ] **3.3** Password encoder bean — `PasswordEncoder` (BCrypt delegated, compatible with `{bcrypt}` prefix stored in DB)
- [ ] **3.4** JPA entities — `UserEntity`, `RoleEntity`, `PermissionEntity` with `@ManyToMany` mappings
- [ ] **3.5** User repository — `findByUsername`
- [ ] **3.6** Authentication provider — `DaoAuthenticationProvider` wired with `UserDetailsService` + `PasswordEncoder`
- [ ] **3.7** Login endpoint — `POST /api/auth/login` returns session cookie (Redis-backed session)
- [ ] **3.8** Logout endpoint — `POST /api/auth/logout` invalidates session
- [ ] **3.9** `GET /api/auth/me` — return current user info + permissions
- [ ] **3.10** Role hierarchy — admin > operator > viewer (or simple flat roles for now)
- [ ] **3.11** Method-level authorization — `@PreAuthorize("hasAuthority('alerts:manage')")` on service methods
- [ ] **3.12** User management CRUD — `POST/GET/PUT/DELETE /api/users` (admin only)
- [ ] **3.13** Role CRUD — `POST/GET/PUT/DELETE /api/roles`
- [ ] **3.14** Permission CRUD — `POST/GET/PUT/DELETE /api/permissions`
- [ ] **3.15** Audit every auth event — login success/fail, logout, user CRUD → `audit_logs` table

## 4. Domain Entities & DTOs

- [ ] **4.1** `AlertEntity` — JPA entity mapping `alerts` table
- [ ] **4.2** `TopologyEventEntity` — mapping `topology_events`
- [ ] **4.3** `TrafficMetricEntity` — mapping `traffic_metrics`
- [ ] **4.4** `RuleEntity` — mapping `rules` table
- [ ] **4.5** `RuleMetadataEntity` — mapping `rule_metadata` (embedded or separate)
- [ ] **4.6** `AuditLogEntity` — mapping `audit_logs`
- [ ] **4.7** Repository interfaces — `JpaRepository` (or `JpaSpecificationExecutor` for dynamic queries) for each entity
- [ ] **4.8** DTOs — separate request/response objects for each module (no entity exposure in API)

## 5. Alert Management

- [ ] **5.1** `GET /api/alerts` — paginated list with filters: severity, status, alertType, sourceIp, destIp, time range
- [ ] **5.2** `GET /api/alerts/{id}` — single alert detail with raw_payload
- [ ] **5.3** `PATCH /api/alerts/{id}/status` — transition status (new→acknowledged→resolved / escalated / false_positive)
- [ ] **5.4** Severity upgrade `POST /api/alerts/{id}/escalate` — set severity to `critical`, status to `escalated`
- [ ] **5.5** Alert statistics — `GET /api/alerts/stats` — counts by severity, status, time buckets (for dashboard charts)
- [ ] **5.6** Alert search specification — dynamic query builder with `Specification<AlertEntity>`

## 6. Topology Events

- [ ] **6.1** `GET /api/topology/events` — paginated list with filters: eventType, deviceId, status, time range
- [ ] **6.2** `GET /api/topology/events/{id}` — single event detail
- [ ] **6.3** `GET /api/topology/devices` — distinct device list (from topology_events, latest status per device)
- [ ] **6.4** `GET /api/topology/devices/{deviceId}/history` — event timeline for a specific device

## 7. Traffic Metrics

- [ ] **7.1** `GET /api/metrics/traffic` — paginated list with filters: sourceIp, destIp, protocol, time range
- [ ] **7.2** `GET /api/metrics/traffic/{id}` — single metric detail
- [ ] **7.3** Traffic stats — `GET /api/metrics/traffic/stats` — aggregate: total bytes/packets per source/dest/protocol in time window
- [ ] **7.4** Top-N queries — `GET /api/metrics/traffic/top-sources` / `top-destinations` (by bytes/packets)

## 8. Rule Management (Policy Engine)

- [ ] **8.1** `GET /api/rules` — paginated list with filters: ruleType, action, enabled, priority range
- [ ] **8.2** `GET /api/rules/{id}` — single rule with metadata key-values
- [ ] **8.3** `POST /api/rules` — create rule with optional metadata array
- [ ] **8.4** `PUT /api/rules/{id}` — update rule + replace metadata
- [ ] **8.5** `DELETE /api/rules/{id}` — delete rule (cascades to rule_metadata)
- [ ] **8.6** `PATCH /api/rules/{id}/enable` / `disable` — toggle without full update
- [ ] **8.7** `PUT /api/rules/{id}/priority` — reorder priority (drag-and-drop support for UI)
- [ ] **8.8** Rule validation — validate `rule_config` JSON structure per `rule_type` (reject invalid config)
- [ ] **8.9** Rule push — `POST /api/rules/{id}/push` → HTTP call to SDN controller to apply the rule

## 9. Audit Logs

- [ ] **9.1** `GET /api/audit-logs` — paginated list with filters: action, username, resource, time range
- [ ] **9.2** `GET /api/audit-logs/{id}` — single audit log detail
- [ ] **9.3** Audit log retention — schedule or endpoint to purge logs older than 90 days (matching Kafka topic retention)

## 10. Kafka Consumer Layer

- [ ] **10.1** `AlertConsumer` — consume `ics.threat.alerts` → deserialize JSON → persist to `alerts` table (trace_id for idempotency)
- [ ] **10.2** `TopologyEventConsumer` — consume `ics.topology.events` → persist to `topology_events`
- [ ] **10.3** `TrafficMetricConsumer` — consume `ics.traffic.metrics` → persist to `traffic_metrics`
- [ ] **10.4** Consumer error handling — dead-letter topic or retry with backoff; log and skip malformed messages
- [ ] **10.5** Idempotency — use `trace_id` (UNIQUE in alerts/topology/traffic) to skip duplicate messages

## 11. Kafka Producer Layer

- [ ] **11.1** `ModelEventProducer` — send to `ics.model.events` (when admin triggers model retrain / hot-reload)
- [ ] **11.2** `AuditLogProducer` — send to `ics.audit.logs` (outgoing audit events for external systems)
- [ ] **11.3** Producer config — `KafkaTemplate` with async send + callback logging

## 12. SDN Controller Integration (HTTP Client)

- [ ] **12.1** `SdnControllerClient` — `RestClient` / `WebClient` bean configured with controller base URL
- [ ] **12.2** Flow block — `POST /api/sdn/flows/block` → controller
- [ ] **12.3** Flow revoke — `DELETE /api/sdn/flows/{flowId}` → controller
- [ ] **12.4** Rule push — send rule config JSON to controller
- [ ] **12.5** Circuit breaker / retry — `Resilience4j` on controller HTTP calls (controller may be unavailable)
- [ ] **12.6** Controller health check — proxy or aggregate in backend health endpoint

## 13. Redis Integration

- [ ] **13.1** Rate limiting — annotation-based `@RateLimit(key, permits, window)` using Redis
- [ ] **13.2** Cache abstraction — `@Cacheable` on read-heavy endpoints (alert stats, device list) with Redis TTL
- [ ] **13.3** Session config — verify Redis session persistence survives backend restart

## 14. API Documentation

- [ ] **14.1** OpenAPI / Swagger — SpringDoc dependency + config, group by module tag
- [ ] **14.2** Endpoint descriptions — at least summary + response examples for each controller

## 15. Testing

- [ ] **15.1** Unit tests — service layer with mocked repositories
- [ ] **15.2** Repository tests — `@DataJpaTest` with Testcontainers PostgreSQL
- [ ] **15.3** Controller tests — `@WebMvcTest` with mocked services, verify security annotations
- [ ] **15.4** Security tests — verify RBAC: admin/operator/viewer access boundaries
- [ ] **15.5** Kafka consumer/producer tests — `@EmbeddedKafka` or Testcontainers Redpanda
- [ ] **15.6** Integration tests — `@SpringBootTest` with full stack (Testcontainers: PG + Redis + Redpanda)
- [ ] **15.7** Test fixtures — SQL scripts or `@Sql` annotations seeding known data per test class

## 16. DevOps & Observability

- [ ] **16.1** Health indicators — custom `HealthIndicator` for Kafka and SDN controller connectivity
- [ ] **16.2** Metrics — Micrometer timers on consumer lag, HTTP request duration, DB query time
- [ ] **16.3** Structured logging — JSON log format for production (Logback encoder), include traceId in MDC
- [ ] **16.4** Graceful shutdown — configure `server.shutdown=graceful` + keep-alive drain timeout
- [ ] **16.5** Build container image — CI-friendly multi-stage Dockerfile (already scaffolded, verify in CI)
- [ ] **16.6** Database migration CI check — verify Flyway migrations are idempotent and clean on fresh DB

## 17. Polish & Hardening

- [ ] **17.1** Pagination wrapper — `PageDTO<T>` with `content, page, size, totalElements, totalPages`
- [ ] **17.2** Input sanitization — strip/escape HTML in string fields; validate IP/CIDR format on sourceIp/destIp
- [ ] **17.3** CORS config — restrict allowed origins (web-ui dev server + production domain)
- [ ] **17.4** Content negotiation — JSON only; reject XML/other with 406
- [ ] **17.5** Rate limit on login endpoint — brute-force protection (Redis-backed)
- [ ] **17.6** CSRF — disabled (stateless API), document rationale
