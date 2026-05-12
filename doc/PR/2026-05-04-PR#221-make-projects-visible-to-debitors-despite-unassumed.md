# PR#221: Make projects visible to debitors despite unassumed grant

See also [Taiga#459: Project per Backend API nicht sichtbar](https://plan.hostsharing.net/project/admin-hsadmin/us/459) - just for internal tracking.

## The Problem

Even though, according to the database, there is at least one project for a given debitor,
it cannot be fetched via API: 

```
curl --no-progress-meter -X 'GET' \
'http://127.0.0.1:<port>/api/hs/booking/projects?debitorUuid=<some-existing-debitorUuid>' \
-H "Authorization: Bearer $BEARER" \
|jq|less
```

See that the response is an empty JSON array.


## The Cause

To avoid that too many objects are visible at once, which might confuse the user and slows the ReBAC-system down,
the grant from a Debitor:AGENT to Project:OWNER is unassumed.
Unassumed means that the grant is only effective if the specific role (here Project:OWNER) is explicitly assumed.

Unfortunately, here we have the problem that it's hard to assume the role of an object which cannot even be fetched,
as we need the UUID of the object to assume the role.


## The Solution

The solution is to add a role Project:REFERRER, which is getting granted to the Debitor:AGENT and auto-assumed.
By this grant, the project becomes visible for debitors, not yet anything below the project.
Usually there are not that many projects, just on the next level there might be very many objects;
thus such a grant is not a problem.
Just for anything below, the owner role still needs to be assumed, which now is possible,
as the UUID of the debitor's project(s) is now known. 

This change is to be applied in the ReBAC-DSL of the HsBookingProjectRbacEntity:

```
        // existing role
        .createSubRole(TENANT, (with) -> {
            with.outgoingSubRole("debitorRel", TENANT);
            // with.permission(SELECT); moved to the REFERRER role
        })
        // new role
        .createSubRole(REFERRER, (with) -> {
            // make the project visible for debitors, but for anything below, the owner role needs to be assumed
            with.incomingSuperRole("debitorRel", AGENT);
            with.permission(SELECT);
        })
```

Also have a look at the [updated diagram](../../src/main/resources/db/changelog/6-hs-booking/620-booking-project/6203-hs-booking-project-rbac.md) 

The new test-cases:

```
@ValueSource(strings = {
    "hs_office.relation#FirstGmbH-with-DEBITOR-FirstGmbH:ADMIN", // the debitor:ADMIN, failed before
    "hs_booking.project#D-1000111-D-1000111defaultproject:OWNER", // the project:OWNER, worked before
    "" // without any assumed-roles - failed before as well
})
void debitorAdminUser_canGetRelatedBookingProjectEvenWithoutAssumingTheProjectRole(final String assumedRoles)
```

### **Attention:** This change does not directly apply to existing data.

First, we needed to re-generate the PostgreSQL code by running 
`net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRbacEntity.main`.
The generated PostgreSQL is part of the PR and applied directly to automated tests which use the Testcontainers PostgreSQL.

But furthermore, we either need to-regenerate the roles+grants for all projects or,
as there is no production data for booking+hosting yet, simply re-generate the whole test-data.

Now use the role-ID "hs_booking.project#<uuid-of-the-project>:OWNER" in the "assumed-roles" header.
This header should probably get renamed to "X-assumed-roles" or "X-hsadmin-ng-rbac-assumed-roles".
