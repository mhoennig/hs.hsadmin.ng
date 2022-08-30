package net.hostsharing.hsadminng.config;

import com.vladmihalcea.hibernate.type.array.UUIDArrayType;
import org.hibernate.dialect.PostgreSQL95Dialect;

import java.sql.Types;

@SuppressWarnings("unused") // configured in application.yml
public class PostgreSQL95CustomDialect extends PostgreSQL95Dialect {

    public PostgreSQL95CustomDialect() {
        this.registerHibernateType(Types.OTHER, "pg-uuid");
        this.registerHibernateType(Types.ARRAY, UUIDArrayType.class.getName());
    }

}
