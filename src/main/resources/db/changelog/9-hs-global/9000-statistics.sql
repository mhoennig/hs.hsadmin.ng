--liquibase formatted sql

-- ============================================================================
--changeset hs-global-object-statistics:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
CREATE VIEW hs_statistics_view AS
select *
    from (select count, "table" as "rbac-table", '' as "hs-table", '' as "type"
              from rbacstatisticsview
          union all
          select to_char(count(*)::int, '9 999 999 999') as "count", 'objects' as "rbac-table", objecttable as "hs-table", '' as "type"
              from rbacobject
              group by objecttable
          union all
          select to_char(count(*)::int, '9 999 999 999'), 'objects', 'hs_hosting_asset', type::text
              from hs_hosting_asset
              group by type
          union all
          select to_char(count(*)::int, '9 999 999 999'), 'objects', 'hs_booking_item', type::text
              from hs_booking_item
              group by type
         ) as totals order by replace(count, ' ', '')::int desc;
--//
