--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-mass-test-data-GENERATORS context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Loop-based mass test-data generators for office/booking/hosting/accounts.
 */
create or replace procedure hs_office.contact_create_mass_test_data(
    startCount integer,
    endCount integer,
    captionPrefix varchar default 'mass contact '
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call base.defineContext('mass contact test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            call hs_office.contact_create_test_data(captionPrefix || base.intToVarChar(t, 4));
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.person_create_mass_test_data(
    startCount integer,
    endCount integer
)
    language plpgsql as $$
declare
    idx varchar;
begin
    for t in startCount..endCount
        loop
            call base.defineContext('mass person test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            if t % 5 = 0 then
                call hs_office.person_create_test_data('NP', null, 'MassFamily' || idx, 'MassGiven' || idx, true);
            else
                call hs_office.person_create_test_data('LP', 'Mass Partner ' || idx || ' GmbH', null, null, true);
            end if;
            call hs_office.person_create_test_data('NP', null, 'MassRep' || idx, 'User' || idx, true);
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.relation_create_mass_test_data(
    startCount integer,
    endCount integer,
    mandantTradeName varchar default 'Hostsharing eG',
    contactCaptionPrefix varchar default 'mass contact '
)
    language plpgsql as $$
declare
    idx varchar;
    partnerPersonName varchar;
    representativeFamilyName varchar;
    contactCaption varchar;
    mandantPerson hs_office.person;
    partnerPerson hs_office.person;
    representativePerson hs_office.person;
    contact hs_office.contact;
begin
    select p.* into mandantPerson from hs_office.person p where p.tradeName = mandantTradeName;
    if mandantPerson is null then
        raise exception 'mandant "%" not found', mandantTradeName;
    end if;

    for t in startCount..endCount
        loop
            call base.defineContext('mass relation test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            partnerPersonName := case when t % 5 = 0 then 'MassFamily' || idx else 'Mass Partner ' || idx || ' GmbH' end;
            representativeFamilyName := 'MassRep' || idx;
            contactCaption := contactCaptionPrefix || idx;

            select p.* into partnerPerson
                from hs_office.person p
                where p.tradeName = partnerPersonName or p.familyName = partnerPersonName;
            select p.* into representativePerson from hs_office.person p where p.familyName = representativeFamilyName;
            select c.* into contact from hs_office.contact c where c.caption = contactCaption;

            if partnerPerson is null or representativePerson is null or contact is null then
                raise exception 'missing mass test base data for index %', idx;
            end if;

            if not exists (
                select 1 from hs_office.relation r
                where r.type = 'PARTNER' and r.anchorUuid = mandantPerson.uuid and r.holderUuid = partnerPerson.uuid
            ) then
                call hs_office.relation_create_test_data(partnerPersonName, 'PARTNER', mandantTradeName, contactCaption);
            end if;

            if not exists (
                select 1 from hs_office.relation r
                where r.type = 'REPRESENTATIVE' and r.anchorUuid = partnerPerson.uuid and r.holderUuid = representativePerson.uuid and r.contactUuid = contact.uuid
            ) then
                call hs_office.relation_create_test_data(representativeFamilyName, 'REPRESENTATIVE', partnerPersonName, contactCaption);
            end if;

            if not exists (
                select 1 from hs_office.relation r
                where r.type = 'DEBITOR' and r.anchorUuid = partnerPerson.uuid and r.holderUuid = partnerPerson.uuid and r.contactUuid = contact.uuid
            ) then
                call hs_office.relation_create_test_data(partnerPersonName, 'DEBITOR', partnerPersonName, contactCaption);
            end if;

            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.partner_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    mandantTradeName varchar default 'Hostsharing eG',
    contactCaptionPrefix varchar default 'mass contact '
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
    partnerPersonName varchar;
    contactCaption varchar;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass partner test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            partnerPersonName := case when t % 5 = 0 then 'MassFamily' || idx else 'Mass Partner ' || idx || ' GmbH' end;
            contactCaption := contactCaptionPrefix || idx;

            if not exists (select 1 from hs_office.partner p where p.partnerNumber = t) then
                call hs_office.partner_create_test_data(mandantTradeName, t, partnerPersonName, contactCaption);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.bankaccount_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5)
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
    v_holder varchar;
    v_iban varchar;
    v_bic varchar;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass bankaccount test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            v_holder := case when t % 5 = 0 then 'MassFamily' || idx else 'Mass Partner ' || idx || ' GmbH' end;
            v_iban := 'DE' || lpad(t::text, 20, '0');
            v_bic := 'MASSDEFF' || lpad((t % 1000)::text, 3, '0');

            if not exists (
                select 1 from hs_office.bankaccount b where b.holder = v_holder and b.iban = v_iban
            ) then
                call hs_office.bankaccount_create_test_data(v_holder, v_iban, v_bic);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.debitor_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    contactCaptionPrefix varchar default 'mass contact '
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
    partnerPersonName varchar;
    suffixNum integer;
    suffixText char(2);
    defaultPrefix char(3);
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass debitor test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            partnerPersonName := case when t % 5 = 0 then 'MassFamily' || idx else 'Mass Partner ' || idx || ' GmbH' end;
            suffixNum := 10 + (t % 90);
            suffixText := lpad(suffixNum::text, 2, '0');
            defaultPrefix := lower(
                chr(97 + ((t / 676) % 26)) ||
                chr(97 + ((t / 26) % 26)) ||
                chr(97 + (t % 26))
            );

            if not exists (
                select 1
                    from hs_office.debitor d
                    join hs_office.relation debitorRel on debitorRel.uuid = d.debitorRelUuid and debitorRel.type = 'DEBITOR'
                    join hs_office.relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid and partnerRel.type = 'PARTNER'
                    join hs_office.partner p on p.partnerRelUuid = partnerRel.uuid
                    where p.partnerNumber = t and d.debitorNumberSuffix = suffixText
            ) then
                call hs_office.debitor_create_test_data(suffixNum, partnerPersonName, contactCaptionPrefix || idx, defaultPrefix);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.sepamandate_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5)
)
    language plpgsql as $$
declare
    t integer;
    suffixText char(2);
    iban varchar;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass sepa-mandate test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            suffixText := lpad((10 + (t % 90))::text, 2, '0');
            iban := 'DE' || lpad(t::text, 20, '0');

            if not exists (
                select 1
                    from hs_office.sepamandate sm
                    join hs_office.debitor d on d.uuid = sm.debitorUuid
                    join hs_office.relation debitorRel on debitorRel.uuid = d.debitorRelUuid
                    join hs_office.relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
                    join hs_office.partner p on p.partnerRelUuid = partnerRel.uuid
                    where p.partnerNumber = t and d.debitorNumberSuffix = suffixText
            ) then
                call hs_office.sepamandate_create_test_data(t, suffixText, iban, 'mass-ref-' || t::text || '-' || suffixText);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.membership_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    withMembershipPercentage integer default 80
)
    language plpgsql as $$
