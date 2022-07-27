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
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      from customer_rv c
     where c.prefix='aab';
    call expectBetween(resultCount, 1, 1);

    -- hostmaster listing all customers
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
       FROM customer_rv;
    call expectBetween(resultCount, 10, 20000);

    -- customer admin listing all their packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM package_rv;
    call expectBetween(resultCount, 2, 10);

    -- cutomer admin listing all their unix users
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM unixuser_rv;
    call expectBetween(resultCount, 20, 50);

    -- hostsharing admin assuming customer role and listing all accessible packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aaa.admin;customer#aab.admin';
    -- SELECT *
    SELECT count(*) INTO resultCount
      FROM package_rv p;
    call expectBetween(resultCount, 2, 10);

    -- hostsharing admin assuming two customer admin roles and listing all accessible unixusers
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';
    -- SELECT c.prefix, c.reference, uu.*
    SELECT count(*) INTO resultCount
      FROM unixuser_rv uu
      JOIN package_rv p ON p.uuid = uu.packageuuid
      JOIN customer_rv c ON c.uuid = p.customeruuid;
    call expectBetween(resultCount, 30, 50);

    -- hostsharing admin assuming two customer admin roles and listing all accessible domains
    -- ABORT; START TRANSACTION;
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aac.admin;customer#aad.admin';
    -- SELECT p.name, uu.name, dom.name
    SELECT count(*) INTO resultCount
       FROM domain_rv dom
       JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
       JOIN package_rv p ON p.uuid = uu.packageuuid
       JOIN customer_rv c ON c.uuid = p.customeruuid;
    call expectBetween(resultCount, 30, 50);

    -- hostsharing admin assuming two customer admin roles and listing all accessible email addresses
    -- ABORT; START TRANSACTION;
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aae.admin;customer#aaf.admin';
    SELECT c.prefix, p.name as "package", ema.localPart || '@' || dom.name as "email-address"
    -- SELECT count(*) INTO resultCount
      FROM emailaddress_rv ema
      JOIN domain_rv dom ON dom.uuid = ema.domainuuid
      JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
      JOIN package_rv p ON p.uuid = uu.packageuuid
      JOIN customer_rv c ON c.uuid = p.customeruuid;
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
3	     174 360	     150 000	   1.162	unixuser
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
3	     249 040	     150 000	   1.660	unixuser
4	     149 946	     100 000	   1.499	domain
5	     749 730	     500 000	   1.499	emailaddress


 */
