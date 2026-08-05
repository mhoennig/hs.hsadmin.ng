# PR#247: Coop-assets read performance via a raw self-reference graph

## Related Links (Hostsharing-internal)

- Taiga-Story: --
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/247

## The Problem

`GET /api/hs/office/coopassets` is pathologically slow.
The JPA entity `HsOfficeCoopAssetsTransactionEntity` maps to the restricted view `hs_office.coopassettx_rv`
and carries four self-referential `@OneToOne` links (`revertedAssetTx`, `reversalAssetTx`,
`adoptionAssetTx`, `transferAssetTx`) that all point back to the same `_rv`-mapped entity.
Rendering a list therefore joins `coopassettx_rv` to itself up to five times, and each occurrence
re-evaluates the expensive recursive accessible-object-uuids CTE of the restricted view.
PR#240 identified this and PR#241 added the `bench_coopassettx` benchmark and CI canary; the fix
was deferred.

## Non-Goals

- No change to the restricted view `coopassettx_rv` itself; optimizing the recursive
  accessible-object-uuids query stays a separate follow-up (the big fix).
- No change to the write path (`POST`); it keeps mapping the persisted `_rv` entity, so the
  cascade-persist logic for reversal/transfer counterparts is untouched.
- No API/OpenAPI change; the response contract is unchanged.

## Background

Whoever may see a coop-asset-transaction may also see the transactions it links to — a reversal
and its reverted tx, and the transfer/adoption counterparts of a member-to-member transfer.
The restricted view is only needed to gate which **root** rows a subject may list;
resolving the links off an already-visible root does not need to be RBAC-filtered again.

## The Solution

- Read the RBAC-gated visibility from `coopassettx_rv` as a plain uuid projection
  (`findVisibleUuidsByOptionalMembershipUuidAndDateRange`) — no self-joins — and gate
  `GET`-single with `existsByUuid` on the same view.
- Resolve the self-referential graph from the raw table via new read-only entities
  `HsOfficeCoopAssetsTransactionRealEntity` (on `hs_office.coopassettx`) and
  `HsOfficeMembershipRealEntity` (on `hs_office.membership`), following the existing
  `Rbac`/`Real` split (cf. `HsOfficePartnerRealEntity`).
  The restricted view is thus paid once per request, not once per link.
- `getListOfCoopAssets`/`getSingleCoopAssetTransactionByUuid` map from the raw graph via a
  read-path twin of the existing response post-mapper; both consolidate once the mapping is
  extracted to the service that the `TODO.refa` in the controller already expects.

### Behavioral change: cross-membership links are no longer dropped

For a member-to-member `TRANSFER`, the `ADOPTION` counterpart lives on the **other** membership.
Previously the counterpart was resolved through `coopassettx_rv`, so a subject who could see the
transfer but not the adopting membership received `null` for that link (silently dropped).
Reading  the link from the raw table now returns it (uuid, member number, amount/date of the directly
linked counterpart), consistent with the premise above.

## The Scenarios

### Feature: fast, RBAC-correct coop-asset listing

#### Scenario: listing pays the restricted view once

- **Given** a subject who may list coop-asset-transactions
- **When** it `GET`s `/api/hs/office/coopassets`
- **Then** the restricted view gates only the root rows
    - **and** the self-referential links are resolved from the raw table
    - **and** the response is byte-for-byte the same as before

##### Verified by

- [HsOfficeCoopAssetsTransactionControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/hs/office/coopassets/HsOfficeCoopAssetsTransactionControllerRestTest.java)
  pins that the listing gates visibility via the uuid projection of the restricted view,
  resolves the links via the raw-table repository, and renders the same JSON as before.
- Scenario-Test [HsOfficeScenarioTests.CoopAssetsScenarios#shouldListCoopAssetsTransactionsWithResolvedLinkedTransactions](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using UseCase [ListCoopAssetsTransactions](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/coopassets/ListCoopAssetsTransactions.java)
  verifies end-to-end that a listing resolves the transfer/adoption links,
  including the counterpart which belongs to the other membership.
- [HsOfficeCoopAssetsTransactionControllerAcceptanceTest](../../src/test/java/net/hostsharing/hsadminng/hs/office/coopassets/HsOfficeCoopAssetsTransactionControllerAcceptanceTest.java)
  still verifies the remaining read-path behavior against the real database
  (full listing row-count, date-range filtering, RBAC-gated single-`GET`).

## Open Questions

- [ ] TODO.spec: Confirm the cross-membership disclosure is intended (a limited subject now sees the linked
  counterpart transaction and its member number). If so, a scenario test with a subject that
  sees only one side should be added to pin the behavior.