declare
    t integer;
    memberSuffix char(2);
begin
    if withMembershipPercentage < 0 or withMembershipPercentage > 100 then
        raise exception 'withMembershipPercentage must be between 0 and 100';
    end if;

    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass membership test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            if (t % 100) < withMembershipPercentage then
                memberSuffix := lpad((10 + (t % 90))::text, 2, '0');
                call hs_office.membership_create_test_data(t, memberSuffix, daterange('20221001', null, '[]'), 'ACTIVE');
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.coopsharetx_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    withMembershipPercentage integer default 80
)
    language plpgsql as $$
declare
    t integer;
    memberSuffix char(2);
    v_membershipUuid uuid;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass coop-sharetx test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            if (t % 100) < withMembershipPercentage then
                memberSuffix := lpad((10 + (t % 90))::text, 2, '0');
                select m.uuid into v_membershipUuid
                    from hs_office.membership m
                    join hs_office.partner p on p.uuid = m.partnerUuid
                    where p.partnerNumber = t and m.memberNumberSuffix = memberSuffix;

                if v_membershipUuid is not null and not exists (
                    select 1 from hs_office.coopsharetx tx where tx.membershipUuid = v_membershipUuid
                ) then
                    call hs_office.coopsharetx_create_test_data(t, memberSuffix);
                end if;
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.coopassettx_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    withMembershipPercentage integer default 80
)
    language plpgsql as $$
declare
    t integer;
    memberSuffix char(2);
    v_membershipUuid uuid;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass coop-assettx test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            if (t % 100) < withMembershipPercentage then
                memberSuffix := lpad((10 + (t % 90))::text, 2, '0');
                select m.uuid into v_membershipUuid
                    from hs_office.membership m
                    join hs_office.partner p on p.uuid = m.partnerUuid
                    where p.partnerNumber = t and m.memberNumberSuffix = memberSuffix;

                if v_membershipUuid is not null and not exists (
                    select 1 from hs_office.coopassettx tx where tx.membershipUuid = v_membershipUuid
                ) then
                    call hs_office.coopassettx_create_test_data(t, memberSuffix);
                end if;
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_booking.project_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5)
)
    language plpgsql as $$
