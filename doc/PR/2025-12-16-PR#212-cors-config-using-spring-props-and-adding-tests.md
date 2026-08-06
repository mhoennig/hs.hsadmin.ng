# PR#212: CORS-config using spring-props and adding tests

## The Problems

- CORS handling was configured via `System.getenv("ALLOWED_ORIGINS")` in `HsadminNgApplication`, which made configuration and testing harder.
- Spring Security had CORS disabled, so CORS behavior was not aligned with the security filter chain.
`/api/pong` only supported `GET`, which limited testing and client integration scenarios for CORS-enabled protected endpoints.

In total, with this PR we want the CORS configuration to work properly and to be configurable for:
- prod env
- dev env
- local env
- JUnit-based tests 

## The Solution

- Introduced a `WebMvcConfigurer` bean that reads `hsadminng.cors.allowed-origins` and applies origin and method rules for `/api/**`.
- Kept `/api/ping` explicitly open for `GET` from any origin to preserve its public health-check style behavior.
- Added CORS integration tests for preflight and actual requests, including allowed and denied origins and unauthorized token scenarios.
- Added `POST /api/pong` to the OpenAPI definition and implemented `pongPost()` in `PingController` using the same response logic as `pong()`.
- Added REST and acceptance tests for `POST /api/pong` to verify translated responses and authenticated behavior.

## Additional Changes

- Moved CORS configuration into `BaseWebSecurityConfig`, thus it's closer to related configurations.
- Included cleanup changes from rebasing and cyclic reference fixes while keeping the final behavior covered by tests.

## Attachments

None.

---

## Release Note

released: 2026-03-21

### Cross-origin requests are restricted to configured origins

Since 2025-12-16, heading towards a public API for a webapp frontende,
cross-origin browser requests to `/api/**` are only accepted from configured origins,
while `GET /api/ping` stays open to any origin.
Since then `/api/pong` also accepts `POST` for CORS testing.
