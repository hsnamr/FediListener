# Phased Implementation Plan – Specification vs. Current State

This document maps **SPECIFICATION.md** and **IMPLEMENTATION_PLAN.md** to the current codebase and defines a phased plan for everything that is still missing.

---

## Current State Summary

### ✅ Implemented (Phase 1 – Foundation)

| Area | Status | Notes |
|------|--------|--------|
| Spring Boot 3.2, Maven | Done | |
| MongoDB + entities | Done | Monitor, MonitorType, DataSource, Keyword, AccountAnalysis, Regional, Metric, FediverseInstance, ActivityPubActor |
| Repositories | Done | 7 repositories |
| Monitor CRUD API | Done | Create, List, Get, Update, Delete, Pause, Resume |
| ActivityPub client | Done | WebFinger, actor discovery (custom client) |
| Docker Compose | Done | MongoDB, Kafka, Zookeeper, Redis |
| Error handling | Done | GlobalExceptionHandler, ResourceNotFoundException |
| Validation | Done | Bean Validation on DTOs |
| API versioning | Done | Header-based (API-Version) |
| Rate limiting | Done | Spring Cloud Gateway + Redis |
| JWT config | Done | Properties present; enforcement may be partial |

### ⚠️ Gaps in Existing Features

| Item | Spec / Expected | Current | Action |
|------|------------------|--------|--------|
| Discover actor | **GET** `/api/actors/discover?resource=...` | **POST** `/api/activitypub/actors/discover` | Align method + path |
| List monitors | Query params: monitor_type_id, product_id, data_source_id, is_approved, paused, sort_by (id, name, created_at, monitor_type, data_sources_count, posts_used, status) | Only search, page, per_page, sortBy, orderBy | Add filters and sort options |
| CreateMonitorDTO | monitor_type_id (Long), data_sources (List&lt;Long&gt;) | monitorTypeId (String), dataSources (List&lt;String&gt;) | Align types and IDs |
| Update monitor | PUT **or** PATCH | Only PUT | Add PATCH if desired |
| MonitorDTO | company_id, data_sources[] | Verify and add if missing |

---

## Phase 2: ActivityPub Data Collection & Instance Management

**Goal:** Collect activities from Fediverse instances and publish them via Kafka; manage instances.

### 2.1 ActivityPub Polling & Data Collection

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 2.1.1 | Implement outbox polling (fetch activities from actor outbox URL) | §6.1, §7.1.1 | OutboxPollingService or equivalent |
| 2.1.2 | Parse ActivityStreams JSON (Create, Update, Delete, Announce, Like, etc.) | §6.2, IMPLEMENTATION_PLAN §2.1 | Activity parser / normalizer |
| 2.1.3 | Handle Collection/OrderedCollection pagination | §6.3 | Pagination support in client |
| 2.1.4 | Persist or stream collected activities (e.g. collected_activities or Kafka-only) | §7.1.1, IMPLEMENTATION_PLAN §8.1 | Model + repo or Kafka-only pipeline |
| 2.1.5 | Per-instance rate limiting and exponential backoff | §6.4, §11.1 | Rate limit + backoff in ActivityPub client |

### 2.2 Kafka Integration for Data Collection

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 2.2.1 | Producer: send new/updated monitor config to data collection engine (topic e.g. tracker-new) | §7.1.1 | Kafka producer + message DTO |
| 2.2.2 | Producer: send collected activities to topic (e.g. activities) | §7.1.1, config | Activity event producer |
| 2.2.3 | Define and document message schemas (tracker config, activity events) | §7.1.1 | Schema / DTO docs or Avro/JSON Schema |

### 2.3 Instance Management API

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 2.3.1 | GET /api/instances – list Fediverse instances | README §ActivityPub-Specific | InstanceController + service |
| 2.3.2 | CRUD for instances (optional): POST/GET/PUT/DELETE /api/instances[/{id}] | IMPLEMENTATION_PLAN §9.2 | Full CRUD if required |
| 2.3.3 | Instance health check (e.g. GET /api/instances/{id}/health or similar) | IMPLEMENTATION_PLAN §9.2 | Health check endpoint + logic |

### 2.4 ActivityPub API Alignment

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 2.4.1 | GET /api/actors/discover?resource=acct:user@instance | README §ActivityPub-Specific | Add GET discover; keep or deprecate POST |
| 2.4.2 | GET /api/actors/{actor_id}/activities?page=&per_page= | README §ActivityPub-Specific | Endpoint + service (from outbox or DB) |

