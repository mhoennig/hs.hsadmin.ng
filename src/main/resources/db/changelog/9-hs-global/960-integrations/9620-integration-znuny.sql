
--liquibase formatted sql

-- ============================================================================
--changeset timotheus.pokorra:hs-global-integration-znuny endDelimiter:--//
-- TODO.impl: also select column debitorNumber and do not filter anymore for '00'
CREATE OR REPLACE VIEW hs_integration.contact AS
  SELECT DISTINCT ON (contact_uuid)
    partner.partnernumber as partnernumber,
    debitor.defaultprefix as defaultprefix,
    partner.uuid as partner_uuid,
    c.uuid as contact_uuid,
    (CASE WHEN per.salutation <> '' THEN per.salutation ELSE NULL END) as salutation,
    (CASE WHEN per.givenname <> '' THEN per.givenname ELSE NULL END) as givenname,
    (CASE WHEN per.familyname <> '' THEN per.familyname ELSE NULL END) as familyname,
    (CASE WHEN per.title <> '' THEN per.title ELSE NULL END) as title,
    (CASE WHEN per.tradename <> '' THEN per.tradename ELSE NULL END) as tradename,
    (CASE WHEN c.postaladdress->>'co' <> '' THEN c.postaladdress->>'co' ELSE NULL END) as co,
    c.postaladdress->>'street' as street,
    c.postaladdress->>'zipcode' as zipcode,
    c.postaladdress->>'city' as city,
    c.postaladdress->>'country' as country,
    c.phonenumbers->>'phone_private' as phone_private,
    c.phonenumbers->>'phone_office' as phone_office,
    c.phonenumbers->>'phone_mobile' as phone_mobile,
    c.phonenumbers->>'fax' as fax,
    c.emailaddresses->>'main' as email
  FROM hs_office.partner AS partner
  JOIN hs_office.partner_legacy_id AS partner_lid ON partner_lid.uuid = partner.uuid
  JOIN hs_office.relation AS pRel
        ON pRel.type = 'PARTNER'
        AND pRel.uuid = partner.partnerRelUuid
  JOIN hs_office.relation AS dRel
        ON dRel.type = 'DEBITOR'
        AND dRel.anchorUuid = pRel.holderUuid
  JOIN hs_office.debitor AS debitor
        ON debitor.debitorreluuid = dRel.uuid
        AND debitor.debitornumbersuffix = '00'
  JOIN hs_office.contact AS c ON c.uuid = pRel.contactuuid
  JOIN hs_office.person AS per ON per.uuid = pRel.holderuuid
  UNION
  SELECT DISTINCT ON (contact_uuid)
    partner.partnernumber as partnernumber,
    debitor.defaultprefix as defaultprefix,
    partner.uuid as partner_uuid,
    c.uuid as contact_uuid,
    (CASE WHEN per.salutation <> '' THEN per.salutation ELSE NULL END) as salutation,
    (CASE WHEN per.givenname <> '' THEN per.givenname ELSE NULL END) as givenname,
    (CASE WHEN per.familyname <> '' THEN per.familyname ELSE NULL END) as familyname,
    (CASE WHEN per.title <> '' THEN per.title ELSE NULL END) as title,
    (CASE WHEN per.tradename <> '' THEN per.tradename ELSE NULL END) as tradename,
    (CASE WHEN c.postaladdress->>'co' <> '' THEN c.postaladdress->>'co' ELSE NULL END) as co,
    c.postaladdress->>'street' as street,
    c.postaladdress->>'zipcode' as zipcode,
    c.postaladdress->>'city' as city,
    c.postaladdress->>'country' as country,
    c.phonenumbers->>'phone_private' as phone_private,
    c.phonenumbers->>'phone_office' as phone_office,
    c.phonenumbers->>'phone_mobile' as phone_mobile,
    c.phonenumbers->>'fax' as fax,
    c.emailaddresses->>'main' as email
  FROM hs_office.partner AS partner
  JOIN hs_office.relation AS pRel
        ON pRel.type = 'PARTNER'
        AND pRel.uuid = partner.partnerRelUuid
  JOIN hs_office.relation AS dRel
        ON dRel.type = 'DEBITOR'
        AND dRel.anchorUuid = pRel.holderUuid
  JOIN hs_office.debitor AS debitor
        ON debitor.debitorreluuid = dRel.uuid
        AND debitor.debitornumbersuffix = '00'
  JOIN hs_office.relation AS rs1 ON rs1.uuid = partner.partnerreluuid AND rs1.type = 'PARTNER'
  JOIN hs_office.relation AS relation ON relation.anchoruuid = rs1.holderuuid
  JOIN hs_office.contact AS c ON c.uuid = relation.contactuuid
  JOIN hs_office.person AS per ON per.uuid = relation.holderuuid;

CREATE OR REPLACE VIEW hs_integration.ticket_customer_user AS
  SELECT c.contact_uuid,
    max(c.partnernumber)::text as number,
    max(c.defaultprefix) as code,
    max(c.email) as login,
    max(c.salutation) as salut,
    max(c.givenname) as firstname,
    max(c.familyname) as lastname,
    max(c.title) as title,
    max(c.tradename) as firma,
    max(c.co) as co,
    max(c.street) as street,
    max(c.zipcode) as zipcode,
    max(c.city) as city,
    max(c.country) as country,
    max(concat_ws(', '::text, c.phone_office, c.phone_private)) AS phone,
    max(c.phone_private) as phone_private,
    max(c.phone_office) as phone_office,
    max(c.phone_mobile) as mobile,
    max(c.fax) as fax,
    max(c.email) as email,
    string_agg(CASE WHEN relation.mark IS NULL THEN relation.type::text ELSE CONCAT(relation.type::text, ':', relation.mark::text) END, '/'::text) AS comment,
    1 AS valid
  FROM hs_integration.contact AS c
  JOIN hs_office.relation AS relation ON c.contact_uuid = relation.contactuuid
  WHERE (c.defaultprefix != 'hsh' OR (c.partnernumber = 10000 AND c.email = 'hostmaster@hostsharing.net'))
  GROUP BY c.contact_uuid;

CREATE OR REPLACE VIEW hs_integration.ticket_customer_company AS
  SELECT
    partner.partnernumber::text as number,
    debitor.defaultprefix as code,
    concat_ws('/'::text, to_char(lower(membership.validity), 'YYYY-MM-DD'::text), to_char(upper(membership.validity) - INTERVAL '1 days', 'YYYY-MM-DD'::text)) AS comment,
    1 AS valid
  FROM hs_office.partner AS partner
  JOIN hs_office.relation AS pRel
        ON pRel.type = 'PARTNER'
        AND pRel.uuid = partner.partnerRelUuid
  JOIN hs_office.relation AS dRel
        ON dRel.type = 'DEBITOR'
        AND dRel.anchorUuid = pRel.holderUuid
  JOIN hs_office.debitor AS debitor
        ON debitor.debitorreluuid = dRel.uuid
        AND debitor.debitornumbersuffix = '00'
  LEFT OUTER JOIN hs_office.membership AS membership ON membership.partneruuid = partner.uuid
  ORDER BY number;

--//