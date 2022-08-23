--liquibase formatted sql


-- ============================================================================
--changeset JSONB-CHANGES-DELTA:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Recursively compares two jsonb values and returns what has changed.
    This is a kind of right sided json diff.
 */

create or replace function jsonb_changes_delta(oldJson jsonb, newJson jsonb)
    returns jsonb
    called on null input
    language plpgsql as $$
declare
    diffJson       jsonb;
    oldJsonElement record;
begin
    raise notice '>>> diffing: % % vs. % %', jsonb_typeof(oldJson), oldJson, jsonb_typeof(newJson), newJson;

    if oldJson is null or jsonb_typeof(oldJson) = 'null' or
        newJson is null or jsonb_typeof(newJson) = 'null' then
        return newJson;
    end if;

    diffJson = newJson;
    for oldJsonElement in select * from jsonb_each(oldJson)
        loop
            raise notice 'intermediate result: %', diffJson;
            raise notice 'record: %', oldJsonElement;
            if diffJson @> jsonb_build_object(oldJsonElement.key, oldJsonElement.value) then
                raise notice 'ignoring equal: %', oldJsonElement.key;
                diffJson = diffJson - oldJsonElement.key;
            elsif diffJson ? oldJsonElement.key then
                if jsonb_typeof(newJson -> (oldJsonElement.key)) = 'object' then
                    raise notice 'diffing new: % -> %', oldJsonElement.key, newJson -> (oldJsonElement.key);
                    diffJson = diffJson ||
                               jsonb_build_object(oldJsonElement.key,
                                                  jsonb_changes_delta(oldJsonElement.value, newJson -> (oldJsonElement.key)));
                else
                    raise notice 'not an object: %, leaving %', oldJsonElement.key, newJson -> (oldJsonElement.key);
                end if;
            else
                raise notice 'nulling old: %', oldJsonElement.key;
                diffJson = diffJson || jsonb_build_object(oldJsonElement.key, null);
            end if;
        end loop;
    raise notice '<<< result: %', diffJson;
    return diffJson;
end; $$;

/*
    Tests jsonb_diff.
 */
do language plpgsql $$
    declare
        expected text;
        actual   text;
    begin

        select jsonb_changes_delta(null::jsonb, null::jsonb) into actual;
        if actual is not null then
            raise exception 'jsonb_diff #1 failed:%    expected: %,%    actually:   %', E'\n', expected, E'\n', actual;
        end if;

        select jsonb_changes_delta(null::jsonb, '{"a":  "new"}'::jsonb) into actual;
        expected := '{"a":  "new"}'::jsonb;
        if actual <> expected then
            raise exception 'jsonb_diff #2 failed:%    expected:   %,%    actual:  %', E'\n', expected, E'\n', actual;
        end if;

        select jsonb_changes_delta('{"a":  "old"}'::jsonb, '{"a":  "new"}'::jsonb) into actual;
        expected := '{"a":  "new"}'::jsonb;
        if actual <> expected then
            raise exception 'jsonb_diff #3 failed:%    expected:   %,%    actual:  %', E'\n', expected, E'\n', actual;
        end if;

        select jsonb_changes_delta('{"a":  "old"}'::jsonb, '{"a":  "old"}'::jsonb) into actual;
        expected := '{}'::jsonb;
        if actual <> expected then
            raise exception 'jsonb_diff #4 failed:%    expected:   %,%    actual:  %', E'\n', expected, E'\n', actual;
        end if;

        select jsonb_changes_delta(
                       $json${
                         "a": "same",
                         "b": "old",
                         "c": "set",
                         "d": "set",
                         "e": null,
                         "i": {
                           "x": "equal",
                           "y": "old",
                           "z": "old"
                         },
                         "j": {
                           "k": "set"
                         },
                         "l": {
                           "m": "equal",
                           "n": null
                         }
                       }$json$::jsonb,
                       $json${
                         "a": "same",
                         "b": "new",
                         "c": null,
                         "e": {
                           "f": "set"
                         },
                         "g": "set",
                         "i": {
                           "x": "equal",
                           "y": "new",
                           "z": null
                         },
                         "j": null,
                         "l": {
                           "m": "equal",
                           "n": null
                         }
                       }$json$::jsonb
                   )
            into actual;
        expected :=
                $json${"b": "new", "c": null, "d": null, "e": {"f": "set"}, "g": "set", "i": {"y": "new", "z": null}, "j": null}$json$;
        if actual <> expected then
            raise exception 'jsonb_diff #5 failed:%    expected: %,%    actual:   %', E'\n', expected, E'\n', actual;
        end if;
    end; $$;
--//