**Phase 2 exit criteria:**  
ActivityPub polling works for at least one instance type; activities flow to Kafka; instance list and actor discover/activities APIs match spec/README.

---

## Phase 3: Monitor Types & Lifecycle (Business Logic) ✅

**Goal:** Implement monitor-type-specific behavior and full lifecycle (approval, pause/resume, collection control). *Implemented.*

### 3.1 Monitor Type–Specific Logic

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 3.1.1 | Keyword: matching and spam filtering (for collected activities) | §6.2.1 | Keyword matching in collection/filter pipeline |
| 3.1.2 | Account analysis: follow specific actors, excluded accounts | §6.2.2 | Account-based collection rules |
| 3.1.3 | Regional: MBR / geographic filter (if data available) | §6.2.3 | Regional filter in pipeline |
| 3.1.4 | Managed account/page: optional entities and wiring | §3.7, §6.2.4 | Models + repo if not present |

### 3.2 Monitor Lifecycle & Approval

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 3.2.1 | Approval workflow: transitions UNAPPROVED → APPROVED / REJECTED | §6.1.1–6.1.3 | State machine or service rules |
| 3.2.2 | Resume: allow only when monitor is APPROVED | §4.3.7, §6.3.3 | Validation in resume endpoint |
| 3.2.3 | Optional: approval/rejection endpoints (e.g. POST approve/reject) | §6.1.2 | Admin or internal API |
| 3.2.4 | Optional: notification hooks (e.g. Kafka) for approval/rejection | §7.3 | Event publishing |

### 3.3 List Monitors – Full Spec

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 3.3.1 | Query params: monitor_type_id, product_id, data_source_id, is_approved, paused | §4.3.2 | MonitorRepository + MonitorController |
| 3.3.2 | Sort options: id, name, created_at, monitor_type, data_sources_count, posts_used, status | §4.3.2 | Sort handling in service/repository |

### 3.4 Data Types & DTO Alignment

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 3.4.1 | CreateMonitorDTO: monitor_type_id (Long), data_sources (List&lt;Long&gt; or List&lt;String&gt; per DB IDs) | §3.9, §4.3.1 | DTO + validation + service |
| 3.4.2 | MonitorDTO: company_id (if applicable), data_sources array | §3.9 | MonitorDTO + mapper |

**Phase 3 exit criteria:**  
All monitor types drive collection/filtering correctly; list monitors supports all specified filters and sort options; resume respects approval; DTOs match spec.

---

## Phase 4: Social Listening Data & Analytics Integration

**Goal:** Implement social listening APIs and integration with analytics/processing engine via Kafka.

### 4.1 Social Listening API

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 4.1.1 | POST /api/social-listening/data – request body per SocialListeningRequestDTO | §4.4.1 | SocialListeningController + DTO |
| 4.1.2 | Validation: monitor_id, data_source, date range, max range (e.g. 30 days), widgets_names, monitor approved and not paused | §4.4.1 | Validation in service |
| 4.1.3 | Build parameters for analytics engine (start_date, end_date, monitor_id, data_source, filters, page_name, etc.) | §6.4 | ParameterPreparationService |
| 4.1.4 | Send request to analytics engine via Kafka (topic e.g. social-listening); return topic + consumer group (and similar) to client | §7.1.2, §7.1.3 | Kafka producer + response DTO (topic, consumer_group, monitor_id, etc.) |
| 4.1.5 | Response: SocialListeningResponseDTO (topic, consumer_group, monitor_id, manual_topics_enabled, monitor_topics_used) | §3.9, §4.4.1 | Response DTO + mapping |

### 4.2 Widgets / Metrics API

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 4.2.1 | GET /api/social-listening/widgets?monitor_id=&data_source=&page_name= | §4.4.2 | Endpoint + service |
| 4.2.2 | Return list of widgets (name, display_name, chart_type) for monitor type + data source | §4.4.2 | Widget DTO + Metric-based or config-based source |

### 4.3 Public API (Async) – Optional

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 4.3.1 | POST /api/public-api/social-listening/data – create async job; return job_id | §4.4.3 | Public API controller + job store |
| 4.3.2 | GET /api/public-api/social-listening/data?job_id=&monitor_id= – status + widget_data | §4.4.3 | Job status + result retrieval |

### 4.4 Filter Processing

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 4.4.1 | Support filters in social listening request: topics, sentiment, languages, users, etc. | §6.5 | Filter DTO + parsing and validation |
| 4.4.2 | Include filters in analytics engine message (e.g. JSON string) | §7.1.2 | Parameter preparation + Kafka payload |

