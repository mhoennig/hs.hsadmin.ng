
DO LANGUAGE plpgsql $$
BEGIN

    -- hostmaster accessing a single customer
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    PERFORM * from customer_rv c where c.prefix='aab';

    -- hostmaster listing all customers
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = '';
    PERFORM * FROM customer_rv;

    -- customer admin listing all their packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    PERFORM * FROM package_rv;

    -- cutomer admin listing all their unix users
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
    SET LOCAL hsadminng.assumedRoles = '';
    PERFORM * FROM unixuser_rv;

    -- hostsharing admin assuming customer role and listing all accessible packages
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aaa.admin;customer#aab.admin';
    PERFORM * FROM package_rv p;

    -- hostsharing admin assuming two customer admin roles and listing all accessible unixusers
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';
    PERFORM c.prefix, c.reference, uu.*
      FROM unixuser_rv uu
      JOIN package_rv p ON p.uuid = uu.packageuuid
      JOIN customer_rv c ON c.uuid = p.customeruuid;

    -- hostsharing admin assuming two customer admin roles and listing all accessible domains
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aac.admin;customer#aad.admin';
    PERFORM p.name, uu.name, dom.name
       FROM domain_rv dom
       JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
       JOIN package_rv p ON p.uuid = uu.packageuuid
       JOIN customer_rv c ON c.uuid = p.customeruuid;

    -- hostsharing admin assuming two customer admin roles and listing all accessible email addresses
    SET SESSION SESSION AUTHORIZATION restricted;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#aad.admin;customer#aae.admin';
    PERFORM c.prefix, p.name as "package", ema.localPart || '@' || dom.name as "email-address"
      FROM emailaddress_rv ema
      JOIN domain_rv dom ON dom.uuid = ema.domainuuid
      JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
      JOIN package_rv p ON p.uuid = uu.packageuuid
      JOIN customer_rv c ON c.uuid = p.customeruuid;
END; $$;

/*
=== with 7000 customers ===

1. [2022-07-26 09:17:19] completed in 801 ms
2. [2022-07-26 09:17:32] completed in 649 ms
3. [2022-07-26 09:17:51] completed in 670 ms

no	       count	    required	  factor	table
1	       7 000	       7 000	   1.000	customers
2	      17 436	      15 000	   1.162	packages
3	     174 360	     150 000	   1.162	unixuser
4	     105 206	     100 000	   1.052	domain
5	     526 030	     500 000	   1.052	emailaddress


 */
