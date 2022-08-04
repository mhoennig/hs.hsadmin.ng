-- ========================================================
-- First Example Entity with History
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS customer (
    "id" SERIAL PRIMARY KEY,
    "reference" int not null unique, -- 10000-99999
    "prefix" character(3) unique
    );

CALL create_historicization('customer');


-- ========================================================
-- Second Example Entity with History
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS package_type (
    "id" serial PRIMARY KEY,
    "name" character varying(8)
    );

CALL create_historicization('package_type');

-- ========================================================
-- Third Example Entity with History
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS package (
    "id" serial PRIMARY KEY,
    "name" character varying(5),
    "customer_id" INTEGER REFERENCES customer(id)
    );

CALL create_historicization('package');


-- ========================================================
-- query historical data
-- --------------------------------------------------------


ABORT;
BEGIN TRANSACTION;
SET LOCAL hsadminng.currentUser TO 'mih42_customer_aaa';
SET LOCAL hsadminng.currentTask TO 'adding customer_aaa';
INSERT INTO package (customer_id, name) VALUES (10000, 'aaa00');
COMMIT;
-- Usage:

SET hsadminng.timestamp TO '2022-07-12 08:53:27.723315';
SET hsadminng.timestamp TO '2022-07-12 11:38:27.723315';
SELECT * FROM customer_hv p WHERE prefix = 'aaa';
