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
SELECT * FROM intToVarChar(211, 4);
