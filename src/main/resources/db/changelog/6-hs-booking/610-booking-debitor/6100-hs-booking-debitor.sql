--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-booking-debitor-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

create view hs_booking.debitor_xv as
    select debitor.uuid,
           debitor.version,
           (partner.partnerNumber::varchar || debitor.debitorNumberSuffix)::numeric as debitorNumber,
           debitor.defaultPrefix
    from hs_office.debitor debitor
     -- RBAC for debitor is sufficient, for faster access we are bypassing RBAC for the join tables
    join hs_office.relation debitorRel on debitor.debitorReluUid=debitorRel.uuid
    join hs_office.relation partnerRel on partnerRel.holderUuid=debitorRel.anchorUuid
    join hs_office.partner partner on partner.partnerReluUid=partnerRel.uuid;
--//
