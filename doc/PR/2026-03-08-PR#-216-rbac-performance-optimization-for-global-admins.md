# PR#216: RBAC Performance Optimization for Global Admins

## The Problem

We have a severe performance problem in SELECT Queries when executed as global-admin.

The cause of the performance problem is, that if a global-admin runs SELECT queries,
they can see all rows but yet the ReBAC filter is still active.
In other words, in the case of a SELECT without a WHERE-condition, 
the ReBAC access rights are checked for each row in the target table.
This is horribly expensive because that's a recursive CTE query.

There was some shortcut in the code (see `procedure rbac.generateRbacRestrictedView` before this merge commit),
but which was not really used by the query-optimizer and still the whole recursive CTE query got exectuted.
This can be seen below in [Query-Plan before](#query-plan-before).

## The Solution

To find a solution, we need mass test-data, a query-plan analysis and a refactored rekursive CTE query.

### Test-Data Generation

To be able to do performance-tests, mass test-data was needed. 
I estimated the production database contains about 400 partner records and 500 SEPA mandate records, both including old ones.
For performance-tests I needed similar test data, or better even a bit more to be future safe, e.g. about twice the quantity.

The test-data script is a stored procedure `procedure hs_office.contact_create_mass_test_data` which is also part of the test-data Liquibase profile, but just the script, no mass test-data is generated automatically.

You can actually generate mass test-data by running the following SQL commands:

```PostgresSQL
rollback; -- for the case of any previously failed transaction

-- generate test data for partner numbers 20xxx with 80% membership
call hs_office.partner_create_mass_bundle_test_data(20000, 20999, 80);

-- show some statistics about what was generated
select * from hs_statistics_v;

-- we mostly care about SEPA mandates:
select count(*) from hs_office.sepamandate;
```

The last statement will most likely show 1003, 3 from the normal test-data plus 1000 from the mass test data.

Find the statistics for the database after mass test-data generation in attachment [Mass-Data-Statistics](#attachment-mass-data-statistics).


### The Performance-Test Script

Find the performance-script in `procedure hs_office.bench_debitor_sepamandates`, 
which is now part of the Liquibase-changesets for the test-data profile. 

The test can be run this way:

```PostgreSQL
\o /dev/null
rollback;
call hs_office.bench_debitor_sepamandates(100); -- 100 is the number of loops
```

### Query Plan Analysis

To get hints about what's going wrong, I did a query-plan analysis:

```PostgreSQ
rollback;

begin;
    call base.defineContext( 'query debitor', null, 'superuser-alex@hostsharing.net' );
    \timing on
    explain analyze
    select count(*)
        from hs_office.debitor d
        join hs_office.sepamandate_rv s on (s.debitoruuid = d.uuid)
        join hs_office.bankaccount b on (b.uuid = s.bankAccountUuid);
```

#### Query-Plan before

The following was the query plan with the shortcut-optimization which was not picked up by the query-optimizer,
thus on the commit before the merge-commit for this branch.

No need to read the resulting query-plan in details, simply put, it does way too much.
If curious, you can find it [in the attachment](#attachment-query-plan-before)



#### Query-Plan after optimization with isGlobalAdmin-cache

```
Aggregate  (cost=90.27..90.28 rows=1 width=8) (actual time=14.961..14.982 rows=1 loops=1)
  ->  Hash Join  (cost=72.50..87.77 rows=1000 width=0) (actual time=7.692..13.800 rows=1003 loops=1)
        Hash Cond: (select_hs_office_sepamandate_rv.bankaccountuuid = b.uuid)
       ->  Hash Join  (cost=35.82..48.45 rows=1000 width=16) (actual time=3.945..7.648 rows=1003 loops=1)
             Hash Cond: (select_hs_office_sepamandate_rv.debitoruuid = d.uuid)
              ->  Function Scan on select_hs_office_sepamandate_rv  (cost=0.25..10.25 rows=1000 width=32) (actual time=1.018..2.277 rows=1003 loops=1)
              ->  Hash  (cost=23.03..23.03 rows=1003 width=16) (actual time=2.894..2.899 rows=1003 loops=1)
                    Buckets: 1024  Batches: 1  Memory Usage: 56kB
                    ->  Seq Scan on debitor d  (cost=0.00..23.03 rows=1003 width=16) (actual time=0.020..1.464 rows=1003 loops=1)
        ->  Hash  (cost=24.08..24.08 rows=1008 width=16) (actual time=3.734..3.738 rows=1008 loops=1)
              Buckets: 1024  Batches: 1  Memory Usage: 56kB
              ->  Seq Scan on bankaccount b  (cost=0.00..24.08 rows=1008 width=16) (actual time=0.036..1.937 rows=1008 loops=1)

Planning Time: 0.349 ms
Execution Time: 15.063 ms
```

This looks like a sensible tidy query plan for the job. 

### Performance-Comparison

#### case A) with old optimization, freshly generated schema

```
limit10 min/avg/max:   2545.297 ms / 2796.658 ms / 4433.851 ms
count all min/avg/max: 2571.942 ms / 2821.460 ms / 3658.856 ms
completed in 9 m 22 s 8 ms
```

This clearly shows the problem; up to almost 3 seconds for a single query is too long.

#### case B) with new optimization, freshly generated schema

```
limit10 min/avg/max:   2418.032 ms / 2656.825 ms / 4212.158 ms
count all min/avg/max: 2443.345 ms / 2680.387 ms / 3475.913 ms
completed in 8 m 53 s 908 ms
```

My new shortcut implementation in the recursive CTE query did not show much improvement,
probably so even no improvement as the difference is statistically just too little.

#### case C) without optimization, but cached isGlobalAdmin, freshly generated schema

Now, I went back to the previous shortcut but store the information 
if the current subject is a global admin as a session-variable.
The caching is done right when `global.defineContext()` is called.

```
limit10 min/avg/max:   383.585 ms / 425.593 ms / 1135.419 ms
count all min/avg/max: 374.289 ms / 408.141 ms / 474.026 ms
completed in 1 m 23 s 437 ms
```

As we can see, this showed some progress, but not enough.

#### case D) with new optimization + cache, freshly generated schema

Now I combined both approaches, the new shortcut and cached the isGlobalAdmin information

```
limit10 min/avg/max:   0.806 ms / 1.212 ms / 2.159 ms
count all min/avg/max: 1.305 ms / 1.805 ms / 3.469 ms
completed in 620 ms
```

This brought the breakthrough; we are now down from almost 4 seconds to below 4 milliseconds,
**faster by a factor of 1000**.

#### case E) with new optimization + cache, upgraded schema

So far, I always freshly generated the schema.
But for our production database, we need to upgrade the existing schema.
Unfortunately, parts of the improved implementation are in code that is generated
(by `procedure rbac.generateRbacRestrictedView`), thus,
not just the generator had to be updated, but also be called for each table with RBAC support.

To be on the safe side that it really worked, I ran the performance-tests again:

```
limit10 min/avg/max:   0.515 ms / 1.012 ms / 3.464 ms
count all min/avg/max: 1.116 ms / 1.801 ms / 2.614 ms
completed in 510 ms
```

Which is quite similar to case D, as expected.


### Epilogue

This performance optimization only works if the current subject is a global admin,
the global-admin role may or may not be assumed, but no lower role.

If any lower role gets assumed or the subject is not granted the global-admin role,
the rekursive CTE query still has to be executed.

This might still be a performance problem, but not as bad as in the case of a global-admin,
because normal users cannot see that many objects, nor do they have that many (indirect) grants.
Therefore, both the width and the depth of the recursion are much smaller than for global-admins.

But for users who can see very many objects, e.g. the admin of a large client,
there could still be a severe performance problem.

There are ideas for optimizing the ReBAC-system, which are described in [RBAC Performance Analysis](rbac-performance-analysis.md#the-problematically-huge-join).
But these need major changes in the RBAC system, for which we currently have no financial capacity.


## Attachments

### Attachment: Mass-Data-Statistics


| count | rbac-table | hs-table | type |
| :--- | :--- | :--- | :--- |
|        218 019 | grants |  |  |
|        168 865 | references |  |  |
|         94 370 | permissions |  |  |
|         69 242 | roles |  |  |
|         29 576 | objects |  |  |
|          7 021 | objects | hs\_booking.item |  |
|          5 253 | login users |  |  |
|          4 818 | objects | hs\_office.coopassettx |  |
|          3 212 | objects | hs\_office.coopsharetx |  |
|          3 015 | objects | hs\_office.relation |  |
|          2 017 | objects | hs\_office.person |  |
|          2 006 | objects | hs\_booking.item | MANAGED\_WEBSPACE |
|          2 006 | objects | hs\_booking.item | MANAGED\_SERVER |
|          2 006 | objects | hs\_booking.item | CLOUD\_SERVER |
|          1 620 | objects | hs\_hosting.asset |  |
|          1 012 | objects | hs\_office.contact |  |
|          1 008 | objects | hs\_office.bankaccount |  |
|          1 005 | objects | hs\_office.partner |  |
|          1 005 | objects | hs\_office.partner\_details |  |
|          1 003 | objects | hs\_office.debitor |  |
|          1 003 | objects | hs\_booking.item | PRIVATE\_CLOUD |
|          1 003 | objects | hs\_booking.project |  |
|          1 003 | objects | hs\_office.sepamandate |  |
|            803 | objects | hs\_office.membership |  |
|            180 | objects | hs\_hosting.asset | UNIX\_USER |
|             90 | objects | hs\_hosting.asset | DOMAIN\_SMTP\_SETUP |
|             90 | objects | hs\_hosting.asset | EMAIL\_ADDRESS |
|             90 | objects | hs\_hosting.asset | CLOUD\_SERVER |
|             90 | objects | hs\_hosting.asset | PGSQL\_DATABASE |
|             90 | objects | hs\_hosting.asset | MANAGED\_WEBSPACE |
|             90 | objects | hs\_hosting.asset | DOMAIN\_SETUP |
|             90 | objects | hs\_hosting.asset | MARIADB\_USER |
|             90 | objects | hs\_hosting.asset | PGSQL\_USER |
|             90 | objects | hs\_hosting.asset | DOMAIN\_MBOX\_SETUP |
|             90 | objects | hs\_hosting.asset | DOMAIN\_HTTP\_SETUP |
|             90 | objects | hs\_hosting.asset | DOMAIN\_DNS\_SETUP |
|             90 | objects | hs\_hosting.asset | MANAGED\_SERVER |
|             90 | objects | hs\_hosting.asset | PGSQL\_INSTANCE |
|             90 | objects | hs\_hosting.asset | EMAIL\_ALIAS |
|             90 | objects | hs\_hosting.asset | MARIADB\_DATABASE |
|             90 | objects | hs\_hosting.asset | MARIADB\_INSTANCE |
|             18 | objects | rbactest.domain |  |
|              9 | objects | rbactest.package |  |
|              3 | objects | rbactest.customer |  |
|              1 | objects | rbac.global |  |


### Attachment: Query-Plan before

```
Aggregate  (cost=1800394.72..1800394.73 rows=1 width=8) (actual time=801.557..801.594 rows=1 loops=1)
  ->  Hash Join  (cost=1800381.15..1800393.04 rows=669 width=0) (actual time=794.549..800.418 rows=1003 loops=1)
        Hash Cond: (target.bankaccountuuid = b.uuid)
        ->  Hash Join  (cost=1800344.47..1800354.60 rows=669 width=16) (actual time=60.423..63.940 rows=1003 loops=1)
              Hash Cond: (target.debitoruuid = d.uuid)
              ->  Sort  (cost=1800308.90..1800310.57 rows=669 width=280) (actual time=56.670..57.796 rows=1003 loops=1)
                    Sort Key: target.validity
                    Sort Method: quicksort  Memory: 87kB
                    CTE accessible_uuids
                      ->  HashAggregate  (cost=1799078.92..1799361.78 rows=28286 width=16) (never executed)
                            Group Key: perm.objectuuid
                            CTE recursive_grants
                              ->  Recursive Union  (cost=4655.41..1575457.36 rows=3199924 width=37) (never executed)
                                    ->  Subquery Scan on "*SELECT* 1"  (cost=4655.41..4720.95 rows=6554 width=37) (never executed)
                                          ->  HashAggregate  (cost=4655.41..4720.95 rows=6554 width=37) (never executed)
                                                Group Key: "grant".descendantuuid, "grant".ascendantuuid
                                                ->  Bitmap Heap Scan on "grant"  (cost=136.43..4622.27 rows=6629 width=37) (never executed)
                                                      Recheck Cond: (ascendantuuid = ANY (rbac.currentsubjectorassumedrolesuuids()))
                                                      Filter: assumed
                                                      ->  Bitmap Index Scan on grant_ascendantuuid_idx  (cost=0.00..134.77 rows=6715 width=0) (never executed)
                                                            Index Cond: (ascendantuuid = ANY (rbac.currentsubjectorassumedrolesuuids()))
                                    ->  Unique  (cost=149882.00..153873.72 rows=319337 width=37) (never executed)
                                          ->  Sort  (cost=149882.00..150680.35 rows=319337 width=37) (never executed)
                                                Sort Key: g.descendantuuid, g.ascendantuuid, ((grants.level + 1)), (base.asserttrue((grants.level < 22), ('too many grant-levels: '::text  (grants.level)::text)))
                                                ->  Merge Join  (cost=6554.45..111954.57 rows=319337 width=37) (never executed)
                                                      Merge Cond: (g.ascendantuuid = grants.descendantuuid)
                                                      ->  Index Scan using grant_ascendantuuid_idx on "grant" g  (cost=0.42..16246.48 rows=215214 width=32) (never executed)
                                                            Filter: assumed
                                                      ->  Sort  (cost=6554.03..6717.88 rows=65540 width=20) (never executed)
                                                            Sort Key: grants.descendantuuid
                                                            ->  WorkTable Scan on recursive_grants grants  (cost=0.00..1310.80 rows=65540 width=20) (never executed)
                            CTE count_check
                              ->  Result  (cost=143996.60..143996.87 rows=1 width=1) (never executed)
                                    InitPlan 2
                                      ->  Aggregate  (cost=71998.29..71998.30 rows=1 width=8) (never executed)
                                            ->  CTE Scan on recursive_grants  (cost=0.00..63998.48 rows=3199924 width=0) (never executed)
                                    InitPlan 3
                                      ->  Aggregate  (cost=71998.29..71998.30 rows=1 width=8) (never executed)
                                            ->  CTE Scan on recursive_grants recursive_grants_1  (cost=0.00..63998.48 rows=3199924 width=0) (never executed)
                            ->  Hash Join  (cost=2270.14..79353.40 rows=108518 width=16) (never executed)
                                  Hash Cond: (recursive_grants_2.descendantuuid = perm.uuid)
                                  ->  CTE Scan on recursive_grants recursive_grants_2  (cost=0.00..63998.48 rows=3199924 width=16) (never executed)
                                  ->  Hash  (cost=2230.14..2230.14 rows=3200 width=32) (never executed)
                                        ->  Hash Join  (cost=80.55..2230.14 rows=3200 width=32) (never executed)
                                              Hash Cond: (perm.objectuuid = obj.uuid)
                                              ->  Seq Scan on permission perm  (cost=0.00..1763.70 rows=94370 width=32) (never executed)
                                              ->  Hash  (cost=68.02..68.02 rows=1003 width=16) (never executed)
                                                    ->  Nested Loop  (cost=0.41..68.02 rows=1003 width=16) (never executed)
                                                          ->  CTE Scan on count_check cc  (cost=0.00..0.02 rows=1 width=0) (never executed)
                                                                Filter: valid
                                                          ->  Index Only Scan using object_objecttable_uuid_key on object obj  (cost=0.41..57.97 rows=1003 width=16) (never executed)
                                                                Index Cond: (objecttable = 'hs_office.sepamandate'::text)
                                                                Heap Fetches: 0
                    ->  Seq Scan on sepamandate target  (cost=636.44..915.72 rows=669 width=280) (actual time=1.189..54.696 rows=1003 loops=1)
                          Filter: (rbac.hasglobaladminrole() OR (ANY (uuid = (hashed SubPlan 6).col1)))
                          SubPlan 6
                            ->  CTE Scan on accessible_uuids  (cost=0.00..565.72 rows=28286 width=16) (never executed)
              ->  Hash  (cost=23.03..23.03 rows=1003 width=16) (actual time=3.680..3.683 rows=1003 loops=1)
                    Buckets: 1024  Batches: 1  Memory Usage: 56kB
                    ->  Seq Scan on debitor d  (cost=0.00..23.03 rows=1003 width=16) (actual time=0.168..1.824 rows=1003 loops=1)
        ->  Hash  (cost=24.08..24.08 rows=1008 width=16) (actual time=734.056..734.059 rows=1008 loops=1)
              Buckets: 1024  Batches: 1  Memory Usage: 56kB
              ->  Seq Scan on bankaccount b  (cost=0.00..24.08 rows=1008 width=16) (actual time=731.222..732.672 rows=1008 loops=1)
Planning Time: 3.765 ms
JIT:
  Functions: 81
  Options: Inlining true, Optimization true, Expressions true, Deforming true
  Timing: Generation 9.230 ms (Deform 3.886 ms), Inlining 201.353 ms, Optimization 312.388 ms, Emission 217.583 ms, Total 740.554 ms
Execution Time: 888.157 ms                                                       
```
