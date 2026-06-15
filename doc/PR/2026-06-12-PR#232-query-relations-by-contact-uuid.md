# PR#232: Query Relations by Contact UUID

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/498
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/232

## The Problem

If users want to edit one of their Contact data records, they need to see in which Relations this Contact is used.
This is necessary to avoid accidentally changing the wrong Contact record or changing contact data too broadly.
Sometimes they might actually need to create new contact data and assign this to one or some Relations,
instead of caning the contact data which is used in several Relations.

We soon will run into similar requirements for the anchor- and holder- personUuid and probably even a generic personUiid (either anchor or holder),
thus this will be also covered with this PR.

## The Solution

The query for the REST-API endpoint `GET /api/hs/office/relations` is extended with new query parameters:

- `contactUuid`
- `anchorPersonUuid`
- `holderPersonUuid`
- `personUuid` for either the anchor or the holder, this already exists

All given query-parameters get AND-combined.
This allows filtering relations by a specific contact and or person.

## Verification

### Automated Tests

- Unit tests for repository logic and generated SQL in `HsOfficeRelationRepositoryIntegrationTest` and `HsOfficeRealRelationRepositoryIntegrationTest`.
- REST tests for controller parameter handling in `HsOfficeRelationControllerRestTest`.
- Acceptance tests for end-to-end verification with different user roles in `HsOfficeRelationControllerAcceptanceTest`.
- Scenario tests `shouldQueryRelationsByContact` and `shouldQuerySubscriptionRelationsOfGivenPerson` in `HsOfficeScenarioTests` using the generic `QueryRelations` use case.

## Additional Changes

- Fix descriptions of `personUuid` and `relationType` in `hs-office-relations.yaml`.
- Added `.junie` to `.gitignore`, temporary AI agent memory should not get commited to git.


