abort;
set local session authorization default;


CREATE OR REPLACE FUNCTION array_distinct(anyarray) RETURNS anyarray AS $f$
SELECT array_agg(DISTINCT x) FROM unnest($1) t(x);
$f$ LANGUAGE SQL IMMUTABLE;


CREATE OR REPLACE FUNCTION lastRowCount()
    RETURNS bigint
    LANGUAGE plpgsql AS $$
DECLARE
    lastRowCount bigint;
BEGIN
    GET DIAGNOSTICS lastRowCount = ROW_COUNT;
    RETURN lastRowCount;
END;
$$;

-- ========================================================
-- Test Data helpers
-- --------------------------------------------------------

CREATE OR REPLACE FUNCTION intToVarChar(i integer, len integer)
    RETURNS varchar
    LANGUAGE plpgsql AS $$
DECLARE
partial varchar;
BEGIN
SELECT chr(ascii('a') + i%26) INTO partial;
IF len > 1 THEN
        RETURN intToVarChar(i/26, len-1) || partial;
ELSE
        RETURN partial;
END IF;
END; $$;

select * from intToVarChar(211, 4);

CREATE OR REPLACE FUNCTION randomInRange(min INTEGER, max INTEGER)
    RETURNS INT
    RETURNS NULL ON NULL INPUT
    language 'plpgsql' AS $$
BEGIN
    RETURN floor(random() * (max-min + 1) + min);
END; $$;

select * from randomInRange(0, 4);
