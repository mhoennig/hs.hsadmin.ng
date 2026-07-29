# PR#248: Relation holders may edit the contact data of their relations

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/248
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

The three defects in this PR have one common origin:
The RBAC graph of `hs_office` fits the Hostsharing employee who administers the data of a partner.
It does not fit the partner who maintains their own data through the API.
Such a partner acts under an assumed role, for example `hs_office.person#BesslerBert:ADMIN`.
On this self-service path, the following defects appear:

1. The holder of a relation could see the contact of the relation, but could not change its data.
   The RBAC roles of `hs_office.relation` gave only `contact:REFERRER` (read access), and only through the `TENANT` role.
   `PATCH /api/hs/office/contacts/…` failed with `[403] … is not allowed to update hs_office.contact …`.
   Only the creator of a contact row (`contact:OWNER` → `ADMIN`) or a global admin could change that row.
   This blocks the use case "the partner maintains the contact data of their own relations".
2. An object which a subject created under an assumed role stayed invisible in that same context.
   The insert trigger gave the ownership to the subject, and not to the assumed role.
3. A partner person could not list their SEPA mandates, although the mandates were accessible.
   The list query also needs the bank account of a mandate, and that bank account stayed invisible.

The technical causes are different:
a missing grant, a grant to the wrong grantee, and a grant at the wrong role level.
But the same workflow needs all three repairs, thus this PR contains them together.
The sections "The Solution", "Creator-Ownership Under Assumed Roles" and
"SEPA-Mandate Visibility For Mandate Holders" describe one defect each.

## Non-Goals

- The rules to point a relation to a different contact stay the same.
  `relation:ADMIN` still guards `contactUuid`.
  For the PARTNER relation this stays with HS.
  See the `TODO.rbac` about a possible future `relation:PROXY` role.
- The visibility of contacts stays the same.
  Relation holders still cannot see unrelated contacts.

## Background

Some terms for readers who do not know the RBAC system of this project:
each business object gets the roles OWNER, ADMIN, AGENT, TENANT and REFERRER.
Grants connect these roles into one graph, also across objects.
A restricted view, for example `hs_office.contact_rv`, shows only the rows which the current context reaches in that graph.
The current context is the subject which logs in, or the assumed roles, if the request assumes roles.
Insert triggers build the roles and the grants for each new row.

The holder gets granted `relation:AGENT`.
For generic relation types the holder gets this role directly.
For a REPRESENTATIVE relation the holder gets it through `OWNER`→`ADMIN`→`AGENT`.
`AGENT` is therefore the role level with the meaning "acts for this relation".
Both sides of the relation have this level, but a simple contact does not, because `contact:ADMIN` starts at `TENANT`.

## The Solution

`HsOfficeRelationRbacEntity.rbac()` now grants `contact:ADMIN` as an outgoing sub-role of `relation:AGENT`,
in both case branches.
The generator merges the two lines into the common `defineRoleWithGrants(relation_AGENT …, outgoingSubRoles => contact_ADMIN)`.
The update trigger of the relation already makes the grants again when `contactUuid` changes.
Thus, the grant follows the new contact automatically.

## Changed Files

- RBAC specifications — `HsOfficeRelationRbacEntity` (two DSL lines, which the generator merges)
  and `HsOfficeSepaMandateEntity`.
- RBAC generator — `RolesGrantsAndPermissionsGenerator` now writes
  `call rbac.grantRoleToCurrentContext(…)` at the end of the insert trigger,
  instead of the `subjectUuids => array[rbac.currentSubjectUuid()]` argument in the role definition.
- New changeset `1052-rbac-current-context-grant.sql` with the procedure
  `rbac.grantRoleToCurrentContext`, and its include in `db.changelog-master.yaml`.
- Regenerated RBAC changesets and diagrams for all entities with a creator grant,
  in the modules `rbactest`, `hs_office` and `hs_hosting`.
  All these changesets are `runOnChange:true`.
  Only the relation and the sepamandate files change grants,
  the other files only get the new form of the insert trigger.
- Tests — the expected mermaid graphs and grant lists now contain the new edges.
  The new nested test `UpdateRelatedContactData` in `HsOfficeRelationRepositoryIntegrationTest`
  shows that the person-ADMIN of the holder can change the related contact,
  but cannot see unrelated contacts.

