package net.hostsharing.hsadminng.hs.migration;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("liquibase-migration-test")
public class LiquibaseConfig {

    @Bean
    public Liquibase liquibase(DataSource dataSource) throws Exception {
        final var connection = dataSource.getConnection();
        final var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new Liquibase(
                "db/changelog/db.changelog-master.yaml", // Path to your Liquibase changelog
                new ClassLoaderResourceAccessor(),
                database
        );
    }
}