declare
    t integer;
    suffixText char(2);
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass booking-project test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            suffixText := lpad((10 + (t % 90))::text, 2, '0');
            if not exists (
                select 1 from hs_booking.project p where p.caption = 'D-' || t::text || suffixText || ' default project'
            ) then
                call hs_booking.project_create_test_data(t, suffixText);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_booking.item_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5)
)
    language plpgsql as $$
declare
    t integer;
    suffixText char(2);
    v_projectUuid uuid;
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass booking-item test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            suffixText := lpad((10 + (t % 90))::text, 2, '0');
            select p.uuid into v_projectUuid from hs_booking.project p where p.caption = 'D-' || t::text || suffixText || ' default project';
            if v_projectUuid is not null and not exists (
                select 1 from hs_booking.item i where i.projectUuid = v_projectUuid
            ) then
                call hs_booking.item_create_test_data(t, suffixText);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_hosting.asset_create_mass_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5)
)
    language plpgsql as $$
declare
    t integer;
    suffixText char(2);
    projectCaption varchar;
    v_debitorNumberSuffix char(2);
    v_defaultPrefix char(3);
begin
    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass hosting-asset test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            suffixText := lpad((10 + (t % 90))::text, 2, '0');
            projectCaption := 'D-' || t::text || suffixText || ' default project';
            select d.debitorNumberSuffix, d.defaultPrefix
                into v_debitorNumberSuffix, v_defaultPrefix
                from hs_booking.project p
                join hs_office.debitor d on d.uuid = p.debitorUuid
                where p.caption = projectCaption;
            if v_debitorNumberSuffix is not null
                and not exists (
                select 1
                    from hs_hosting.asset a
                    join hs_booking.item i on i.uuid = a.bookingItemUuid
                    join hs_booking.project p on p.uuid = i.projectUuid
                    where p.caption = projectCaption
                )
                and not exists (
                    select 1 from hs_hosting.asset a
                    where (a.type = 'MANAGED_SERVER' and a.identifier = 'vm10' || v_debitorNumberSuffix)
                       or (a.type = 'CLOUD_SERVER' and a.identifier = 'vm20' || v_debitorNumberSuffix)
                       or (a.type = 'MANAGED_WEBSPACE' and a.identifier = v_defaultPrefix || '01')
                       or (a.type = 'MARIADB_INSTANCE' and a.identifier = 'vm10' || v_debitorNumberSuffix || '.MariaDB.default')
                       or (a.type = 'MARIADB_USER' and a.identifier = v_defaultPrefix || '01_web')
                       or (a.type = 'MARIADB_DATABASE' and a.identifier = v_defaultPrefix || '01_web')
                       or (a.type = 'PGSQL_INSTANCE' and a.identifier = 'vm10' || v_debitorNumberSuffix || '.Postgresql.default')
                       or (a.type = 'PGSQL_USER' and a.identifier = v_defaultPrefix || '01_web')
                       or (a.type = 'PGSQL_DATABASE' and a.identifier = v_defaultPrefix || '01_web')
                       or (a.type = 'EMAIL_ALIAS' and a.identifier = v_defaultPrefix || '01-web')
                       or (a.type = 'UNIX_USER' and a.identifier = v_defaultPrefix || '01-web')
                       or (a.type = 'UNIX_USER' and a.identifier = v_defaultPrefix || '01-mbox')
                       or (a.type = 'DOMAIN_SETUP' and a.identifier = v_defaultPrefix || '.example.org')
                       or (a.type = 'DOMAIN_DNS_SETUP' and a.identifier = v_defaultPrefix || '.example.org|DNS')
                       or (a.type = 'DOMAIN_HTTP_SETUP' and a.identifier = v_defaultPrefix || '.example.org|HTTP')
                       or (a.type = 'DOMAIN_SMTP_SETUP' and a.identifier = v_defaultPrefix || '.example.org|SMTP')
                       or (a.type = 'DOMAIN_MBOX_SETUP' and a.identifier = v_defaultPrefix || '.example.org|MBOX')
                       or (a.type = 'EMAIL_ADDRESS' and a.identifier = 'test@' || v_defaultPrefix || '.example.org')
                ) then
                call hs_hosting.asset_create_test_data(projectCaption);
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.person_create_mass_test_data_for_accounts(
    startCount integer,
    endCount integer
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
begin
    for t in startCount..endCount
        loop
            call base.defineContext('mass account-person test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            call hs_office.person_create_test_data('NP', null, 'MassAccountFamily' || idx, 'MassAccountGiven' || idx, true);
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_accounts.account_create_mass_test_data(
    startCount integer,
    endCount integer,
    emailPrefix varchar default 'mass-account-',
    uidOffset integer default 200000
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
    accountEmail varchar;
    subjectUuid uuid;
    personUuid uuid;
begin
    for t in startCount..endCount
        loop
            call base.defineContext('mass profile test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            idx := base.intToVarChar(t, 4);
            accountEmail := emailPrefix || idx || '@example.com';
            select p.uuid into personUuid
                from hs_office.person p
                where p.familyName = 'MassAccountFamily' || idx and p.givenName = 'MassAccountGiven' || idx;

            if personUuid is not null and not exists (
                select 1 from hs_accounts.account pr where pr.person_uuid = personUuid
            ) then
                perform rbac.create_subject(accountEmail);
                select s.uuid into subjectUuid from rbac.subject s where s.name = accountEmail;

                insert into hs_accounts.account (
                    uuid, version, person_uuid,
                    global_uid, global_gid
                ) values (
                    subjectUuid, 0, personUuid,
                    uidOffset + t, uidOffset + t
                );
            end if;
            commit;
        end loop;
end; $$;
--//

create or replace procedure hs_office.partner_create_mass_bundle_test_data(
    startPartnerNumber numeric(5),
    endPartnerNumber numeric(5),
    withMembershipPercentage integer default 80
)
    language plpgsql as $$
declare
    t integer;
    idx varchar;
    personUuid uuid;
    accountEmail varchar;
    subjectUuid uuid;
begin
    call base.defineContext('creating mass partner bundle test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
    set constraints all deferred;

    call hs_office.contact_create_mass_test_data(startPartnerNumber::integer, endPartnerNumber::integer);
    call hs_office.person_create_mass_test_data(startPartnerNumber::integer, endPartnerNumber::integer);
    call hs_office.relation_create_mass_test_data(startPartnerNumber::integer, endPartnerNumber::integer);
    call hs_office.partner_create_mass_test_data(startPartnerNumber, endPartnerNumber);
    call hs_office.bankaccount_create_mass_test_data(startPartnerNumber, endPartnerNumber);
    call hs_office.debitor_create_mass_test_data(startPartnerNumber, endPartnerNumber);
    call hs_office.sepamandate_create_mass_test_data(startPartnerNumber, endPartnerNumber);

    call hs_office.membership_create_mass_test_data(startPartnerNumber, endPartnerNumber, withMembershipPercentage);
    call hs_office.coopsharetx_create_mass_test_data(startPartnerNumber, endPartnerNumber, withMembershipPercentage);
    call hs_office.coopassettx_create_mass_test_data(startPartnerNumber, endPartnerNumber, withMembershipPercentage);

    call hs_booking.project_create_mass_test_data(startPartnerNumber, endPartnerNumber);
    call hs_booking.item_create_mass_test_data(startPartnerNumber, endPartnerNumber);
    call hs_hosting.asset_create_mass_test_data(startPartnerNumber, endPartnerNumber);

    for t in startPartnerNumber::integer..endPartnerNumber::integer
        loop
            call base.defineContext('mass partner bundle account test-data #' || t, null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');
            if t % 5 = 0 then
                idx := base.intToVarChar(t, 4);
                select p.uuid into personUuid
                    from hs_office.person p
                    where p.familyName = 'MassFamily' || idx and p.givenName = 'MassGiven' || idx;

                if personUuid is not null and not exists (
                    select 1 from hs_accounts.account pr where pr.person_uuid = personUuid
                ) then
                    accountEmail := 'mass-person-' || idx || '@example.com';
                    perform rbac.create_subject(accountEmail);
                    select s.uuid into subjectUuid from rbac.subject s where s.name = accountEmail;

                    insert into hs_accounts.account (
                        uuid, version, person_uuid,
                        global_uid, global_gid
                    ) values (
                        subjectUuid, 0, personUuid,
                        300000 + t, 300000 + t
                    );
                end if;
            end if;

            idx := base.intToVarChar(t, 4);
            select p.uuid into personUuid
                from hs_office.person p
                where p.familyName = 'MassRep' || idx and p.givenName = 'User' || idx;

            if personUuid is not null and not exists (
                select 1 from hs_accounts.account pr where pr.person_uuid = personUuid
            ) then
                accountEmail := 'mass-rep-' || idx || '@example.com';
                perform rbac.create_subject(accountEmail);
                select s.uuid into subjectUuid from rbac.subject s where s.name = accountEmail;

                insert into hs_accounts.account (
                    uuid, version, person_uuid,
                    global_uid, global_gid
                ) values (
                    subjectUuid, 0, personUuid,
                    400000 + t, 400000 + t
                );
            end if;
            commit;
        end loop;
end; $$;
--//
