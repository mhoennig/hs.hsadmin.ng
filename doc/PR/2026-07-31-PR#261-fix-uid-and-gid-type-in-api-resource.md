# PR#261: Fix globalUid/globalGid API type from number to integer

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/261
- Fixes an issue introduced with PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/259

## The Problem

The OpenAPI schemas `Account` and `AccountInsert` declared `globalUid` and `globalGid` as `type: number`.
The generator thus produced a decimal Java type for these resource fields,
and the accounts API rendered them as floats in JSON responses, e.g. `"globalUid": 80100.0`.

## The Solution

Declare both properties as `type: integer` in `account-schemas.yaml`.
The generated `AccountResource` and `AccountInsertResource` now use `Integer`,
which matches the `Integer` fields of `HsAccountEntity`,
and responses render the values as plain integers.

No handwritten controller or mapper code needed changes,
because the conversion goes through `StrictMapper`.

## Verified by

- [HsAccountControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/HsAccountControllerRestTest.java)
- [HsAccountControllerAcceptanceTest](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/HsAccountControllerAcceptanceTest.java)
