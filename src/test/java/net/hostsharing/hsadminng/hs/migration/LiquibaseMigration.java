package net.hostsharing.hsadminng.hs.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class LiquibaseMigration extends Liquibase {

    private final EntityManager em;

    public LiquibaseMigration(final EntityManager em, final String changeLogFile, final Database db) {
        super(changeLogFile, new ClassLoaderResourceAccessor(), db);
        this.em = em;
    }

    @SneakyThrows
    public void runWithContexts(final String... contexts) {
        update(
                new liquibase.Contexts(contexts),
                new liquibase.LabelExpression());
    }

    public int assertReferenceStatusAfterRestore(
            final int minExpectedLiquibaseChangelogs,
            final String expectedChangesetOnlyAfterNewMigration) {
        final var schemas = singleColumnSqlQuery("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname='public'");
        assertThat(schemas).containsExactly("databasechangelog", "databasechangeloglock");

        final var liquibaseScripts = singleColumnSqlQuery("SELECT id FROM public.databasechangelog");
        assertThat(liquibaseScripts).hasSize(minExpectedLiquibaseChangelogs);
        assertThat(liquibaseScripts).doesNotContain(expectedChangesetOnlyAfterNewMigration);
        return liquibaseScripts.size();
    }

    public void assertThatCurrentMigrationsGotApplied(
            final int initialChangeSetCount,
            final String expectedChangesetOnlyAfterNewMigration) {
        final var liquibaseScripts = singleColumnSqlQuery("SELECT id FROM public.databasechangelog");
        assertThat(liquibaseScripts).hasSizeGreaterThan(initialChangeSetCount);
        assertThat(liquibaseScripts).contains(expectedChangesetOnlyAfterNewMigration);
    }

    private List<String> singleColumnSqlQuery(final String sql) {
        //noinspection unchecked
        final var rows = (List<Object>) em.createNativeQuery(sql).getResultList();
        return rows.stream().map(Objects::toString).toList();
    }
}
