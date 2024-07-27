# RBAC Performance Analysis

This describes the analysis of the legacy-data-import which took way too long, which turned out to be a problem in the RBAC-access-rights-check.


## Our Performance-Problem

During the legacy data import for hosting assets we noticed massive performance problems. The import of about 2200 hosting-assets (IP-numbers, managed-webspaces, managed- and cloud-servers) as well as the creation of booking-items and booking-projects as well as necessary office-data entities (persons, contacts, partners, debitors, relations) **took 10-25 minutes**.

We could not find a pattern, why the import mostly took about 25 minutes, but sometimes took *just* 10 minutes. The impression that it had to do with too many other parallel processes, e.g. browser with BBB or IntelliJ IDEA was proved wrong, but stopping all unnecessary processes and performing the import again.


## Preparation

### Configuring PostgreSQL 

The pg_stat_statements PostgreSQL-Extension can be used to measure how long queries take and how often they are called.

The module auto_explain can be used to automatically run EXPLAIN on long-running queries.

To use this extension and module, we extended the PostgreSQL-Docker-image:

```Dockerfile
FROM postgres:15.5-bookworm

RUN apt-get update && \
    apt-get install -y postgresql-contrib && \
    apt-get clean

COPY etc/postgresql-log-slow-queries.conf /etc/postgresql/postgresql.conf
```

And create an image from it:

```sh
docker build -t postgres-with-contrib:15.5-bookworm .
```

Then we created a config file for PostgreSQL in `etc/postgresql-log-slow-queries.conf`:

```
shared_preload_libraries = 'pg_stat_statements,auto_explain'
log_min_duration_statement = 1000
log_statement = 'all'
log_duration = on
pg_stat_statements.track = all
auto_explain.log_min_duration = '1s'  # Logs queries taking longer than 1 second
auto_explain.log_analyze = on         # Include actual run times
auto_explain.log_buffers = on         # Include buffer usage statistics
auto_explain.log_format = 'json'      # Format the log output in JSON
listen_addresses = '*'
```

And a Docker-Compose config in 'docker-compose.yml':

```
version: '3.8'

services:
  postgres:
    image: postgres-with-contrib:15.5-bookworm
    container_name: custom-postgres
    environment:
      POSTGRES_PASSWORD: password
    volumes:
      - /home/mi/Projekte/Hostsharing/hsadmin-ng/etc/postgresql-log-slow-queries.conf:/etc/postgresql/postgresql.conf
    ports:
      - "5432:5432"
    command:
      - bash
      - -c
      - >
        apt-get update &&
        apt-get install -y postgresql-contrib &&
        docker-entrypoint.sh postgres -c config_file=/etc/postgresql/postgresql.conf
```

### Activate the pg_stat_statements Extension

The pg_stat_statements extension was activated in our Liquibase-scripts: 

```
create extension if not exists "pg_stat_statements";
```

### Running the Tweaked PostgreSQL

Now we can run PostgreSQL with activated slow-query-logging:

```shell
docker-compose up -d
```

### Running the Import

Using an environment like this:

```shell
export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
export HSADMINNG_POSTGRES_ADMIN_USERNAME=postgres
export HSADMINNG_POSTGRES_ADMIN_PASSWORD=password
export HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted
export HSADMINNG_SUPERUSER=superuser-alex@hostsharing.net
```

We can now run the hosting-assets-import:

```shell
time gw-importHostingAssets
```

### Fetch the Query Statistics

And afterward we can query the statistics in PostgreSQL:

```SQL
SELECT pg_stat_statements_reset();
```


## Analysis Result

### RBAC-Access-Rights Detection query

This CTE query was run over 4000 times during a single import and takes in total the whole execution time of the import process:

