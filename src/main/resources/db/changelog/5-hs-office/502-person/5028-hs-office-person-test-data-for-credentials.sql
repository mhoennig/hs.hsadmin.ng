--liquibase formatted sql

-- In a separate file to avoid changed checksums in the existing changsets.
-- I presume it's a bug in Liquibase that other changeset checksums are changed by new changesets in the same file

-- ============================================================================
--changeset michael.hoennig:hs-office-person-TEST-DATA-GENERATION-FOR-CREDENTIALS context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call hs_office.person_create_test_data('NP', null,'Hostmaster', 'Alex');
        call hs_office.person_create_test_data('NP', null, 'Hostmaster', 'Fran');
        call hs_office.person_create_test_data('NP', null, 'User', 'Drew');
        call hs_office.person_create_test_data('NP', null, 'User', 'Test');
    end;
$$;
--//

