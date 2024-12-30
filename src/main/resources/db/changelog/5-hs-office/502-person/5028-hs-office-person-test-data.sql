--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-person-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single person test record.
 */
create or replace procedure hs_office.person_create_test_data(
        newPersonType hs_office.PersonType,
        newTradeName varchar,
        newFamilyName varchar = null,
        newGivenName varchar = null
)
    language plpgsql as $$
declare
    fullName    varchar;
    emailAddr   varchar;
begin
    fullName := concat_ws(', ', newTradeName, newFamilyName, newGivenName);
    emailAddr = 'person-' || left(base.cleanIdentifier(fullName), 32) || '@example.com';
    call base.defineContext('creating person test-data');
    perform rbac.create_subject(emailAddr);
    call base.defineContext('creating person test-data', null, emailAddr);

    raise notice 'creating test person: % by %', fullName, emailAddr;
    insert
        into hs_office.person (persontype, tradename, givenname, familyname)
        values (newPersonType, newTradeName, newGivenName, newFamilyName);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-person-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call hs_office.person_create_test_data('LP', 'Hostsharing eG');
        call hs_office.person_create_test_data('LP', 'First GmbH');
        call hs_office.person_create_test_data('NP', null, 'Firby', 'Susan');
        call hs_office.person_create_test_data('NP', null, 'Smith', 'Peter');
        call hs_office.person_create_test_data('NP', null, 'Tucker', 'Jack');
        call hs_office.person_create_test_data('NP', null, 'Fouler', 'Ellie');
        -- the next tradeName is deliberately 63 chars in length, the max length for that field, also to test long rbac-role names
        call hs_office.person_create_test_data('LP', 'Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', 'Smith', 'Peter');
        call hs_office.person_create_test_data('IF', 'Third OHG');
        call hs_office.person_create_test_data('LP', 'Fourth eG');
        call hs_office.person_create_test_data('UF', 'Erben Bessler', 'Mel', 'Bessler');
        call hs_office.person_create_test_data('NP', null, 'Bessler', 'Anita');
        call hs_office.person_create_test_data('NP', null, 'Bessler', 'Bert');
        call hs_office.person_create_test_data('NP', null, 'Winkler', 'Paul');
    end;
$$;
--//
