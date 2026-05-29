# PR#225: Reduce Conflict Potential of our Custom HTTP Header `assumed-roles`

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/486
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/225

## The Problem

The REST API used a custom HTTP header called `assumed-roles` for selecting the effective RBAC roles of the current subject.
This might conflict with HTTP headers, potentially even later standard headers.

We need to find a better name and for a migration period, support both names in this backend.

## The Solution

Initially we wanted to name the header `X-hsadmin-NG-assumed-roles`,
but https://www.rfc-editor.org/info/rfc6648/ suggests not to use "X-" anymore
and instead simply use well-scoped header-names.

We choose `Hostsharing-Assumed-Roles` which now works additionally to the deprecated `assumed-roles`.
During the migration period, clients can use either header.
The deprecated header `assumed-roles` will be removed from the backend later.

When both headers are sent, they must contain the same value.
Requests with conflicting values are rejected with `400 Bad Request` to avoid ambiguous role selection.

The OpenAPI contract now declares just the canonical `Hostsharing-Assumed-Roles` header.
The deprecated `assumed-roles` header remains a runtime-only migration fallback:
`AssumedRolesHeaderFilter` maps it to the canonical header when only the deprecated header is sent,
and rejects requests with conflicting dual-header values before they reach a controller.

Tests and scenario helpers were updated to use the new header where they exercise the current API contract.
Focused tests cover deprecated-header fallback, same-value dual-header requests, and conflicting dual-header requests.

## Additional Changes

One unrelated previous PR-doc was renamed to fix the date in its name.
