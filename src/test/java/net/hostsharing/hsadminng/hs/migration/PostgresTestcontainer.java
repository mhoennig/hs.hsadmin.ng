package net.hostsharing.hsadminng.hs.migration;

import lombok.SneakyThrows;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.jdbc.ContainerDatabaseDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.assertj.core.api.Assertions.assertThat;

public class PostgresTestcontainer {

    @SneakyThrows
    public static void dump(final String jdbcUrl, final File targetFileName) {
        makeDir(targetFileName.getParentFile());

        final var jdbcDatabaseContainer = getJdbcDatabaseContainer(jdbcUrl);

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

    private static void makeDir(final File dir) {
        assertThat(!dir.exists() || dir.isDirectory()).describedAs(dir + " does exist, but is not a directory").isTrue();
        assertThat(dir.isDirectory() || dir.mkdirs()).describedAs(dir + " cannot be created").isTrue();
    }

    @SneakyThrows
    private static JdbcDatabaseContainer<?> getJdbcDatabaseContainer(final String jdbcUrl) {
        // TODO.test: check if, in the future, there is a better way to access auto-created Testcontainers
        final var getContainerMethod = ContainerDatabaseDriver.class.getDeclaredMethod("getContainer", String.class);
        getContainerMethod.setAccessible(true);

        @SuppressWarnings("rawtypes")
        final var container = (JdbcDatabaseContainer) getContainerMethod.invoke(null, jdbcUrl);
        return container;
    }
}
