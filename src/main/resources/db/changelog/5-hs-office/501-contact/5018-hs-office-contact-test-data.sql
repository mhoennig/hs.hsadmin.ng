--liquibase formatted sql


-- ============================================================================
--changeset hs-office-contact-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single contact test record.
 */
create or replace procedure createHsOfficeContactTestData(contCaption varchar)
    language plpgsql as $$
declare
    postalAddr      varchar;
    emailAddr       varchar;
begin
    emailAddr = 'contact-admin@' || cleanIdentifier(contCaption) || '.example.com';
    call defineContext('creating contact test-data');
    perform createRbacUser(emailAddr);
    call defineContext('creating contact test-data', null, emailAddr);

    postalAddr := E'Vorname Nachname\nStraße Hnr\nPLZ Stadt';

    raise notice 'creating test contact: %', contCaption;
    insert
        into hs_office_contact (caption, postaladdress, emailaddresses, phonenumbers)
        values (
            contCaption,
            postalAddr,
            ('{ "main": "' || emailAddr || '" }')::jsonb,
            ('{ "phone_office": "+49 123 1234567" }')::jsonb
        );
end; $$;
--//

/*
    Creates a range of test contact for mass data generation.
 */
create or replace procedure createHsOfficeContactTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call createHsOfficeContactTestData(intToVarChar(t, 4) || '#' || t);
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-contact-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        -- TODO: use better names
        call createHsOfficeContactTestData('first contact');
        call createHsOfficeContactTestData('second contact');
        call createHsOfficeContactTestData('third contact');
        call createHsOfficeContactTestData('fourth contact');
        call createHsOfficeContactTestData('fifth contact');
        call createHsOfficeContactTestData('sixth contact');
        call createHsOfficeContactTestData('seventh contact');
        call createHsOfficeContactTestData('eighth contact');
        call createHsOfficeContactTestData('ninth contact');
        call createHsOfficeContactTestData('tenth contact');
        call createHsOfficeContactTestData('eleventh contact');
        call createHsOfficeContactTestData('twelfth contact');
    end;
$$;
--//
