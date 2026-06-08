package net.hostsharing.hsadminng.config;

import lombok.val;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresCustomDialectUnitTest {

    @Test
    void constructsPostgresDialectForConfiguredVersion() {
        // when
        val dialect = new PostgresCustomDialect();

        // then
        assertThat(dialect).isInstanceOf(PostgreSQLDialect.class);
        assertThat(dialect.getVersion().getMajor()).isEqualTo(15);
        assertThat(dialect.getVersion().getMinor()).isEqualTo(5);
    }
}
