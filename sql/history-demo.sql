CREATE FUNCTION historicize() RETURNS trigger
AS $$
BEGIN
    IF (TG_OP = 'INSERT') OR (TG_OP = 'UPDATE') THEN
        EXECUTE format('INSERT INTO %I_history VALUES (DEFAULT, now(), txid_current(), False, $1.*)', TG_TABLE_NAME) USING NEW;
        RETURN NEW;
    ELSE
        EXECUTE format('INSERT INTO %I_history VALUES (DEFAULT, now(), txid_current(), True, $1.*)', TG_TABLE_NAME) USING OLD;
        RETURN OLD;
    END IF;
END;
$$
LANGUAGE plpgsql;

CREATE TABLE person (
    id serial PRIMARY KEY,
    name character varying(50) NOT NULL UNIQUE,
    email character varying(50) NOT NULL UNIQUE
);

CREATE TABLE person_history (
    history_id serial PRIMARY KEY,
    history_timestamp timestamp NOT NULL,
    history_transaction bigint NOT NULL,
    history_tombstone boolean NOT NULL,
    id integer NOT NULL,
    name character varying(50) NOT NULL,
    email character varying(50) NOT NULL
);

CREATE TRIGGER person_historicize AFTER INSERT OR DELETE OR UPDATE ON person FOR EACH ROW EXECUTE PROCEDURE historicize();

CREATE OR REPLACE FUNCTION person_history(transaction bigint, VARIADIC groupby text[]) RETURNS TABLE (
    history_id integer,
    history_timestamp timestamp,
    history_transaction bigint,
    history_tombstone boolean,
    id integer,
    name character varying(50),
    email character varying(50)
)
AS $$
BEGIN
    RETURN QUERY EXECUTE format('SELECT * FROM person_history WHERE history_id IN (SELECT max(history_id) AS history_id FROM person_history WHERE history_transaction <= $1 GROUP BY %s)', array_to_string(groupby, ', ')) USING transaction;
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_history(VARIADIC groupby text[]) RETURNS TABLE (
    history_id integer,
    history_timestamp timestamp,
    history_transaction bigint,
    history_tombstone boolean,
    id integer,
    name character varying(50),
    email character varying(50)
)
AS $$
BEGIN
    RETURN QUERY EXECUTE format('SELECT * FROM person_history WHERE history_id IN (SELECT max(history_id) AS history_id FROM person_history GROUP BY %s)', array_to_string(groupby, ', '));
END;
$$
LANGUAGE plpgsql;

INSERT INTO person (name, email) VALUES ('michael', 'michael@hierweck.de');
INSERT INTO person (name, email) VALUES ('annika', 'annika@hierweck.de');

UPDATE person SET email='mh@hierweck.de' WHERE name='michael';
UPDATE person SET email='ah@hierweck.de' WHERE name='annika';

DELETE FROM person WHERE name='michael';
DELETE FROM person WHERE name='annika';

INSERT INTO person (name, email) VALUES ('michael', 'michael@hierweck.de');
INSERT INTO person (name, email) VALUES ('annika', 'annika@hierweck.de');

BEGIN;
INSERT INTO person (name, email) VALUES ('mx', 'mx@hierweck.de');
INSERT INTO person (name, email) VALUES ('ax', 'ax@hierweck.de');
UPDATE person SET email='mxx@hierweck.de' WHERE name='mx';
UPDATE person SET email='axx@hierweck.de' WHERE name='ax';
COMMIT;
