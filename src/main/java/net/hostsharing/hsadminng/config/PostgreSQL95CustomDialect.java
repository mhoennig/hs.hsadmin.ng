package net.hostsharing.hsadminng.config;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.dialect.PostgreSQL95Dialect;

@SuppressWarnings("unused") // configured in application.yml
public class PostgreSQL95CustomDialect extends PostgreSQL95Dialect {

    public PostgreSQL95CustomDialect() {
        this.registerHibernateType(2003, StringArrayType.class.getName());
        this.registerHibernateType(1111, "pg-uuid");
    }

}
