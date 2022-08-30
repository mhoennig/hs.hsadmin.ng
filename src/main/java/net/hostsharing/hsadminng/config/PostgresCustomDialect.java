package net.hostsharing.hsadminng.config;

import org.hibernate.dialect.PostgreSQL95Dialect;

import java.sql.Types;

@SuppressWarnings("unused") // configured in application.yml
public class PostgresCustomDialect extends PostgreSQL95Dialect {

    public PostgresCustomDialect() {
        this.registerHibernateType(Types.OTHER, "pg-uuid");
        this.registerHibernateType(Types.ARRAY, "array");
    }

}
