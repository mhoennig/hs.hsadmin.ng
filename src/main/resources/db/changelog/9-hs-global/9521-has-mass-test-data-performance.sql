--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-mass-test-data-PERFORMANCE context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure hs_office.bench_debitor_sepamandates(iterations int default 10)
    language plpgsql
as $$
declare
    i int;
    t0 timestamptz;
    ms numeric;
    total_limit numeric := 0;
    total_count numeric := 0;
    min_limit numeric := null;
    max_limit numeric := null;
    min_count numeric := null;
    max_count numeric := null;
    rows_read bigint;
begin
    for i in 1..iterations loop
            call base.defineContext(
                    'query debitor',
                    null,
                    'superuser-alex@hostsharing.net');

            t0 := clock_timestamp();
            select count(*) into rows_read
                            from (
                                     select d.defaultprefix, b.iban, s.validity
                                         from hs_office.debitor d
                                                  join hs_office.sepamandate_rv s on s.debitoruuid = d.uuid
                                                  join hs_office.bankaccount b on b.uuid = s.bankaccountuuid
                                         where d.defaultprefix like 'dq%'
                                         limit 10
                                 ) x;

            ms := extract(epoch from (clock_timestamp() - t0)) * 1000;
            total_limit := total_limit + ms;
            if min_limit is null or ms < min_limit then min_limit := ms; end if;
            if max_limit is null or ms > max_limit then max_limit := ms; end if;

            commit;
        end loop;

    for i in 1..iterations loop
            call base.defineContext('query debitor',
                                    null,
                                    'superuser-alex@hostsharing.net');

            t0 := clock_timestamp();
            select count(*) into rows_read
                            from hs_office.debitor d
                                     join hs_office.sepamandate_rv s on s.debitoruuid = d.uuid
                                     join hs_office.bankaccount b on b.uuid = s.bankaccountuuid;

            ms := extract(epoch from (clock_timestamp() - t0)) * 1000;
            total_count := total_count + ms;
            if min_count is null or ms < min_count then min_count := ms; end if;
            if max_count is null or ms > max_count then max_count := ms; end if;

            commit;
        end loop;

    raise notice 'limit10 min/avg/max:   % ms / % ms / % ms',
        round(min_limit, 3), round(total_limit / iterations, 3), round(max_limit, 3);

    raise notice 'count all min/avg/max: % ms / % ms / % ms',
        round(min_count, 3), round(total_count / iterations, 3), round(max_count, 3);
end $$;
--//

