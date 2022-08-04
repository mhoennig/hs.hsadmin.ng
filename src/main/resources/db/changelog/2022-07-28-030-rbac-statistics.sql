--liquibase formatted sql

--changeset rbac-statistics:1 endDelimiter:--//

/*
    Creates a view which presents some statistics about the RBAC tables.
 */
create view RbacStatisticsView as
select no, to_char("count", '9 999 999 999') as "count", "table"
    from (select 1 as no, count(*) as "count", 'login users' as "table"
              from RbacUser
          union
          select 2 as no, count(*) as "count", 'roles' as "table"
              from RbacRole
          union
          select 3 as no, count(*) as "count", 'permissions' as "table"
              from RbacPermission
          union
          select 4 as no, count(*) as "count", 'references' as "table"
              from RbacReference
          union
          select 5 as no, count(*) as "count", 'grants' as "table"
              from RbacGrants
          union
          select 6 as no, count(*) as "count", 'objects' as "table"
              from RbacObject) as totals
    order by totals.no;
--//
