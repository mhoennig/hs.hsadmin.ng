
-- ========================================================
-- Some Business Table Statistics
-- --------------------------------------------------------

DROP VIEW IF EXISTS "BusinessTableStatisticsV";
CREATE VIEW "BusinessTableStatisticsV" AS
SELECT no, to_char("count", '999 999 999') as "count", to_char("required", '999 999 999') as "required", to_char("count"::float/"required"::float, '990.999') as  "factor", "table"
FROM (select 1 as no, count(*) as "count", 7000 as "required",  'customers' as "table"
      from customer
      UNION
      select 2 as no, count(*) as "count", 15000 as "required", 'packages' as "table"
      from package
      UNION
      select 3 as no, count(*) as "count", 150000 as "required", 'unixuser' as "table"
      from unixuser
      UNION
      select 4 as no, count(*) as "count", 100000 as "required", 'domain' as "table"
      from domain
      UNION
      select 5 as no, count(*) as "count", 500000 as "required", 'emailaddress' as "table"
      from emailaddress
     ) totals
ORDER BY totals.no;

SELECT * FROM "BusinessTableStatisticsV";
