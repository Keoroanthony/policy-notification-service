# Policy Notification Service

A Kotlin Spring Boot microservice that receives `DENY` decisions from `policy-rule-engine` and
stores them as in-memory alert records, providing a queryable notification log for security operators.

---

## Prerequisites

| Requirement | Version                      |
|-------------|------------------------------|
| Java        | 21                           |
| Maven       | 3.9+ (wrapper included)      |

> **SDKMAN users:** `java` may not be on `PATH` in non-interactive shells.
> Either run `sdk use java <version>` first, or prefix commands with
> `JAVA_HOME="$HOME/.sdkman/candidates/java/current"`.

---

## Building & Running

### Build

```bash
./mvnw clean package
```

### Run

```bash
./mvnw spring-boot:run
```

The service starts on **http://localhost:8081** by default.

---

## Running Both Services Together

This service is designed to run alongside `policy-rule-engine` (port 8080).

**Terminal 1 — policy-rule-engine:**
```bash
cd ../tufin
./gradlew bootRun
# → Listening on http://localhost:8080
```

**Terminal 2 — policy-notification-service:**
```bash
cd policy-notification-service
./mvnw spring-boot:run
# → Listening on http://localhost:8081
```

Both services are fully independent — no shared state, no startup dependency between them.

---

## REST API

### POST `/api/v1/notifications`

Accepts a DENY decision payload and stores it as a notification record.
Called automatically by `policy-rule-engine` on every DENY — not intended for direct use.

**Request body**
```json
{
  "subject": "GUEST",
  "resource": "/admin/users",
  "action": "READ",
  "decision": "DENY",
  "matchedRuleId": "rule-002",
  "reason": "Rule 'Deny Guests Admin' matched"
}
```

> `matchedRuleId` is nullable — omit or pass `null` when the denial is a default
> (no rule matched).

**Response `201 Created`**
```json
{
  "id": "3f2a1b4c-...",
  "subject": "GUEST",
  "resource": "/admin/users",
  "action": "READ",
  "decision": "DENY",
  "matchedRuleId": "rule-002",
  "reason": "Rule 'Deny Guests Admin' matched",
  "receivedAt": "2026-07-20T12:34:56.789Z"
}
```

**Validation errors `400 Bad Request`**

| Condition                   | Message                                                          |
|-----------------------------|------------------------------------------------------------------|
| `subject` is blank          | `subject must not be blank`                                      |
| `resource` is blank         | `resource must not be blank`                                     |
| `action` is blank           | `action must not be blank`                                       |
| `decision` is not `DENY`    | `decision must be DENY — this service only accepts DENY notifications` |
| `reason` is blank           | `reason must not be blank`                                       |
| Malformed JSON body         | `Malformed or missing request body`                              |

---

### GET `/api/v1/notifications`

Returns all stored notification records. Returns `[]` when no notifications exist (never 404).

**Response `200 OK`**
```json
[
  {
    "id": "3f2a1b4c-...",
    "subject": "GUEST",
    "resource": "/admin/users",
    "action": "READ",
    "decision": "DENY",
    "matchedRuleId": "rule-002",
    "reason": "Rule 'Deny Guests Admin' matched",
    "receivedAt": "2026-07-20T12:34:56.789Z"
  }
]
```

---

### GET `/api/v1/notifications/summary`

Returns aggregate statistics over all stored notifications.

**Response `200 OK`**
```json
{
  "total": 5,
  "topSubject": "GUEST"
}
```

> `topSubject` is `null` when there are no notifications yet.
> Tie-breaking: when two subjects share the same DENY count, the one that
> appears first in insertion order wins.

---

## Architecture

```
com.tufin.notificationservice/
├── controller/     HTTP layer — routes requests, returns DTOs
├── service/        Business logic, domain mapping
├── repository/     Thread-safe in-memory store (CopyOnWriteArrayList)
├── domain/         NotificationRecord — core immutable domain type
├── dto/            Request / response data transfer objects
└── exception/      GlobalExceptionHandler + ErrorResponse
```

---

## Design Decisions

### A. Timestamp: `Instant` (not `LocalDateTime`)

**Decision:** `receivedAt: Instant`

**Reasoning:**
- `Instant` is a UTC point in time — timezone-agnostic and unambiguous.
- `LocalDateTime` carries no timezone context, which causes silent data
  corruption in multi-region deployments or when services run in different JVM timezones.
- Security/audit records must be unambiguous about *when* an event occurred.
  A DENY notification that says "12:34" with no timezone is not actionable.

**Checked before deciding:**
- `policy-rule-engine` uses `Instant` for both `Rule.createdAt` and
  `EvaluationHistoryEntry.timestamp` — this service is consistent with that choice.
- `ErrorResponse.timestamp` in the rule engine is also `Instant.now().toString()`.

**Serialization:** Jackson serializes `Instant` to ISO-8601 UTC (e.g. `2026-07-20T12:34:56.789Z`)
by default when `jackson-module-kotlin` is on the classpath.

---

### B. `reason`: non-nullable `String` (not `String?`)

**Decision:** `reason: String`

**Reasoning:**
- A DENY notification is an actionable security alert. A null reason produces
  an incomplete audit record that an operator cannot act on.
- Every DENY *has* a reason — it is either a matched rule (e.g.
  `"Rule 'Block SSH' matched"`) or the default deny policy
  (e.g. `"No matching rule — default deny"`). Neither case is unknowable
  at the point of notification creation.
