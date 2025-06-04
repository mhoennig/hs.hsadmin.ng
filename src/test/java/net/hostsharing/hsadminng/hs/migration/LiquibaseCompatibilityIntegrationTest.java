package net.hostsharing.hsadminng.hs.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.io.File;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

// BLOG: Liquibase-migration-test (not before the reference-SQL-dump-generation is simplified)
// HOWTO: generate the prod-reference-SQL-dump during a prod-release

/**
 * Tests, if the Liquibase scripts can be applied to a database which is already populated with schemas
 * and test-data from a previous version.
 *
 * <p>The test works as follows:</p>
 *
 * <ol>
 *     <li>the database is initialized by `db/released-only-prod-schema-with-test-data.sql` from the test-resources</li>
 *     <li>the current Liquibase-migrations (only-prod-schema but with-test-data) are performed</li>
 *     <li>a new dump is written to `db/released-only-prod-schema-with-test-data.sql` in the build-directory</li>
 *     <li>an extra Liquibase-changeset (liquibase-migration-test) is applied</li>
 *     <li>it's asserted that the extra changeset got applied</li>
 * </ol>
 *
 * <p>During a release, the generated dump has to be committed to git and will be used in future test-runs
 * until it gets replaced with a new dump at the next release.</p>
 */
@Tag("officeIntegrationTest")
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15.5-bookworm:///liquibaseMigrationTestTC",
        "hsadminng.superuser=${HSADMINNG_SUPERUSER:import-superuser@hostsharing.net}",
        "spring.liquibase.enabled=false" // @Sql should go first, Liquibase will be initialized programmatically
})
@DirtiesContext
@ActiveProfiles("liquibase-migration-test")
@Import(LiquibaseConfig.class)
@Sql(value = "/db/released-only-prod-schema-with-test-data.sql", executionPhase = BEFORE_TEST_CLASS) // release-schema
public class LiquibaseCompatibilityIntegrationTest {

    private static final String EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION = "hs-global-liquibase-migration-test";
    private static final int EXPECTED_LIQUIBASE_CHANGELOGS_IN_PROD_SCHEMA_DUMP = 287;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Autowired
    private LiquibaseMigration liquibase;

    @Test
    void migrationWorksBasedOnAPreviouslyPopulatedSchema() {
        // check the initial status from the @Sql-annotation
        final var initialChangeSetCount = liquibase.assertReferenceStatusAfterRestore(
                EXPECTED_LIQUIBASE_CHANGELOGS_IN_PROD_SCHEMA_DUMP, EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);

        // run the current migrations and dump the result to the build-directory
        liquibase.runWithContexts("only-prod-schema", "with-test-data");
        PostgresTestcontainer.dump(jdbcUrl, new File("build/db/released-only-prod-schema-with-test-data.sql"));

        // then add another migration and assert if it was applied
        liquibase.runWithContexts("liquibase-migration-test");
        liquibase.assertThatCurrentMigrationsGotApplied(
                initialChangeSetCount, EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);
    }
}
