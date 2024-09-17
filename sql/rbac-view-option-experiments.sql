
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
        rbac.isPermissionGrantedToSubject(rbac.findEffectivePermissionId('rbactest.customer', id, 'SELECT'), rbac.currentSubjectUuid())
    );

SET SESSION AUTHORIZATION restricted;
SET hsadminng.currentSubject TO 'alex@example.com';
SELECT * from customer;

-- access control via view-rule and isPermissionGrantedToSubject - way too slow (35 s 580 ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
DROP VIEW cust_view;
CREATE VIEW cust_view AS
SELECT * FROM rbactest.customer;
CREATE OR REPLACE RULE "_RETURN" AS
    ON SELECT TO cust_view
    DO INSTEAD
    SELECT * FROM rbactest.customer WHERE rbac.isPermissionGrantedToSubject(rbac.findEffectivePermissionId('rbactest.customer', id, 'SELECT'), rbac.currentSubjectUuid());
SELECT * from cust_view LIMIT 10;

select rbac.queryAllPermissionsOfSubjectId(findRbacSubject('superuser-alex@hostsharing.net'));

-- access control via view-rule with join to recursive permissions - really fast (38ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE rbactest.customer ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS cust_view;
CREATE OR REPLACE VIEW cust_view AS
SELECT *
FROM rbactest.customer;
CREATE OR REPLACE RULE "_RETURN" AS
    ON SELECT TO cust_view
    DO INSTEAD
    SELECT c.uuid, c.reference, c.prefix FROM rbactest.customer AS c
      JOIN rbac.queryAllPermissionsOfSubjectId(rbac.currentSubjectUuid()) AS p
           ON p.objectTable='rbactest.customer' AND p.objectUuid=c.uuid;
GRANT ALL PRIVILEGES ON cust_view TO restricted;

SET SESSION SESSION AUTHORIZATION restricted;
SET hsadminng.currentSubject TO 'alex@example.com';
SELECT * from cust_view;


-- access control via view with join to recursive permissions - really fast (38ms for 1 million rows)
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS cust_view;
CREATE OR REPLACE VIEW cust_view AS
    SELECT c.uuid, c.reference, c.prefix
      FROM customer AS c
      JOIN queryAllPermissionsOfSubjectId(rbac.currentSubjectUuid()) AS p
           ON p.objectUuid=c.uuid;
GRANT ALL PRIVILEGES ON cust_view TO restricted;

SET SESSION SESSION AUTHORIZATION restricted;
-- SET hsadminng.currentSubject TO 'alex@example.com';
SET hsadminng.currentSubject TO 'superuser-alex@hostsharing.net';
-- SET hsadminng.currentSubject TO 'aaaaouq@example.com';
SELECT * from cust_view where reference=1144150;

select rr.uuid, rr.type from rbac.RbacGrants g
    join rbac.RbacReference RR on g.ascendantUuid = RR.uuid
    where g.descendantUuid in (
        select uuid from rbac.queryAllPermissionsOfSubjectId(findRbacSubject('alex@example.com'))
            where objectTable='rbactest.customer');

call rbac.grantRoleToUser(rbac.findRoleId('rbactest.customer#aaa:ADMIN'), rbac.findRbacSubject('aaaaouq@example.com'));

select rbac.queryAllPermissionsOfSubjectId(findRbacSubject('aaaaouq@example.com'));

