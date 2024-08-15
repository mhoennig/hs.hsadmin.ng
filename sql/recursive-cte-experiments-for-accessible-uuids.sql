-- just a permanent playground to explore optimization of the central recursive CTE query for RBAC

rollback transaction;
begin transaction;
SET TRANSACTION READ ONLY;
call defineContext('performance testing', null, 'superuser-alex@hostsharing.net',
                   'hs_booking_project#D-1000000-hshdefaultproject:ADMIN');
--                    'hs_booking_project#D-1000300-mihdefaultproject:ADMIN');
select count(type) as counter, type from hs_hosting_asset_rv
    group by type
    order by counter desc;
commit transaction;




rollback transaction;
begin transaction;
SET TRANSACTION READ ONLY;
call defineContext('performance testing', null, 'superuser-alex@hostsharing.net',
     'hs_booking_project#D-1000000-hshdefaultproject:ADMIN');
--                    'hs_booking_project#D-1000300-mihdefaultproject:ADMIN');

with accessible_hs_hosting_asset_uuids as
         (with recursive
              recursive_grants as
                  (select distinct rbacgrants.descendantuuid,
                                   rbacgrants.ascendantuuid,
                                   1 as level,
                                   true
                       from rbacgrants
                       where rbacgrants.assumed
                         and (rbacgrants.ascendantuuid = any (currentsubjectsuuids()))
                   union all
                   select distinct g.descendantuuid,
                                   g.ascendantuuid,
                                   grants.level + 1 as level,
                                   assertTrue(grants.level < 22, 'too many grant-levels: ' || grants.level)
                       from rbacgrants g
                                join recursive_grants grants on grants.descendantuuid = g.ascendantuuid
                       where g.assumed),
              grant_count AS (
                SELECT COUNT(*) AS grant_count FROM recursive_grants
              ),
              count_check as (select assertTrue((select count(*) as grant_count from recursive_grants) < 300000,
                    'too many grants for current subjects: ' || (select count(*) as grant_count from recursive_grants))
                                         as valid)
          select distinct perm.objectuuid
              from recursive_grants
                       join rbacpermission perm on recursive_grants.descendantuuid = perm.uuid
                       join rbacobject obj on obj.uuid = perm.objectuuid
                       join count_check cc on cc.valid
              where obj.objecttable::text = 'hs_hosting_asset'::text)
select type,
--        count(*) as counter
       target.uuid,
--        target.version,
--        target.bookingitemuuid,
--        target.type,
--        target.parentassetuuid,
--        target.assignedtoassetuuid,
       target.identifier,
       target.caption
--        target.config,
--        target.alarmcontactuuid
    from hs_hosting_asset target
    where (target.uuid in (select accessible_hs_hosting_asset_uuids.objectuuid
                               from accessible_hs_hosting_asset_uuids))
        and target.type in ('EMAIL_ADDRESS', 'CLOUD_SERVER', 'MANAGED_SERVER', 'MANAGED_WEBSPACE')
--         and target.type = 'EMAIL_ADDRESS'
--     order by target.identifier;
--     group by type
--     order by counter desc
;
commit transaction;




rollback transaction;
begin transaction;
SET TRANSACTION READ ONLY;
call defineContext('performance testing', null, 'superuser-alex@hostsharing.net',
                   'hs_booking_project#D-1000000-hshdefaultproject:ADMIN');
--                    'hs_booking_project#D-1000300-mihdefaultproject:ADMIN');

with one_path as (with recursive path as (
        -- Base case: Start with the row where ascending equals the starting UUID
        select ascendantuuid,
               descendantuuid,
               array [ascendantuuid] as path_so_far
            from rbacgrants
            where ascendantuuid = any (currentsubjectsuuids())

        union all

        -- Recursive case: Find the next step in the path
        select c.ascendantuuid,
               c.descendantuuid,
               p.path_so_far || c.ascendantuuid
            from rbacgrants c
                     inner join
                 path p on c.ascendantuuid = p.descendantuuid
            where c.ascendantuuid != all (p.path_so_far) -- Prevent cycles
    )
      -- Final selection: Output all paths that reach the target UUID
      select distinct array_length(path_so_far, 1),
          path_so_far || descendantuuid as full_path
          from path
                   join rbacpermission perm on perm.uuid = path.descendantuuid
                   join hs_hosting_asset ha on ha.uuid = perm.objectuuid
      --    JOIN rbacrole_ev re on re.uuid = any(path_so_far)
          where ha.identifier = 'vm1068'
          order by array_length(path_so_far, 1)
          limit 1
  )
select
    (
        SELECT ARRAY_AGG(re.roleidname ORDER BY ord.idx)
            FROM UNNEST(one_path.full_path) WITH ORDINALITY AS ord(uuid, idx)
                     JOIN rbacrole_ev re ON ord.uuid = re.uuid
    ) AS name_array
    from one_path;
commit transaction;

with grants as (
    select uuid
        from rbacgrants
        where descendantuuid in (
            select uuid
                from rbacrole
                where objectuuid in (
                    select uuid
                        from hs_hosting_asset
                    --  where type = 'DOMAIN_MBOX_SETUP'
                    --  and identifier = 'example.org|MBOX'
                        where type = 'EMAIL_ADDRESS'
                          and identifier='test@example.org'
                ))
)
select * from rbacgrants_ev gev where exists ( select uuid from grants where gev.uuid = grants.uuid );

