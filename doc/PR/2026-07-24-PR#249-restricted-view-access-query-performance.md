# PR#249: Single implementation for the restricted-view access query, and a faster `rbac.grant_rv`

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/242
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

The recursive "which objects may the current context access" query existed in several places:
inlined in the generated restricted views, and again in the write-triggers.
Divergent copies make the central access query hard to optimize and hard to reason,
because a fix has to be repeated in every copy.

`rbac.grant_rv` additionally recursed in the expensive direction:
it evaluated an upward `rbac.isGranted()` reachability check per grant row,
instead of computing the reachable roles once.

## The Solution

The access query has a single implementation in `rbac.queryAccessibleObjectUuidsOfSubjectIds`,
which the generators now emit calls to instead of inlining their own copies.

`rbac.grant_rv` computes one downward closure from the current subject
and selects the grants whose descendant is in that closure,
which replaces the per-row upward recursion.

The view deliberately keeps no `assumed` filter,
so that grants which are only reachable via `assumeRoles` stay visible.

The generator change also moves `validCheckSum: ANY` onto its own line inside the changeset,
because as an attribute on the `--changeset` line it has no effect.
All regenerated changesets are `runOnChange:true`, so they re-apply without a checksum conflict.

## Non-Goals

- No change to the RBAC role model or to any grant semantics;
  this is a performance and code-duplication change only.
- The remaining coop-assets read performance work is a separate branch.

## Verification

`gw rbacGenerate` reproduces the committed generated files without drift.

## Merge Order

Extracted from the PR#246 branch.
This branch has the broadest footprint in the generated RBAC SQL,
so merging it **before** the relation-holder branch
(contact ADMIN for relation AGENT, creator-ownership, SEPA-mandate visibility)
avoids a rebase of the generated files there.

Independent of the other extracted branches.
