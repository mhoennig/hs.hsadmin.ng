CREATE FUNCTION historicize() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
    IF (TG_OP = 'INSERT') OR (TG_OP = 'UPDATE') THEN
        EXECUTE format('INSERT INTO ' || TG_TABLE_NAME || '_history VALUES (DEFAULT, now(), txid_current(), False, $1.*)') USING NEW;
        RETURN NEW;
    ELSE
        EXECUTE format('INSERT INTO ' || TG_TABLE_NAME || '_history VALUES (DEFAULT, now(), txid_current(), True, $1.*)') USING OLD;
        RETURN OLD;
    END IF;
END;
$$;

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

SELECT * FROM person_history WHERE history_id IN (SELECT max(history_id) AS history_id FROM person_history WHERE history_transaction <= 12345 GROUP BY id);
SELECT * FROM person_history WHERE history_id IN (SELECT max(history_id) AS history_id FROM person_history WHERE history_transaction <= 12345 GROUP BY name);
