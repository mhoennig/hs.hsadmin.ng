
-- ========================================================
-- Options for SELECT under RBAC rules
-- --------------------------------------------------------

-- access control via view policy and isPermissionGrantedToSubject - way too slow (33 s 617ms for 1 million rows)
SET SESSION AUTHORIZATION DEFAULT;
CREATE ROLE admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
CREATE ROLE restricted;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO restricted;

SET SESSION AUTHORIZATION DEFAULT;
ALTER TABLE customer DISABLE ROW LEVEL SECURITY;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS customer_policy ON customer;
CREATE POLICY customer_policy ON customer
    FOR SELECT
    TO restricted
    USING (
        -- id=1000
        isPermissionGrantedToSubject(findEffectivePermissionId('test_customer', id, 'SELECT'), currentUserUuid())
    );

SET SESSION AUTHORIZATION restricted;
SET hsadminng.currentUser TO 'alex@example.com';
SELECT * from customer;

-- access control via view-rule and isPermissionGrantedToSubject - way too slow (35 s 580 ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
DROP VIEW cust_view;
CREATE VIEW cust_view AS
SELECT * FROM customer;
CREATE OR REPLACE RULE "_RETURN" AS
    ON SELECT TO cust_view
    DO INSTEAD
    SELECT * FROM customer WHERE isPermissionGrantedToSubject(findEffectivePermissionId('test_customer', id, 'SELECT'), currentUserUuid());
SELECT * from cust_view LIMIT 10;

select queryAllPermissionsOfSubjectId(findRbacUser('superuser-alex@hostsharing.net'));

-- access control via view-rule with join to recursive permissions - really fast (38ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS cust_view;
CREATE OR REPLACE VIEW cust_view AS
SELECT *
FROM customer;
CREATE OR REPLACE RULE "_RETURN" AS
    ON SELECT TO cust_view
    DO INSTEAD
    SELECT c.uuid, c.reference, c.prefix FROM customer AS c
      JOIN queryAllPermissionsOfSubjectId(currentUserUuid()) AS p
           ON p.objectTable='test_customer' AND p.objectUuid=c.uuid;
GRANT ALL PRIVILEGES ON cust_view TO restricted;

SET SESSION SESSION AUTHORIZATION restricted;
SET hsadminng.currentUser TO 'alex@example.com';
SELECT * from cust_view;


-- access control via view with join to recursive permissions - really fast (38ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS cust_view;
CREATE OR REPLACE VIEW cust_view AS
    SELECT c.uuid, c.reference, c.prefix
      FROM customer AS c
      JOIN queryAllPermissionsOfSubjectId(currentUserUuid()) AS p
           ON p.objectUuid=c.uuid;
GRANT ALL PRIVILEGES ON cust_view TO restricted;

SET SESSION SESSION AUTHORIZATION restricted;
-- SET hsadminng.currentUser TO 'alex@example.com';
SET hsadminng.currentUser TO 'superuser-alex@hostsharing.net';
-- SET hsadminng.currentUser TO 'aaaaouq@example.com';
SELECT * from cust_view where reference=1144150;

select rr.uuid, rr.type from RbacGrants g
    join RbacReference RR on g.ascendantUuid = RR.uuid
    where g.descendantUuid in (
        select uuid from queryAllPermissionsOfSubjectId(findRbacUser('alex@example.com'))
            where objectTable='test_customer');

call grantRoleToUser(findRoleId('test_customer#aaa.admin'), findRbacUser('aaaaouq@example.com'));

select queryAllPermissionsOfSubjectId(findRbacUser('aaaaouq@example.com'));

