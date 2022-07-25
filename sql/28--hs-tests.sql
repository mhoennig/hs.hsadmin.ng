

-- hostmaster listing all customers
ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
SET LOCAL hsadminng.assumedRoles = '';
SELECT * FROM customer_rv;
END TRANSACTION;

-- customer admin listing all their packages
ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
SET LOCAL hsadminng.assumedRoles = '';
SELECT * FROM package_rv;
END TRANSACTION;


-- cutomer admin listing all their unix users
ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'admin@aae.example.com';
SET LOCAL hsadminng.assumedRoles = '';

SELECT * FROM unixuser_rv;
END TRANSACTION;


-- hostsharing admin assuming customer role and listing all accessible packages
ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';
SELECT * FROM package_rv p;
END TRANSACTION;

---

-- hostsharing admin assuming two customer admin role and listing all accessible unixusers
ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';

SELECT c.prefix, c.reference, uu.*
FROM unixuser_rv uu
         JOIN package_rv p ON p.uuid = uu.packageuuid
         JOIN customer_rv c ON c.uuid = p.customeruuid;
END TRANSACTION;

---

BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';

SELECT p.name, uu.name, dom.name
FROM domain_rv dom
         JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
         JOIN package_rv p ON p.uuid = uu.packageuuid
         JOIN customer_rv c ON c.uuid = p.customeruuid;
END TRANSACTION;

---

BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
SET LOCAL hsadminng.assumedRoles = 'customer#aab.admin;customer#aac.admin';

SELECT c.prefix, p.name as "package", ema.localPart || '@' || dom.name as "email-address"
  FROM emailaddress_rv ema
  JOIN domain_rv dom ON dom.uuid = ema.domainuuid
  JOIN unixuser_rv uu ON uu.uuid = dom.unixuseruuid
  JOIN package_rv p ON p.uuid = uu.packageuuid
  JOIN customer_rv c ON c.uuid = p.customeruuid;
END TRANSACTION;

---

ROLLBACK;
BEGIN TRANSACTION;
SET SESSION SESSION AUTHORIZATION restricted;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
select * from customer_rv c where c.prefix='aab';
END TRANSACTION;