```SQL
WITH RECURSIVE grants AS (
    SELECT descendantUuid, ascendantUuid, $5 AS level
        FROM RbacGrants
        WHERE assumed
            AND ascendantUuid = any(subjectIds)
    UNION ALL
    SELECT g.descendantUuid, g.ascendantUuid, grants.level + $6 AS level
        FROM RbacGrants g
        INNER JOIN grants ON grants.descendantUuid = g.ascendantUuid
        WHERE g.assumed
),
granted AS (
    SELECT DISTINCT descendantUuid
    FROM grants
)
SELECT DISTINCT perm.objectUuid
    FROM granted
    JOIN RbacPermission perm ON granted.descendantUuid = perm.uuid
    JOIN RbacObject obj ON obj.uuid = perm.objectUuid
    WHERE (requiredOp = $7 OR perm.op = requiredOp)
        AND obj.objectTable = forObjectTable
    LIMIT maxObjects+$8
```

That query is used to determine access rights of the currently active RBAC-subject(s).

We used `EXPLAIN` with a concrete version (parameters substituted with real values) of that query and got this result:

```
QUERY PLAN
Limit  (cost=6549.08..6549.35 rows=54 width=16)
  CTE grants
    ->  Recursive Union  (cost=4.32..5845.97 rows=1103 width=36)
            ->  Bitmap Heap Scan on rbacgrants  (cost=4.32..15.84 rows=3 width=36)
                  Recheck Cond: (ascendantuuid = ANY ('{ad1133dc-fbb7-43c9-8c20-0da3f89a2388}'::uuid[]))
                  Filter: assumed
                  ->  Bitmap Index Scan on rbacgrants_ascendantuuid_idx  (cost=0.00..4.32 rows=3 width=0)
                         Index Cond: (ascendantuuid = ANY ('{ad1133dc-fbb7-43c9-8c20-0da3f89a2388}'::uuid[]))
            ->  Nested Loop  (cost=0.29..580.81 rows=110 width=36)
                  ->  WorkTable Scan on grants grants_1  (cost=0.00..0.60 rows=30 width=20)
                  ->  Index Scan using rbacgrants_ascendantuuid_idx on rbacgrants g  (cost=0.29..19.29 rows=4 width=32)
                        Index Cond: (ascendantuuid = grants_1.descendantuuid)
                        Filter: assumed
  ->  Unique  (cost=703.11..703.38 rows=54 width=16)
        ->  Sort  (cost=703.11..703.25 rows=54 width=16)
              Sort Key: perm.objectuuid
              ->  Nested Loop  (cost=31.60..701.56 rows=54 width=16)
                    ->  Hash Join  (cost=31.32..638.78 rows=200 width=16)
                          Hash Cond: (perm.uuid = grants.descendantuuid)
                            ->  Seq Scan on rbacpermission perm  (cost=0.00..532.92 rows=28392  width=32)
                            ->  Hash  (cost=28.82..28.82 rows=200 width=16)
                                  ->  HashAggregate  (cost=24.82..26.82 rows=200 width=16)
                                        Group Key: grants.descendantuuid
                                        ->  CTE Scan on grants  (cost=0.00..22.06 rows=1103 width=16)
                    ->  Index Only Scan using rbacobject_objecttable_uuid_key on rbacobject obj  (cost=0.28..0.31 rows=1 width=16)
                            Index Cond: ((objecttable = 'hs_hosting_asset'::text) AND (uuid = perm.objectuuid))
```

### Office-Relation-Query

```SQL
SELECT hore1_0.uuid,a1_0.uuid,a1_0.familyname,a1_0.givenname,a1_0.persontype,a1_0.salutation,a1_0.title,a1_0.tradename,a1_0.version,c1_0.uuid,c1_0.caption,c1_0.emailaddresses,c1_0.phonenumbers,c1_0.postaladdress,c1_0.version,h1_0.uuid,h1_0.familyname,h1_0.givenname,h1_0.persontype,h1_0.salutation,h1_0.title,h1_0.tradename,h1_0.version,hore1_0.mark,hore1_0.type,hore1_0.version 
    FROM hs_office_relation_rv hore1_0
    LEFT JOIN hs_office_person_rv a1_0 ON a1_0.uuid=hore1_0.anchoruuid 
    LEFT JOIN hs_office_contact_rv c1_0 ON c1_0.uuid=hore1_0.contactuuid
    LEFT JOIN hs_office_person_rv h1_0 ON h1_0.uuid=hore1_0.holderuuid
    WHERE hore1_0.uuid=$1
```