- Making `reason` non-null pushes the responsibility for clarity to the publisher,
  preventing silent gaps in the audit log.

**Trade-off considered:**
- `EvaluationResponse.matchedRuleId` and `matchedRuleName` in `policy-rule-engine`
  *are* nullable — but those are engine output fields emitted when no rule matches.
  `reason` here serves a different purpose: it is a human-readable explanation
  written for security analysts, not a machine-readable rule reference.
  The semantic difference justifies the stricter contract.

**Future consumers:**
- Alerting pipelines that filter or route by reason (e.g. `contains("SSH")`) are
  simpler and safer when the field is guaranteed non-null. No null-checks, no
  `reason?.contains(...)` chains.

---

## AI Review Task

### Prompt used to review `NotificationRecord` design

> Review this Kotlin `NotificationRecord` design for a policy alert service.
> The record represents a DENY decision received from `policy-rule-engine`,
> which produces `EvaluationResponse(decision, matchedRuleId, matchedRuleName)`.
> 
> My implementation:
> ```kotlin
> data class NotificationRecord(
>     val id: String,
>     val sourceIp: String,
>     val destinationIp: String,
>     val port: Int,
>     val decision: String,
>     val reason: String,          // non-nullable: intentional
>     val receivedAt: Instant      // Instant not LocalDateTime: intentional
> )
> ```
> 
> Design decisions made:
> 1. `receivedAt: Instant` — timezone-agnostic, consistent with the existing
>    service's use of Instant for all timestamps.
> 2. `reason: String` (non-null) — every DENY has an explainable cause;
>    null reasons create incomplete audit records.
> 
> Evaluate whether these choices are appropriate given that records originate
> from DENY decisions in policy-rule-engine. Identify risks around:
> - Jackson/Spring serialization of Instant with and without JavaTimeModule
> - Future consumers of `reason` (alerting pipelines, dashboards)
> - Whether `decision: String` vs a proper `enum` is the right choice
> - Kotlin null safety interoperability with any future Java consumers

### AI feedback received

The AI review raised three points:

1. **`Instant` serialization risk** — Without explicit `JavaTimeModule` registration,
   Jackson serializes `Instant` as a numeric epoch timestamp rather than ISO-8601.
   `jackson-module-kotlin` alone is not sufficient; `jackson-datatype-jsr310` is also needed.

   **Resolution applied:** `jackson-module-kotlin` in Spring Boot 3.x auto-configures
   `JavaTimeModule` via `JacksonAutoConfiguration`. No manual bean required. Verified
   by checking the Spring Boot 3.x auto-configuration source. Risk acknowledged in
   documentation for future manual Spring setups.

2. **`decision: String` vs enum** — The reviewer noted that `decision: String` allows
   invalid values like `"ALLOW"` or `""` to reach the domain layer if validation is
   bypassed. A `Decision` enum (matching `policy-rule-engine`'s `Decision.DENY`) would
   be more robust.

   **Resolution applied:** The DTO uses `@Pattern(regexp = "DENY")` for API-layer
   validation. The domain layer accepts `String` to avoid a shared-type dependency
   between two independent services. A future shared-contracts module could introduce
   a common `Decision` enum if a third service is added.

3. **`reason` non-null — feedback agreed** — The reviewer confirmed that non-null
   `reason` is the correct choice for an audit-first service. The `String?` alternative
   would force every downstream consumer to handle null, creating brittle null-checks
   in alerting code that is typically written quickly under incident pressure.

   **No change applied.** Decision stands.

---

## Notification Data Model

| Field           | Type     | Nullable | Description                                                           |
|-----------------|----------|----------|-----------------------------------------------------------------------|
| `id`            | String   | no       | UUID generated on receipt                                             |
| `subject`       | String   | no       | The entity that was denied (e.g. `"GUEST"`, `"USER"`)                |
| `resource`      | String   | no       | The resource that was requested (e.g. `"/admin/users"`)              |
| `action`        | String   | no       | The operation attempted (e.g. `"READ"`, `"WRITE"`)                   |
| `decision`      | String   | no       | Always `"DENY"` — this service rejects any other value               |
| `matchedRuleId` | String   | yes      | ID of the matching rule; `null` when default deny (no rule matched)  |
| `reason`        | String   | no       | Human-readable denial explanation — never null (see Design Decision B) |
| `receivedAt`    | Instant  | auto     | UTC timestamp set server-side on receipt (ISO-8601)                   |

---

## Integration Contract

`policy-rule-engine` calls this service automatically on every DENY using
`WebClientAuditServiceClient`. The call is **fire-and-forget** (async via WebClient
`.subscribe()`), which means:

- Notification delivery failures **never** delay or block the DENY response to the caller.
- If this service is unavailable, the rule engine logs a `WARN` and continues.
- If this service rejects a request (4xx), the rule engine logs an `ERROR` — this
  indicates a field-contract mismatch that requires a code fix.

### `reason` convention

The rule engine populates `reason` as follows:

| Scenario                | `reason` value                              |
|-------------------------|---------------------------------------------|
| A rule matched          | `"Rule '<ruleName>' matched"`              |
| No rule matched         | `"No matching rule — default deny"`        |

### Configuration (in `policy-rule-engine`)

```properties
notification.service.base-url=http://localhost:8081
```
