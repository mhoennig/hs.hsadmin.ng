--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single relation test record.
 */
create or replace procedure createHsOfficeRelationTestData(
        holderPersonName varchar,
        relationType HsOfficeRelationType,
        anchorPersonName varchar,
        contactCaption varchar,
        mark varchar default null)
    language plpgsql as $$
declare
    idName          varchar;
    anchorPerson    hs_office.person;
    holderPerson    hs_office.person;
    contact         hs_office.contact;

begin
    idName := base.cleanIdentifier( anchorPersonName || '-' || holderPersonName);

    select p.*
            into anchorPerson
            from hs_office.person p
            where p.tradeName = anchorPersonName or p.familyName = anchorPersonName;
    if anchorPerson is null then
        raise exception 'anchorPerson "%" not found', anchorPersonName;
    end if;

    select p.*
            into holderPerson
            from hs_office.person p
            where p.tradeName = holderPersonName or p.familyName = holderPersonName;
    if holderPerson is null then
        raise exception 'holderPerson "%" not found', holderPersonName;
    end if;

    select c.* into contact from hs_office.contact c where c.caption = contactCaption;
    if contact is null then
        raise exception 'contact "%" not found', contactCaption;
    end if;

    raise notice 'creating test relation: %', idName;
    raise notice '- using anchor person (%): %', anchorPerson.uuid, anchorPerson;
    raise notice '- using holder person (%): %', holderPerson.uuid, holderPerson;
    raise notice '- using contact (%): %', contact.uuid, contact;
    insert
        into hs_office.relation (uuid, anchoruuid, holderuuid, type, mark, contactUuid)
        values (uuid_generate_v4(), anchorPerson.uuid, holderPerson.uuid, relationType, mark, contact.uuid);
end; $$;
--//

/*
    Creates a range of test relation for mass data generation.
 */
create or replace procedure createHsOfficeRelationTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
declare
    person hs_office.person;
    contact hs_office.contact;
begin
    for t in startCount..endCount
        loop
            select p.* from hs_office.person p where tradeName = base.intToVarChar(t, 4) into person;
            select c.* from hs_office.contact c where c.caption = base.intToVarChar(t, 4) || '#' || t into contact;

            call createHsOfficeRelationTestData(person.uuid, contact.uuid, 'REPRESENTATIVE');
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating relation test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createHsOfficeRelationTestData('First GmbH', 'PARTNER', 'Hostsharing eG', 'first contact');
        call createHsOfficeRelationTestData('Firby', 'REPRESENTATIVE', 'First GmbH', 'first contact');
        call createHsOfficeRelationTestData('First GmbH', 'DEBITOR', 'First GmbH', 'first contact');

        call createHsOfficeRelationTestData('Second e.K.', 'PARTNER', 'Hostsharing eG', 'second contact');
        call createHsOfficeRelationTestData('Smith', 'REPRESENTATIVE', 'Second e.K.', 'second contact');
        call createHsOfficeRelationTestData('Second e.K.', 'DEBITOR', 'Second e.K.', 'second contact');

        call createHsOfficeRelationTestData('Third OHG', 'PARTNER', 'Hostsharing eG', 'third contact');
        call createHsOfficeRelationTestData('Tucker', 'REPRESENTATIVE', 'Third OHG', 'third contact');
        call createHsOfficeRelationTestData('Third OHG', 'DEBITOR', 'Third OHG', 'third contact');

        call createHsOfficeRelationTestData('Fourth eG', 'PARTNER', 'Hostsharing eG', 'fourth contact');
        call createHsOfficeRelationTestData('Fouler', 'REPRESENTATIVE', 'Third OHG', 'third contact');
        call createHsOfficeRelationTestData('Third OHG', 'DEBITOR', 'Third OHG', 'third contact');

        call createHsOfficeRelationTestData('Smith', 'PARTNER', 'Hostsharing eG', 'sixth contact');
        call createHsOfficeRelationTestData('Smith', 'DEBITOR', 'Smith', 'third contact');
        call createHsOfficeRelationTestData('Smith', 'SUBSCRIBER', 'Third OHG', 'third contact', 'members-announce');
    end;
$$;
--//
