--liquibase formatted sql

--changeset uuid-ossp-extension:1 endDelimiter:--//

/*
    Makes improved uuid generation available.
 */
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--//
