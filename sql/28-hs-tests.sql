ABORT;
SET SESSION SESSION AUTHORIZATION DEFAULT;

-- there are some random ractors in test data generation, thus a range has to be accepted
CREATE OR REPLACE PROCEDURE expectBetween(actualCount integer, expectedFrom integer, expectedTo integer)
    LANGUAGE plpgsql AS $$
BEGIN
    IF NOT actualCount BETWEEN expectedFrom AND expectedTo THEN
        RAISE EXCEPTION 'count expected to be between % and %, but got %', expectedFrom, expectedTo, actualCount;
END IF;
END; $$;

DO LANGUAGE plpgsql $$
DECLARE
    resultCount integer;
BEGIN

    -- hostmaster accessing a single customer
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      from test_customer_rv c
     where c.prefix='aab';
    call expectBetween(resultCount, 1, 1);

    -- hostmaster listing all customers
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
       FROM test_customer_rv;
    call expectBetween(resultCount, 10, 20000);

    -- customer admin listing all their packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM test_package_rv;
    call expectBetween(resultCount, 2, 10);

    -- cutomer admin listing all their unix users
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM domain_rv;
    call expectBetween(resultCount, 20, 50);

    -- hostsharing admin assuming customer role and listing all accessible packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = 'test_customer#aaa.admin;test_customer#aab.admin';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM test_package_rv p;
    call expectBetween(resultCount, 2, 10);

    -- hostsharing admin assuming two customer admin roles and listing all accessible domains
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = 'test_customer#aab.admin;test_customer#aac.admin';
    -- SELECT c.prefix, c.reference, uu.*
    SELECT count(*) INTO resultCount
      FROM domain_rv uu
      JOIN test_package_rv p ON p.uuid = uu.packageuuid
      JOIN test_customer_rv c ON c.uuid = p.customeruuid;
    call expectBetween(resultCount, 40, 60);

    -- hostsharing admin assuming two customer admin roles and listing all accessible domains
    -- ABORT; START TRANSACTION;
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = 'test_customer#aac.admin;test_customer#aad.admin';
    -- SELECT p.name, uu.name, dom.name
    SELECT count(*) INTO resultCount
       FROM domain_rv dom
       JOIN domain_rv uu ON uu.uuid = dom.domainuuid
       JOIN test_package_rv p ON p.uuid = uu.packageuuid
       JOIN test_customer_rv c ON c.uuid = p.customeruuid;
    call expectBetween(resultCount, 20, 40);

    -- hostsharing admin assuming two customer admin roles and listing all accessible email addresses
    -- ABORT; START TRANSACTION;
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@example.org';
    SET LOCAL hsadminng.assumedRoles = 'test_customer#aae.admin;test_customer#aaf.admin';
    -- SELECT c.prefix, p.name as "package", ema.localPart || '@' || dom.name as "email-address"
    SELECT count(*) INTO resultCount
      FROM emailaddress_rv ema
      JOIN domain_rv dom ON dom.uuid = ema.domainuuid
      JOIN domain_rv uu ON uu.uuid = dom.domainuuid
      JOIN test_package_rv p ON p.uuid = uu.packageuuid
      JOIN test_customer_rv c ON c.uuid = p.customeruuid;
    call expectBetween(resultCount, 100, 300);

    -- ~170ms
END; $$;

/*
=== with 7000 customers ===

1. 7105 vs 801 ms
2. 960 vs. 649 ms
3. 970 vs. 670 ms

no	       count	    required	  factor	table
1	       7 000	       7 000	   1.000	customers
2	      17 436	      15 000	   1.162	packages
3	     174 360	     150 000	   1.162	domain
4	     105 206	     100 000	   1.052	domain
5	     526 030	     500 000	   1.052	emailaddress

=== with 10000 customers (+43%) ===

1. 7491 vs. 1189 ms (-1%)
2. 1049 ms (+31%)
3. 1028 ms (+53%)
in average +9,33%

no	count	required	factor	table
1	      10 000	       7 000	   1.429	customers
2	      24 904	      15 000	   1.660	packages
3	     249 040	     150 000	   1.660	domain
4	     149 946	     100 000	   1.499	domain
5	     749 730	     500 000	   1.499	emailaddress


 */
