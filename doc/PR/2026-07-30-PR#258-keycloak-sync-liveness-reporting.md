# PR#258: Keycloak subject-sync liveness reporting

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/258
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

Monitoring cannot tell whether the Keycloak subject-sync is still running or when it last ran.
The sync is an external process.
It authenticates with an API-key and calls `PUT /api/rbac/subjects/{uuid}` for each Keycloak user/group.

Its only durable trace is `base.tx_context` if anything was actually updated.
That table gets one row per journaled write and grows unbounded, which it should not if nothing really changed.
In other words: Each HTTP PUT of the sync process produces a new row in `base.tx_context`, even with same data.
The latter is actually a bug.

And we need a low-cost, credential-free signal which a monitor can poll to alert when the sync stalls.

## Non-Goals

- No retention/cleanup of the `base.tx_context` rows which accumulated before this change (separate issue);
  this PR only stops the sync from producing new no-op rows.
- No change to the sync process itself; it will post the report in a parallel change.
- The reports do not expose which subjects were synced; only a timestamp and aggregate counts become public.
- The monitoring-side consumer of the new endpoint stays in PR#246 (`tools/login-monitoring-check`),
  because it builds on the throttled-alert infrastructure which that PR introduces.

## The Scenarios

### Feature: sync-liveness reporting and polling

#### Background

- As part of this feature, a parallel change to the sync tool makes it post a short statistics report
  at the end of each run (~1 post / 2 min).
- The backend stores the reports in `rbac.subject_sync_report` and prunes them by delete-on-insert
  (7-day retention), so the table stays small.
- `reported_by` is the reporting API-key subject-name; `statistics` is the free-form report body stored as-is.

#### Scenario#258.01: The syncer records an end-of-run report

So that monitoring has a heartbeat independent of whether the run changed anything.

- **Given** an authenticated API-key with scope `rbac.subjects:sync`
- **When** it `POST`s any JSON statistics to `/api/rbac/subjects/sync-report`
- **Then** a report row is stored with the API-key as `reported_by` and the receive time as `reported_at`
    - **and** reports older than the retention window are pruned
    - **and** the response is `204 No Content`

#### Scenario#258.02: Monitoring polls the last report without credentials

So that an external monitor can alert when the sync stalls, without holding any secret.

- **Given** reports exist for an API-key
- **When** anyone (unauthenticated) `GET`s `/api/rbac/subjects/last-sync?name=<key>&interval=PT1H`
- **Then** the response is `200 OK` with `{ "timestamp": <timestamp>, "statistics": {…} }` of the most recent report within the interval
    - **and** `timestamp` (and `statistics`) are `null` if there was none (a stalled sync), distinct from a connection error (backend down)
    - **and** the statistics are aggregate run counts and never reveal which subjects were synced

##### Verified by

- [RbacSubjectControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/rbac/subject/RbacSubjectControllerRestTest.java)
- [SubjectSyncReportRepositoryIntegrationTest](../../src/test/java/net/hostsharing/hsadminng/rbac/subject/SubjectSyncReportRepositoryIntegrationTest.java)
- [ApiKeyScopeUnitTest](../../src/test/java/net/hostsharing/hsadminng/config/ApiKeyScopeUnitTest.java)

## The Solution

- The new table `rbac.subject_sync_report(uuid, reported_at, reported_by, statistics jsonb)`
  has an index on `(reported_by, reported_at)`;
  the runtime `restricted` user gets write permission.
- `POST /api/rbac/subjects/sync-report` requires authentication
  and belongs to the `rbac.subjects:sync` scope allowlist in `ApiKeyScope`.
  The body is free-form JSON (mapped to `Map`);
  the backend stores it as-is and sets `reported_by`/`reported_at`.
  Each insert prunes the rows past the retention window.
- `GET /api/rbac/subjects/last-sync` is public (`permitAll`, method-level).
  It returns the timestamp and the statistics of the last report by `name`
  within an ISO-8601 `interval` (default `PT1H`).
  The interval also bounds the query cost via the BRIN-friendly time filter.
- The endpoint contract is source-agnostic:
  it currently reads the report table and can stay unchanged if the data source evolves.
- The response carries only a `timestamp`;
  the endpoint is named `last-sync` because its source is the end-of-run report, not the individual PUTs.
- In `rbac.upsert_subject`, a `WHERE` clause now guards the `ON CONFLICT DO UPDATE`.
  It skips the update when name, organization, and deactivated-state are unchanged.
  Thus, a re-sync of an unchanged subject writes no `tx_context`/`tx_journal` row.
  (PostgreSQL fires the audit trigger on every unguarded `DO UPDATE`, even for identical values.)

## Prerequisite PRs

- PR#257 (`subject-organization-and-name-limits`): this branch builds on it,
  so this PR shows both change-sets until PR#257 is merged.

---

## Release Note

released: 2026-07-30

### Endpoints report the liveness of the Keycloak synchronization

Since 2026-07-30, an authenticated endpoint accepts the statistics of a finished Keycloak synchronization run and answers 204 No Content.
A public endpoint reports the timestamp and the aggregated statistics of the last run within a configurable interval, without naming the synchronized subjects.
