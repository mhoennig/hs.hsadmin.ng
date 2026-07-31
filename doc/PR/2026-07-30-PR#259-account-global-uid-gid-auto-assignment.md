# PR#259: Auto-assign account global uid/gid from the lowest free number

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/259
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

`POST /api/hs/accounts/accounts` required the client to provide `globalUid` and `globalGid`.
The caller had to know a free number.
That is race-prone and burdensome, especially for mass imports.

## The Solution

- `globalUid`/`globalGid` are optional on account creation.
  When the client omits both, the backend assigns the lowest unused number
  in 100000-999999 (free in both columns) to both, kept equal.
- A single given value fills the other; the backend keeps two explicitly given values as an override.
- The new gap-filling DB function `hs_accounts.next_free_global_id()` allocates the number.
  A transaction-scoped advisory lock serializes concurrent allocations.
- `tools/create-accounts-from-csv` no longer reads uid/gid columns from the CSV.
  Both derive from the new `<starting-uid-gid>` argument, +1 per line.
  Thus repeated runs stay stable as long as the line order stays stable.

## Additional Changes

- `tools/create-accounts-from-csv` now authenticates with `HSADMINNG_API_KEY` instead of a JWT username/password,
  because mass imports run under the provisioning API-key, not under a personal login.

## Verified by

- [HsAccountControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/HsAccountControllerRestTest.java)
- [HsAccountRepositoryIntegrationTest](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/HsAccountRepositoryIntegrationTest.java)

## Merge Order

Independent of the other sub-PRs extracted from PR#246; mergeable at any time before PR#246.
See the merge-order checklist in the [PR#246 doc](2026-07-22-PR%23246-remote-tooling-and-login-monitoring.md).
