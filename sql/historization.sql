
-- ========================================================
-- Historization twiddle
-- --------------------------------------------------------

rollback;
begin transaction;
call defineContext('historization testing', null, 'superuser-alex@hostsharing.net',
--                    'hs_booking.project#D-1000000-hshdefaultproject:ADMIN'); -- prod+test
                   'hs_booking.project#D-1000313-D-1000313defaultproject:ADMIN'); -- prod+test
--                    'hs_booking.project#D-1000300-mihdefaultproject:ADMIN'); -- prod
--                    'hs_booking.project#D-1000300-mimdefaultproject:ADMIN'); -- test
-- update hs_hosting.asset set caption='lug00 b' where identifier = 'lug00' and type = 'MANAGED_WEBSPACE'; -- prod
-- update hs_hosting.asset set caption='hsh00 A ' || now()::text where identifier = 'hsh00' and type = 'MANAGED_WEBSPACE'; -- test
-- update hs_hosting.asset set caption='hsh00 B ' || now()::text where identifier = 'hsh00' and type = 'MANAGED_WEBSPACE'; -- test

-- insert into hs_hosting.asset
--     (uuid, bookingitemuuid, type, parentassetuuid, assignedtoassetuuid, identifier, caption, config, alarmcontactuuid)
--     values
--     (uuid_generate_v4(), null, 'EMAIL_ADDRESS', 'bbda5895-0569-4e20-bb4c-34f3a38f3f63'::uuid, null,
--         'new@thi.example.org', 'some new E-Mail-Address', '{}'::jsonb, null);

delete from hs_hosting.asset where uuid='5aea68d2-3b55-464f-8362-b05c76c5a681'::uuid;
commit;

-- single version at point in time
-- set hsadminng.tx_history_txid to (select max(txid) from base.tx_context where txtimestamp<='2024-08-27 12:13:13.450821');
set hsadminng.tx_history_txid to '';
set hsadminng.tx_history_timestamp to '2024-08-29 12:42';
-- all versions
select base.tx_history_txid(), txc.txtimestamp, txc.currentSubject, txc.currentTask, haex.*
    from hs_hosting.asset_ex haex
             join base.tx_context txc on haex.txid=txc.txid
    where haex.identifier = 'test@thi.example.org';

select uuid, version, type, identifier, caption from hs_hosting.asset_hv p where identifier = 'test@thi.example.org';

select pg_current_xact_id();

