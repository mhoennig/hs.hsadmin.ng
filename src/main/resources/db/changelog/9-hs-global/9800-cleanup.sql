--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-global-office-test-ddl-cleanup context:hosting-asset-import endDelimiter:--//
-- ----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS hs_office.bankaccount_create_test_data(IN givenholder character varying, IN giveniban character varying, IN givenbic character varying);
DROP PROCEDURE IF EXISTS hs_office.contact_create_test_data(IN contcaption character varying);
DROP PROCEDURE IF EXISTS hs_office.contact_create_test_data(IN startcount integer, IN endcount integer);
DROP PROCEDURE IF EXISTS hs_office.coopassettx_create_test_data(IN givenpartnernumber numeric, IN givenmembernumbersuffix character);
DROP PROCEDURE IF EXISTS hs_office.coopsharetx_create_test_data(IN givenpartnernumber numeric, IN givenmembernumbersuffix character);
DROP PROCEDURE IF EXISTS hs_office.debitor_create_test_data(IN withdebitornumbersuffix numeric, IN forpartnerpersonname character varying, IN forbillingcontactcaption character varying, IN withdefaultprefix character varying);
DROP PROCEDURE IF EXISTS hs_office.membership_create_test_data(IN forpartnernumber numeric, IN newmembernumbersuffix character);
DROP PROCEDURE IF EXISTS hs_office.partner_create_test_data(IN mandanttradename character varying, IN newpartnernumber numeric, IN partnerpersonname character varying, IN contactcaption character varying);
DROP PROCEDURE IF EXISTS hs_office.person_create_test_data(IN newpersontype hs_office.persontype, IN newtradename character varying, IN newfamilyname character varying, IN newgivenname character varying);
DROP PROCEDURE IF EXISTS hs_office.relation_create_test_data(IN startcount integer, IN endcount integer);
DROP PROCEDURE IF EXISTS hs_office.relation_create_test_data(IN holderpersonname character varying, IN relationtype hs_office.relationtype, IN anchorpersonname character varying, IN contactcaption character varying, IN mark character varying);
DROP PROCEDURE IF EXISTS hs_office.sepamandate_create_test_data(IN forpartnernumber numeric, IN fordebitorsuffix character, IN foriban character varying, IN withreference character varying);
--//


-- ============================================================================
--changeset michael.hoennig:hs-global-rbac-test-ddl-cleanup context:hosting-asset-import endDelimiter:--//
-- ----------------------------------------------------------------------------

DROP SCHEMA IF EXISTS rbactest CASCADE;
--//


-- ============================================================================
--changeset michael.hoennig:hs-global-rbac-test-dml-cleanup context:hosting-asset-import endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.defineContext('9800-cleanup', null, '${HSADMINNG_SUPERUSER}', null);

DELETE FROM rbac.subject WHERE name='superuser-alex@hostsharing.net';
DELETE FROM rbac.subject WHERE name='superuser-fran@hostsharing.net';
--//