That query on the `hs_office_relation_rv`-table joins the three references anchor-person, holder-person and contact.


### Total-Query-Time > Total-Import-Runtime

That both queries total up to more than the runtime of the import-process is most likely due to internal parallel query processing.


## Attempts to Mitigate the Problem

### VACUUM ANALYZE

In the middle of the import, we updated the PostgreSQL statistics to recalibrate the query optimizer:

```SQL
VACUUM ANALYZE;
```

This did not improve the performance.


### Improving Joins + Indexes

We were suspicious about the sequential scan over all `rbacpermission` rows which was done by PostgreSQL to execute a HashJoin strategy. Turning off that strategy by

```SQL
ALTER FUNCTION queryAccessibleObjectUuidsOfSubjectIds SET enable_hashjoin = off;
```

did not improve the performance though. The HashJoin was actually still applied, but no full table scan anymore:

```
[...]
    QUERY PLAN
    ->  Hash Join  (cost=36.02..40.78 rows=1 width=16)
         Hash Cond: (grants.descendantuuid = perm.uuid)
            ->  HashAggregate  (cost=13.32..15.32 rows=200 width=16)
                    Group Key: grants.descendantuuid
                        ->  CTE Scan on grants  (cost=0.00..11.84 rows=592 width=16)
[...]
```

The HashJoin strategy could be great if the hash-map could be kept for multiple invocations. But during an import process, of course, there are always new rows in the underlying table and the hash-map would be outdated immediately.

Also creating indexes which should suppor the RBAC query, like the following, did not improve performance:

```SQL
create index on RbacPermission (objectUuid, op);
create index on RbacPermission (opTableName, op);
```

### LAZY loading for Relation.anchorPerson/.holderPerson/

At this point, the import took 21mins with these statistics:

| query | calls | total_m | mean_ms |
|-------|-------|---------|---------|
| select hore1_0.uuid,a1_0.uuid,a1_0.familyname,a1_0.givenname,a1_0.persontype,a1_0.salutation,a1_0.title,a1_0.tradename,a1_0.version,c1_0.uuid,c1_0.caption,c1_0.emailaddresses,c1_0.phonenumbers,c1_0.postaladdress, c1_0.version,h1_0.uuid,h1_0.familyname,h1_0.givenname,h1_0.persontype,h1_0.salutation,h1_0.title,h1_0.tradename,h1_0.version,hore1_0.mark,hore1_0.type,hore1_0.version from public.hs_office_relation_rv hore1_0 left join public.hs_office_person_rv a1_0 on a1_0.uuid=hore1_0.anchoruuid left join public.hs_office_contact_rv c1_0 on c1_0.uuid=hore1_0.contactuuid left join public.hs_office_person_rv h1_0 on h1_0.uuid=hore1_0.holderuuid where hore1_0.uuid=$1 | 517 | 11 | 1282 |
| select hope1_0.uuid,hope1_0.familyname,hope1_0.givenname,hope1_0.persontype,hope1_0.salutation,hope1_0.title,hope1_0.tradename,hope1_0.version from public.hs_office_person_rv hope1_0 where hope1_0.uuid=$1 | 973 | 4 | 254 |
| select hoce1_0.uuid,hoce1_0.caption,hoce1_0.emailaddresses,hoce1_0.phonenumbers,hoce1_0.postaladdress,hoce1_0.version from public.hs_office_contact_rv hoce1_0 where hoce1_0.uuid=$1 | 973 | 4 | 253 |
| call grantRoleToRole(roleUuid, superRoleUuid, superRoleDesc.assumed) | 31316 | 0 | 1 |
| call buildRbacSystemForHsHostingAsset(NEW) | 2258 | 0 | 7 |
| select * from isGranted(array[granteeId], grantedId) | 44613 | 0 | 0 |
| insert into public.hs_hosting_asset_rv (alarmcontactuuid,assignedtoassetuuid,bookingitemuuid,caption,config,identifier,parentassetuuid,type,version,uuid) values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) | 2207 | 0 | 7 |
| insert into hs_hosting_asset (alarmcontactuuid, version, bookingitemuuid, type, parentassetuuid, assignedtoassetuuid, config, uuid, identifier, caption) values (new.alarmcontactuuid, new. version, new. bookingitemuuid, new. type, new. parentassetuuid, new. assignedtoassetuuid, new. config, new. uuid, new. identifier, new. caption) returning * | 2207 | 0 | 7 |
| insert into public.hs_office_relation_rv (anchoruuid,contactuuid,holderuuid,mark,type,version,uuid) values ($1,$2,$3,$4,$5,$6,$7) | 1261 | 0 | 9 |
| insert into hs_office_relation (uuid, version, anchoruuid, holderuuid, contactuuid, type, mark) values (new.uuid, new. version, new. anchoruuid, new. holderuuid, new. contactuuid, new. type, new. mark) returning * | 1261 | 0 | 9 |
| call buildRbacSystemForHsOfficeRelation(NEW) | 1276 | 0 | 8 |
| with recursive grants as ( select descendantUuid, ascendantUuid from RbacGrants where descendantUuid = grantedId union all select ""grant"".descendantUuid, ""grant"".ascendantUuid from RbacGrants ""grant"" inner join grants recur on recur.ascendantUuid = ""grant"".descendantUuid ) select exists ( select $3 from grants where ascendantUuid = any(granteeIds) ) or grantedId = any(granteeIds) | 47540 | 0 | 0 |
| insert into RbacGrants (grantedByTriggerOf, ascendantuuid, descendantUuid, assumed) values (currentTriggerObjectUuid(), superRoleId, subRoleId, doAssume) on conflict do nothing" | 40472 | 0 | 0 |
| insert into public.hs_booking_item_rv (caption,parentitemuuid,projectuuid,resources,type,validity,version,uuid) values ($1,$2,$3,$4,$5,$6,$7,$8) | 926 | 0 | 7 |
| insert into hs_booking_item (resources, version, projectuuid, type, parentitemuuid, validity, uuid, caption) values (new.resources, new. version, new. projectuuid, new. type, new. parentitemuuid, new. validity, new. uuid, new. caption) returning * | 926 | 0 | 7 |


The slowest query now was fetching Relations joined with Contact, Anchor-Person and Holder-Person, for all tables using the restricted (RBAC) views (_rv).

We changed these mappings from `EAGER` (default) to `LAZY` to `@ManyToOne(fetch = FetchType.LAZY)` and got this result:

| query                                                                                                                                                                                                                                                                                                                                                                                                 | calls | total (min) | mean (ms) |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------|-------------|----------|
| select hope1_0.uuid,hope1_0.familyname,hope1_0.givenname,hope1_0.persontype,hope1_0.salutation,hope1_0.title,hope1_0.tradename,hope1_0.version from public.hs_office_person_rv hope1_0 where hope1_0.uuid=$1                                                                                                                                                                                          | 1015 | 4           | 238 |
| select hore1_0.uuid,hore1_0.anchoruuid,hore1_0.contactuuid,hore1_0.holderuuid,hore1_0.mark,hore1_0.type,hore1_0.version from public.hs_office_relation_rv hore1_0 where hore1_0.uuid=$1                                                                                                                                                                                                               | 517 | 4           | 439       |
| select hoce1_0.uuid,hoce1_0.caption,hoce1_0.emailaddresses,hoce1_0.phonenumbers,hoce1_0.postaladdress,hoce1_0.version from public.hs_office_contact_rv hoce1_0 where hoce1_0.uuid=$1                                                                                                                                                                                                                  | 497 | 2           | 213 |      
| call grantRoleToRole(roleUuid, superRoleUuid, superRoleDesc.assumed)                                                                                                                                                                                                                                                                                                                                  | 31316 | 0           | 1   |      
| select * from isGranted(array[granteeId], grantedId)                                                                                                                                                                                                                                                                                                                                                  | 44613 | 0           | 0     |    
| call buildRbacSystemForHsHostingAsset(NEW)                                                                                                                                                                                                                                                                                                                                                            | 2258 | 0           | 7        | 
| insert into public.hs_hosting_asset_rv (alarmcontactuuid,assignedtoassetuuid,bookingitemuuid,caption,config,identifier,parentassetuuid,type,version,uuid) values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)                                                                                                                                                                                                     | 2207 | 0           | 7         |
| insert into hs_hosting_asset (alarmcontactuuid, version, bookingitemuuid, type, parentassetuuid, assignedtoassetuuid, config, uuid, identifier, caption) values (new.alarmcontactuuid, new. version, new. bookingitemuuid, new. type, new. parentassetuuid, new. assignedtoassetuuid, new. config, new. uuid, new. identifier, new. caption) returning *                                              | 2207 | 0           | 7 |        
| with recursive grants as ( select descendantUuid, ascendantUuid from RbacGrants where descendantUuid = grantedId union all select ""grant"".descendantUuid, ""grant"".ascendantUuid from RbacGrants ""grant"" inner join grants recur on recur.ascendantUuid = ""grant"".descendantUuid ) select exists ( select $3 from grants where ascendantUuid = any(granteeIds) ) or grantedId = any(granteeIds) | 47538 | 0           | 0 |        
 insert into public.hs_office_relation_rv (anchoruuid,contactuuid,holderuuid,mark,type,version,uuid) values ($1,$2,$3,$4,$5,$6,$7)                                                                                                                                                                                                                                                                     | 1261 | 0           | 8         |
| insert into hs_office_relation (uuid, version, anchoruuid, holderuuid, contactuuid, type, mark) values (new.uuid, new. version, new. anchoruuid, new. holderuuid, new. contactuuid, new. type, new. mark) returning *                                                                                                                                                                                 | 1261 | 0           | 8         |
| call buildRbacSystemForHsOfficeRelation(NEW)                                                                                                                                                                                                                                                                                                                                                          | 1276 | 0           | 7 |        
| insert into public.hs_booking_item_rv (caption,parentitemuuid,projectuuid,resources,type,validity,version,uuid) values ($1,$2,$3,$4,$5,$6,$7,$8)                                                                                                                                                                                                                                                      | 926 | 0           | 7 |        
| insert into hs_booking_item (resources, version, projectuuid, type, parentitemuuid, validity, uuid, caption) values (new.resources, new. version, new. projectuuid, new. type, new. parentitemuuid, new. validity, new. uuid, new. caption) returning *                                                                                                                                               | 926 | 0           | 7         |
 insert into RbacGrants (grantedByTriggerOf, ascendantuuid, descendantUuid, assumed) values (currentTriggerObjectUuid(), superRoleId, subRoleId, doAssume) on conflict do nothing                                                                                                                                                                                                                      | 40472 | 0           | 0 |

Now, finally, the total runtime of the import was down to 12 minutes. This is repeatable, where originally, the import took about 25mins in most cases and just rarely -  and for unknown reasons - 10min.

## Summary

That the import runtime is down to about 12min is repeatable, where originally, the import took about 25mins in most cases and just rarely - and for unknown reasons - just 10min.

Merging the recursive CTE query to determine the RBAC SELECT-permission, made it more clear which business-queries take the time.

Avoiding EAGER-loading where not neccessary, reduced the total runtime of the import to about the half.
