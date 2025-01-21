--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single contact test record.
 */
create or replace procedure hs_office.contact_create_test_data(contCaption varchar)
    language plpgsql as $$
declare
    emailAddr       varchar;
begin
    emailAddr = 'contact-admin@' || base.cleanIdentifier(contCaption) || '.example.com';
    call base.defineContext('creating contact test-data');
    perform rbac.create_subject(emailAddr);
    call base.defineContext('creating contact test-data', null, emailAddr);

    raise notice 'creating test contact: %', contCaption;
    insert
        into hs_office.contact (caption, postaladdress, emailaddresses, phonenumbers)
        values (
            contCaption,
            ( '{ ' ||
--                 '"name": "' || contCaption || '",' ||
--                 '"street": "Somewhere 1",' ||
--                 '"zipcode": "12345",' ||
--                 '"city": "Where-Ever",' ||
                '"country": "Germany"' ||
              '}')::jsonb,
            ('{ "main": "' || emailAddr || '" }')::jsonb,
            ('{ "phone_office": "+49 123 1234567" }')::jsonb
        );
end; $$;
--//

/*
    Creates a range of test contact for mass data generation.
 */
create or replace procedure hs_office.contact_create_test_data(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call hs_office.contact_create_test_data(base.intToVarChar(t, 4) || '#' || t);
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        -- TODO: use better names
        call hs_office.contact_create_test_data('first contact');
        call hs_office.contact_create_test_data('second contact');
        call hs_office.contact_create_test_data('third contact');
        call hs_office.contact_create_test_data('fourth contact');
        call hs_office.contact_create_test_data('fifth contact');
        call hs_office.contact_create_test_data('sixth contact');
        call hs_office.contact_create_test_data('seventh contact');
        call hs_office.contact_create_test_data('eighth contact');
        call hs_office.contact_create_test_data('ninth contact');
        call hs_office.contact_create_test_data('tenth contact');
        call hs_office.contact_create_test_data('eleventh contact');
        call hs_office.contact_create_test_data('twelfth contact');
    end;
$$;
--//
