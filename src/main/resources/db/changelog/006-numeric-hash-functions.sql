--liquibase formatted sql


-- ============================================================================
-- NUMERIC-HASH-FUNCTIONS
--changeset hash:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create function bigIntHash(text) returns bigint as $$
select ('x'||substr(md5($1),1,16))::bit(64)::bigint;
$$ language sql;
--//
