--liquibase formatted sql

--changeset uuid-ossp-extension:1 endDelimiter:--//

/*
    Makes improved uuid generation available.
 */
create extension if not exists "uuid-ossp";
--//
