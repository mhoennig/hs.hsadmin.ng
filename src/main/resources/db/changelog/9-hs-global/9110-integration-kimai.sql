--liquibase formatted sql

-- ============================================================================
--changeset timotheus.pokorra:hs-global-integration-kimai endDelimiter:--//
-- TODO.impl: also select column debitorNumber and do not filter anymore for '00'
CREATE OR REPLACE VIEW hs_integration.time_customer AS
 SELECT p.partnernumber, debitor.defaultprefix
    FROM hs_office.partner p
    JOIN hs_office.relation AS pRel
        ON pRel.type = 'PARTNER'
        AND pRel.uuid = p.partnerRelUuid
    JOIN hs_office.relation AS dRel
        ON dRel.type = 'DEBITOR'
        AND dRel.anchorUuid = pRel.holderUuid
    JOIN hs_office.debitor AS debitor
        ON debitor.debitorreluuid = dRel.uuid
        AND debitor.debitornumbersuffix = '00';
--//