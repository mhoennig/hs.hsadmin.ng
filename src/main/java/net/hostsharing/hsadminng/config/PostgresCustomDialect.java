package net.hostsharing.hsadminng.config;

import org.hibernate.dialect.PostgreSQLDialect;

import static org.hibernate.dialect.DatabaseVersion.make;

@SuppressWarnings("unused") // configured in application.yml
public class PostgresCustomDialect extends PostgreSQLDialect {

    public PostgresCustomDialect() {
        super(make(13, 7));
    }

}
