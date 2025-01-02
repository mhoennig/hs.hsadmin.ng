--liquibase formatted sql

--changeset michael.hoennig:rbac-statistics endDelimiter:--//

/*
    Creates a view which presents some statistics about the RBAC tables.
 */
create view rbac.statistics_v as
select no, to_char("count", '9 999 999 999') as "count", "table"
    from (select 1 as no, count(*) as "count", 'login users' as "table"
              from rbac.subject
          union
          select 2 as no, count(*) as "count", 'roles' as "table"
              from rbac.role
          union
          select 3 as no, count(*) as "count", 'permissions' as "table"
              from rbac.permission
          union
          select 4 as no, count(*) as "count", 'references' as "table"
              from  rbac.reference
          union
          select 5 as no, count(*) as "count", 'grants' as "table"
              from rbac.grant
          union
          select 6 as no, count(*) as "count", 'objects' as "table"
              from rbac.object) as totals
    order by totals.no;
--//
