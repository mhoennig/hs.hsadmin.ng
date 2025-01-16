
--liquibase formatted sql

-- ============================================================================
--changeset timotheus.pokorra:hs-global-integration-znuny endDelimiter:--//
CREATE OR REPLACE VIEW hs_integration.subscription AS
  SELECT DISTINCT
    relation.mark as subscription,
    contact.emailaddresses->>'main' as email
  FROM hs_office.contact AS contact
    JOIN hs_office.relation AS relation ON relation.contactuuid = contact.uuid AND relation.type = 'SUBSCRIBER'
  ORDER BY subscription, email;

--//