**Phase 4 exit criteria:**  
POST social-listening/data and GET social-listening/widgets work; Kafka message format matches analytics engine expectations; filters are validated and passed through.

---

## Phase 5: Filter Management & Auth/Quota

**Goal:** Saved filters per monitor; consistent auth and quota checks.

### 5.1 Saved Filters

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 5.1.1 | GET /api/monitors/{id}/filters | §4.5.1 | FilterController + service |
| 5.1.2 | POST /api/monitors/{id}/filters – body: name, filters (JSON) | §4.5.2 | Create saved filter |
| 5.1.3 | Entity/repository for saved filters (monitor_id, name, filters, user_id?) | §4.5 | SavedFilter (or equivalent) + repo |
| 5.1.4 | Enforce filter limit per monitor if specified | §4.5.2 | Validation |

### 5.2 Authentication & Authorization

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 5.2.1 | Enforce JWT on all API endpoints (except health/public if any) | §4.2, §5.1 | Security config or filter |
| 5.2.2 | Extract user/company from JWT; apply permission checks (e.g. CREATE_MONITOR, VIEW_MONITOR, etc.) | §5.2 | Permission checks in services or controllers |
| 5.2.3 | Ensure monitor ownership (user_id) and optional company_id checks | §5.2 | Service-level checks |

### 5.3 Quota (Optional / External)

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 5.3.1 | Check monitor creation limit (e.g. max per user) before create | §6.6, §4.3.1 | Quota check in MonitorService |
| 5.3.2 | Optional: integrate with external quota/subscription service | §7.2 | Client or adapter |

**Phase 5 exit criteria:**  
Saved filters CRUD works; all endpoints protected by JWT; quota (or placeholder) applied on monitor creation.

---

## Phase 6: Testing, Documentation & NFRs

**Goal:** Reliable, documented, and observable service.

### 6.1 Testing

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 6.1.1 | Unit tests for services (Monitor, SocialListening, ParameterPreparation, ActivityPub) | §11.1 | JUnit 5 + Mockito |
| 6.1.2 | Integration tests: API flows, DB, Kafka (Testcontainers or embedded) | §11.2 | Integration test suite |
| 6.1.3 | Error scenarios: 401, 403, 404, 422, validation failures | §8.3 | Tests for GlobalExceptionHandler and validation |

### 6.2 API Documentation

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 6.2.1 | OpenAPI 3 / Swagger for all endpoints | §12.3 | springdoc-openapi or Swagger config |
| 6.2.2 | Document request/response bodies and error format per §8.1 | §8.1 | Schemas and examples |

### 6.3 Non-Functional Requirements

| # | Task | Spec Reference | Deliverable |
|---|------|----------------|--------------|
| 6.3.1 | Health: /actuator/health, readiness, liveness | §11.5 | Actuator config |
| 6.3.2 | Structured logging (e.g. JSON) and correlation IDs | §11.5 | Logging config + filter |
| 6.3.3 | Metrics: request count, latency, error rate (Prometheus/Micrometer) | §11.5 | Metrics endpoints and dashboards if needed |

**Phase 6 exit criteria:**  
Core paths covered by tests; OpenAPI describes all APIs; health and metrics in place.

---

## Implementation Order Summary

| Phase | Focus | Suggested order |
|-------|--------|------------------|
| **Phase 2** | ActivityPub polling, Kafka producers, instance API, actor discover/activities | First |
| **Phase 3** | Monitor-type logic, lifecycle/approval, list monitors filters/sort, DTO alignment | Second |
| **Phase 4** | Social listening data + widgets APIs, Kafka analytics integration, filters in payloads | Third |
| **Phase 5** | Saved filters, JWT enforcement, permissions, quota | Fourth |
| **Phase 6** | Tests, OpenAPI, health, metrics, logging | Fifth |

---

## Quick Reference: Spec vs. Codebase

- **SPECIFICATION.md** – Full API, data models, business rules, Kafka/analytics contract.
- **IMPLEMENTATION_PLAN.md** – ActivityPub-specific design and phasing.
- **README.md** – High-level features and roadmap (Phase 1 done; Phases 2–6 open).
- **This document** – Gap analysis and phased task list for what is still missing.

Use this plan to pick the next phase (e.g. Phase 2 or 4) and implement tasks in the order that best fits your analytics engine and product roadmap.
