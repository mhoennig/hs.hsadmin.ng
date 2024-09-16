--liquibase formatted sql


-- ============================================================================
-- NUMERIC-HASH-FUNCTIONS
--changeset michael.hoennig:numeric-hash-functions endDelimiter:--//
-- ----------------------------------------------------------------------------

create function base.bigIntHash(text) returns bigint as $$
select ('x'||substr(md5($1),1,16))::bit(64)::bigint;
$$ language sql;
--//
