# PR#257: API-key organization and unified subject-name length limits

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/257
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

API-key subject names use `.` as their separator.
But the organization derivation split the name at the `-` realm-prefix delimiter.
Thus, the whole name became the organization.
Also, an API-key could not get an explicit organization, unlike USER and GROUP subjects.

The layers also disagreed about the realm-prefix length:
the accounts OpenAPI permitted 3-9 chars,
the rbac OpenAPI and the DB name-validation functions permitted 3-12 chars.

## The Solution

- The backend now derives the organization of an API_KEY subject from the part of the name before the first `.`.
  Java (`Subject.apiKeyOrganizationFromName`) and the DB default trigger (`rbac.subject_default_organization_tf`)
  implement the same rule.
- The new insert variant `RbacApiKeySubjectWithOrganizationInsert` accepts an explicit organization,
  symmetric with USER/GROUP.
  Nothing references an API_KEY by name, thus its organization need not match the name.
- All layers now use the same realm-prefix length of 3-16 chars:
  the accounts OpenAPI, the rbac OpenAPI, and the DB name-validation functions.

## Verified by

- [RbacSubjectControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/rbac/subject/RbacSubjectControllerRestTest.java)

## Merge Order

PR#258 builds on this branch.
See the merge-order checklist in the [PR#246 doc](2026-07-22-PR%23246-remote-tooling-and-login-monitoring.md).

---

## Release Note

released: 2026-07-30

### Unified length limits for subject-name realm prefixes

Since 2026-07-30, a subject-name realm prefix is accepted with 3 to 16 characters at every endpoint, instead of the differing limits before.
An API-key subject can be created with an explicit organization; without one it takes the part of its name before the first dot.