## Deployment Note

The insert and update triggers only apply to rows which somebody creates or changes after the deployment.
For relations which already exist, run this once, as the generated changelog shows:

    begin transaction;
      call base.defineContext('re-creating RBAC for hs_office.relation', null, '<global-admin>');
      call hs_office.relation_rebuild_rbac_system();
    commit;

## Creator-Ownership Under Assumed Roles

The holder of a relation acts under an assumed role, and thus also creates objects in that mode.
The new integration test shows this pattern: it assumes `hs_office.person#BesslerBert:ADMIN`.
The first defect alone is therefore not sufficient for the self-service use case.

If a subject created an object, for example a contact, while it acted under an assumed role,
the object stayed invisible in that same context.
The insert triggers granted OWNER to `rbac.currentSubjectUuid()`, which is the real subject.
But the context of an assumed role contains only the uuids of the assumed roles.

The generated insert triggers now call `rbac.grantRoleToCurrentContext(<table>_OWNER(NEW))` at the end of the trigger,
and no longer give `subjectUuids` to `defineRoleWithGrants`.
The new changeset `1052-rbac-current-context-grant.sql` adds this procedure.
The procedure looks at `rbac.currentSubjectOrAssumedRolesUuids()` and reacts to the type of each reference.
Without assumed roles, it grants OWNER to the subject which creates the object.
This is the same call with the same granted-by role as before, thus the behavior stays the same.
With assumed roles, OWNER goes to those roles instead.
But the procedure does not grant OWNER, if a spec-grant already makes the object reachable from the assumed role.
The procedure tests this condition with `rbac.isGranted`.
This test is also the reason for the position of the call at the end of the trigger.
Before the spec-grants, the role graph is not yet complete.
The test then finds no path, and each new object gets an unnecessary grant.
An object which a subject creates in the context of a role thus belongs to that context.
For example, all representatives of a partner can see a contact which one of them created.

## SEPA-Mandate Visibility For Mandate Holders

The third defect has the same origin, but it becomes visible in a read operation of the partner.

A partner person which reached a mandate only through `sepamandate:REFERRER`,
thus through the debitor relation, could not list that mandate.
`GET /api/hs/office/sepamandates` sorts and filters by `mandate.bankAccount.iban`.
This makes an inner join to the restricted view `bankaccount_rv` necessary.
The visibility of the bank account came only from `sepamandate:AGENT`.
Thus a viewer at REFERRER level saw the mandate in `queryAccessibleObjectUuidsOfSubjectIds`,
but the list query removed it again.
Also, the `bankAccount` in the response would have been null.

`HsOfficeSepaMandateEntity.rbac()` now grants `bankAccount:REFERRER` from `sepamandate:REFERRER`,
and no longer from `sepamandate:AGENT`.
The IBAN identifies a mandate, thus a subject which sees the mandate must also see its bank account.
`AGENT` still reaches the bank account indirectly, thus the change only adds access.
The change affects `5073-hs-office-sepamandate-rbac.sql` (`runOnChange:true`),
the regenerated diagram, and the sepamandate unit tests and grant-list tests.

Deployment: run this once for the rows which already exist.
The number of grants stays the same, only the wiring changes:

    begin transaction;
      call base.defineContext('re-creating RBAC for hs_office.sepamandate', null, '<global-admin>');
      call hs_office.sepamandate_rebuild_rbac_system();
    commit;

## Open Questions

- TODO.spec: Relations of different partners can refer to the same contact row.
  For example, one person owns two companies and uses the same contact for both relations.
  But each holder of such a relation can now change that contact data.
  A change for one partner therefore also applies to the other partner.
  Maybe the Fronend should guide the user to avoid this trap.

## Merge Order

This branch comes from the PR#246 branch (`mihoe/Taiga480-keycloak-integration-aftermaths`).

This branch needed the branch for the restricted-view access-query performance,
because both branches change the same generated RBAC changesets.
That branch moves `validCheckSum: ANY` onto its own line, where it has an effect,
and the changesets which this branch regenerates use that format.

**This dependency is fulfilled.**
`master` contains that work as commit `960e0ad5` (PR#249), and this branch contains that commit.
Thus no open dependency remains.
