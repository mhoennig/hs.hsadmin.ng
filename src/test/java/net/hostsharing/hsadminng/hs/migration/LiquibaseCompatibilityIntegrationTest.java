package net.hostsharing.hsadminng.hs.migration;

import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

// TODO.impl: The reference-SQL-dump-generation needs to be automated
// BLOG: Liquibase-migration-test (not before the reference-SQL-dump-generation is simplified)
// HOWTO: generate the prod-reference-SQL-dump during a prod-release

/**
 * Tests, if the Liquibase scripts can be applied to a database ionitialized with schemas
 * and test-data from a previous version.
 *
 * <p>The test needs a dump, ideally from the version of the lastest prod-release:</p>
 *
 * <ol>
 * <li>clean the database:<br/>
 * <code>pg-sql-reset</code>
 * </li>
 *
 * <li>restote the database from latest dump</br>
 *  <pre><code>
 *      docker exec -i hsadmin-ng-postgres psql -U postgres postgres \
 *          <src/test/resources/db/prod-only-office-schema-with-test-data.sql
 *  </code></pre>
 * </li>
 *
 * <li>run the missing migrations:</br>
 * <code>gw bootRun --args='--spring.profiles.active=only-office'</code>
 * </li>
 *
 * <li>create the reference-schema SQL-file with some initializations:</li>
 * <pre><code>
 * cat >src/test/resources/db/prod-only-office-schema-with-test-data.sql <<EOF
 * -- =================================================================================
 * -- Generated reference-SQL-dump (hopefully of latest prod-release).
 * -- See: net.hostsharing.hsadminng.hs.migration.LiquibaseCompatibilityIntegrationTest
 * -- ---------------------------------------------------------------------------------
 *
 * --
 * -- Explicit pre-initialization because we cannot use \`pg_dump --create ...\`
 * -- because the database is already created by Testcontainers.
 * --
 *
 * CREATE ROLE postgres;
 *
 * CREATE ROLE admin;
 * GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
 * CREATE ROLE restricted;
 * GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO restricted;
 *
 * EOF
 * </code></pre>
 * </li>
 *
 * <li>add the dump to that reference-schema SQL-file:</p>
 * <pre><code>docker exec -i hsadmin-ng-postgres /usr/bin/pg_dump \
 * --column-inserts --disable-dollar-quoting -U postgres postgres \
 * >>src/test/resources/db/prod-only-office-schema-with-test-data.sql
 * </code></pre>
 * </li>
 * </ol>
 *
 * <p>The generated dump has to be committed to git and will be used in future test-runs
 * until it gets replaced at the next release.</p>
 */
@Tag("officeIntegrationTest")
@DataJpaTest(properties = {
        "spring.liquibase.enabled=false" // @Sql should go first, Liquibase will be initialized programmatically
})
@DirtiesContext
@ActiveProfiles("liquibase-migration-test")
@Import({ Context.class, JpaAttempt.class, LiquibaseConfig.class })
@Sql(value = "/db/prod-only-office-schema-with-test-data.sql", executionPhase = BEFORE_TEST_CLASS)
public class LiquibaseCompatibilityIntegrationTest extends CsvDataImport {

    private static final String EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION = "hs-hosting-SCHEMA";
    private static int initialChangeSetCount = 0;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Liquibase liquibase;

    @BeforeEach
    public void setup() throws Exception {
        assertThatDatabaseIsInitialized();
        runLiquibaseMigrations();
    }

    @Test
    void test() {
        final var liquibaseScripts = singleColumnSqlQuery("SELECT id FROM public.databasechangelog");
        assertThat(liquibaseScripts).hasSizeGreaterThan(initialChangeSetCount);
        assertThat(liquibaseScripts).contains(EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);
    }

    private void assertThatDatabaseIsInitialized() {
        final var schemas = singleColumnSqlQuery("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname='public'");
        assertThat(schemas).containsExactly("databasechangelog", "databasechangeloglock");

        final var liquibaseScripts = singleColumnSqlQuery("SELECT * FROM public.databasechangelog");
        assertThat(liquibaseScripts).hasSizeGreaterThan(285);
        assertThat(liquibaseScripts).doesNotContain(EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);
        initialChangeSetCount = liquibaseScripts.size();
    }

    private void runLiquibaseMigrations() throws LiquibaseException {
        liquibase.update(new liquibase.Contexts(), new liquibase.LabelExpression());
    }

    private List<String> singleColumnSqlQuery(final String sql) {
        //noinspection unchecked
        final var rows = (List<Object>) em.createNativeQuery(sql).getResultList();
        return rows.stream().map(Objects::toString).toList();
    }
}
