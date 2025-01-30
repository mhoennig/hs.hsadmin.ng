package net.hostsharing.hsadminng.hs.migration;

import liquibase.Liquibase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.jdbc.ContainerDatabaseDriver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.assertj.core.api.Assertions.assertThat;
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
 *     <li>the database is initialized by `db/prod-only-office-schema-with-test-data.sql` from the test-resources</li>
 *     <li>the current Liquibase-migrations (only-office but with-test-data) are performed</li>
 *     <li>a new dump is written to `db/prod-only-office-schema-with-test-data.sql` in the build-directory</li>
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
        "spring.liquibase.enabled=false" // @Sql should go first, Liquibase will be initialized programmatically
})
@DirtiesContext
@ActiveProfiles("liquibase-migration-test")
@Import(LiquibaseConfig.class)
@Sql(value = "/db/prod-only-office-schema-with-test-data.sql", executionPhase = BEFORE_TEST_CLASS)
public class LiquibaseCompatibilityIntegrationTest {

    private static final String EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION = "hs-global-liquibase-migration-test";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Liquibase liquibase;

    @PersistenceContext
    private EntityManager em;

    @Test
    void migrationWorksBasedOnAPreviouslyPopulatedSchema() {
        // check the initial status from the @Sql-annotation
        final var initialChangeSetCount = assertProdReferenceStatusAfterRestore();

        // run the current migrations and dump the result to the build-directory
        runLiquibaseMigrationsWithContexts("only-office", "with-test-data");
        dumpTo(new File("build/db/prod-only-office-schema-with-test-data.sql"));

        // then add another migration and assert if it was applied
        runLiquibaseMigrationsWithContexts("liquibase-migration-test");
        assertThatCurrentMigrationsGotApplied(initialChangeSetCount);
    }

    private int assertProdReferenceStatusAfterRestore() {
        final var schemas = singleColumnSqlQuery("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname='public'");
        assertThat(schemas).containsExactly("databasechangelog", "databasechangeloglock");

        final var liquibaseScripts1 = singleColumnSqlQuery("SELECT * FROM public.databasechangelog");
        assertThat(liquibaseScripts1).hasSizeGreaterThan(285);
        assertThat(liquibaseScripts1).doesNotContain(EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);
        final var initialChangeSetCount = liquibaseScripts1.size();
        return initialChangeSetCount;
    }

    private void assertThatCurrentMigrationsGotApplied(final int initialChangeSetCount) {
        final var liquibaseScripts = singleColumnSqlQuery("SELECT id FROM public.databasechangelog");
        assertThat(liquibaseScripts).hasSizeGreaterThan(initialChangeSetCount);
        assertThat(liquibaseScripts).contains(EXPECTED_CHANGESET_ONLY_AFTER_NEW_MIGRATION);
    }

    @SneakyThrows
    private void dumpTo(final File targetFileName) {
        makeDir(targetFileName.getParentFile());

        final var jdbcDatabaseContainer = getJdbcDatabaseContainer();

        final var sqlDumpFile = new File(targetFileName.getParent(), "." + targetFileName.getName());
        final var pb = new ProcessBuilder(
                "pg_dump", "--column-inserts", "--disable-dollar-quoting",
                "--host=" + jdbcDatabaseContainer.getHost(),
                "--port=" + jdbcDatabaseContainer.getFirstMappedPort(),
                "--username=" + jdbcDatabaseContainer.getUsername() ,
                "--dbname=" + jdbcDatabaseContainer.getDatabaseName(),
                "--file=" + sqlDumpFile.getCanonicalPath()
        );
        pb.environment().put("PGPASSWORD", jdbcDatabaseContainer.getPassword());

        final var process = pb.start();
        int exitCode = process.waitFor();
        final var stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                .lines().collect(Collectors.joining("\n"));
        assertThat(exitCode).describedAs(stderr).isEqualTo(0);

        final var header = """
              -- =================================================================================
              -- Generated reference-SQL-dump (hopefully of latest prod-release).
              -- See: net.hostsharing.hsadminng.hs.migration.LiquibaseCompatibilityIntegrationTest
              -- ---------------------------------------------------------------------------------
              
              --
              -- Explicit pre-initialization because we cannot use `pg_dump --create ...`
              -- because the database is already created by Testcontainers.
              --
              
              CREATE ROLE postgres;
              CREATE ROLE admin;
              CREATE ROLE restricted;

              """;
        writeStringToFile(targetFileName, header, UTF_8, false); // false = overwrite

        write(targetFileName, readFileToString(sqlDumpFile, UTF_8), UTF_8, true);

        assertThat(sqlDumpFile.delete()).describedAs(sqlDumpFile + " cannot be deleted");
    }

    private void makeDir(final File dir) {
        assertThat(!dir.exists() || dir.isDirectory()).describedAs(dir + " does exist, but is not a directory").isTrue();
        assertThat(dir.isDirectory() || dir.mkdirs()).describedAs(dir + " cannot be created").isTrue();
    }

    @SneakyThrows
    private void runLiquibaseMigrationsWithContexts(final String... contexts) {
        liquibase.update(
                new liquibase.Contexts(contexts),
                new liquibase.LabelExpression());
    }

    private List<String> singleColumnSqlQuery(final String sql) {
        //noinspection unchecked
        final var rows = (List<Object>) em.createNativeQuery(sql).getResultList();
        return rows.stream().map(Objects::toString).toList();
    }

    @SneakyThrows
    private static JdbcDatabaseContainer<?> getJdbcDatabaseContainer() {
        final var getContainerMethod = ContainerDatabaseDriver.class.getDeclaredMethod("getContainer", String.class);
        getContainerMethod.setAccessible(true);

        @SuppressWarnings("rawtypes")
        final var container = (JdbcDatabaseContainer) getContainerMethod.invoke(null,
                "jdbc:tc:postgresql:15.5-bookworm:///liquibaseMigrationTestTC");
        return container;
    }
}
