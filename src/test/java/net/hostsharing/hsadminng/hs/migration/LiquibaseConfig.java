package net.hostsharing.hsadminng.hs.migration;

import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;

@Configuration
@Profile({"liquibase-migration", "liquibase-migration-test"})
public class LiquibaseConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public LiquibaseMigration liquibase(DataSource dataSource) throws Exception {
        final var connection = dataSource.getConnection();
        final var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new LiquibaseMigration(em, "db/changelog/db.changelog-master.yaml", database);
    }
}
