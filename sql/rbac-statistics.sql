
DROP VIEW "RbacStatisticsV";
CREATE VIEW "RbacStatisticsV" AS
    SELECT no, to_char("count", '9 999 999 999') as "count", "table"
      FROM (
             select 1 as no, count(*) as "count", 'login users' as "table" from RbacUser
             UNION
             select 2 as no, count(*) as "count", 'roles' as "table" from RbacRole
             UNION
             select 3 as no, count(*) as "count", 'permissions' as "table" from RbacPermission
             UNION
             select 4 as no, count(*) as "count", 'references' as "table" from RbacReference
             UNION
             select 5 as no, count(*) as "count", 'grants' as "table" from RbacGrants
             UNION
             select 6 as no, count(*) as "count", 'objects' as "table" from RbacObject
         ) as totals
    ORDER BY totals.no;
