-- ========================================================
-- Some Business Table Statistics
-- --------------------------------------------------------

drop view if exists "BusinessTableStatisticsV";
create view "BusinessTableStatisticsV" as
select no,
       to_char("count", '999 999 999')                        as "count",
       to_char("required", '999 999 999')                     as "required",
       to_char("count"::float / "required"::float, '990.999') as "factor",
       "table"
    from (select 1 as no, count(*) as "count", 7000 as "required", 'customers' as "table"
              from customer
          union
          select 2 as no, count(*) as "count", 15000 as "required", 'packages' as "table"
              from package
          union
          select 3 as no, count(*) as "count", 150000 as "required", 'unixuser' as "table"
              from unixuser
          union
          select 4 as no, count(*) as "count", 100000 as "required", 'domain' as "table"
              from domain
          union
          select 5 as no, count(*) as "count", 500000 as "required", 'emailaddress' as "table"
              from emailaddress) totals
    order by totals.no;

select * from "BusinessTableStatisticsV";
