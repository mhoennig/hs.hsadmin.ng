package net.hostsharing.hsadminng.hs.migration;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.Array.emptyArray;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.fail;

public class CsvDataImport extends ContextBasedTest {

    public static final String TEST_DATA_MIGRATION_DATA_PATH = "migration";
    public static final String MIGRATION_DATA_PATH = ofNullable(System.getenv("HSADMINNG_MIGRATION_DATA_PATH"))
            .orElse(TEST_DATA_MIGRATION_DATA_PATH);

    @Value("${spring.datasource.url}")
    protected String jdbcUrl;

    @Value("${spring.datasource.username}")
    protected String postgresAdminUser;

    @Value("${hsadminng.superuser}")
    protected String rbacSuperuser;

    @PersistenceContext
    EntityManager em;

    @Autowired
    TransactionTemplate txTemplate;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
    HttpServletRequest request;

    static final LinkedHashSet<String> errors = new LinkedHashSet<>();

    public List<String[]> readAllLines(Reader reader) throws Exception {

        final var parser = new CSVParserBuilder()
                .withSeparator(';')
                .withQuoteChar('"')
                .build();

        final var filteredReader = skippingEmptyAndCommentLines(reader);
        try (CSVReader csvReader = new CSVReaderBuilder(filteredReader)
                .withCSVParser(parser)
                .build()) {
            return csvReader.readAll();
        }
    }

    public static Reader skippingEmptyAndCommentLines(Reader reader) throws IOException {
        try (var bufferedReader = new BufferedReader(reader);
             StringWriter writer = new StringWriter()) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    writer.write(line);
                    writer.write("\n");
                }
            }

            return new StringReader(writer.toString());
        }
    }

    protected static String[] justHeader(final List<String[]> lines) {
        return stream(lines.getFirst()).map(String::trim).toArray(String[]::new);
    }

    protected Reader resourceReader(@NotNull final String resourcePath) {
        try {
            return new InputStreamReader(requireNonNull(getClass().getClassLoader().getResourceAsStream(resourcePath)));
        } catch (Exception exc) {
            throw new AssertionFailedError("cannot open '" + resourcePath + "'");
        }
    }

    @SneakyThrows
    protected String resourceAsString(final Resource resource) {
        final var lines = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8);
        return String.join("\n", lines);
    }

    protected List<String[]> withoutHeader(final List<String[]> records) {
        return records.subList(1, records.size());
    }

    @SneakyThrows
    public static String[] parseCsvLine(final String csvLine) {
        try (final var reader = new CSVReader(new StringReader(csvLine))) {
            return stream(ofNullable(reader.readNext()).orElse(emptyArray(String.class)))
                    .map(String::trim)
                    .map(target -> target.startsWith("'") && target.endsWith("'") ?
                            target.substring(1, target.length() - 1) :
                            target)
                    .toArray(String[]::new);
        }
    }

    String[] trimAll(final String[] record) {
        for (int i = 0; i < record.length; ++i) {
            if (record[i] != null) {
                record[i] = record[i].trim();
            }
        }
        return record;
    }

    public <T extends BaseEntity> T persist(final Integer id, final T entity) {
        try {
            if (entity instanceof HsHostingAsset ha) {
                //noinspection unchecked
                return (T) persistViaSql(id, ha);
            }
            return persistViaEM(id, entity);
        } catch (Exception exc) {
            errors.add("failed to persist #" + entity.hashCode() + ": " + entity);
            errors.add(exc.toString());
        }
        return entity;
    }

    public <T extends BaseEntity> T persistViaEM(final Integer id, final T entity) {
        if (em.contains(entity)) {
            return entity;
        }
        try {
            em.persist(entity);
            em.flush(); // makes it a bit slower, but produces better error messages
            System.out.println("persisted #" + id + " as " + entity.getUuid());
            return entity;
        } catch (final Exception exc) {
            System.err.println("persist failed for #" + id + " as " + entity);
            throw exc; // for breakpoints
        }
    }

    @SneakyThrows
    public BaseEntity<HsHostingAsset> persistViaSql(final Integer id, final HsHostingAsset entity) {
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }

        final var query = em.createNativeQuery("""
                         insert into hs_hosting.asset(
                                    uuid,
                                    type,
                                    bookingitemuuid,
                                    parentassetuuid,
                                    assignedtoassetuuid,
                                    alarmcontactuuid,
                                    identifier,
                                    caption,
                                    config,
                                    version)
                        values (
                                    :uuid,
                                    :type,
                                    :bookingitemuuid,
                                    :parentassetuuid,
                                    :assignedtoassetuuid,
                                    :alarmcontactuuid,
                                    :identifier,
                                    :caption,
                                    cast(:config as jsonb),
                                    :version)
                        """)
                .setParameter("uuid", entity.getUuid())
                .setParameter("type", entity.getType().name())
                .setParameter("bookingitemuuid", ofNullable(entity.getBookingItem()).map(BaseEntity::getUuid).orElse(null))
                .setParameter("parentassetuuid", ofNullable(entity.getParentAsset()).map(BaseEntity::getUuid).orElse(null))
                .setParameter(
                        "assignedtoassetuuid",
                        ofNullable(entity.getAssignedToAsset()).map(BaseEntity::getUuid).orElse(null))
                .setParameter("alarmcontactuuid", ofNullable(entity.getAlarmContact()).map(BaseEntity::getUuid).orElse(null))
                .setParameter("identifier", entity.getIdentifier())
                .setParameter("caption", entity.getCaption())
                .setParameter("config", entity.getConfig().toString().replace("\t", "\\t"))
                .setParameter("version", entity.getVersion());

        final var count = query.executeUpdate();
        logError(() -> {
            assertThat(count).isEqualTo(1);
        });
        return entity;
    }

    protected <E> String toJsonFormattedString(final Map<Integer, E> map) {
        if ( map.isEmpty() ) {
            return "{}";
        }
        final var json = "{\n" +
                map.keySet().stream()
                        .map(id -> "   " + id + "=" + map.get(id).toString())
                        .map(e -> e.replaceAll("\n    ", " ").replace("\n", "").replace(" : ", ": ").replace("{  ", "{").replace(",  ", ", "))
                        .sorted()
                        .collect(Collectors.joining(",\n")) +
                "\n}\n";
        return json;
    }

    protected void makeSureThatTheImportAdminUserExists() {
        jpaAttempt.transacted(() -> {
            context(null);
            em.createNativeQuery("""
                do language plpgsql $$
                    declare
                        admins uuid;
                    begin
                        if not exists (select 1 from rbac.subject where name = '${rbacSuperuser}') then
                            admins = rbac.findRoleId(rbac.global_ADMIN());
                            call rbac.grantRoleToSubjectUnchecked(admins, admins, rbac.create_subject('${rbacSuperuser}'));
                        end if;
                    end;
                $$;
                """.replace("${rbacSuperuser}", rbacSuperuser))
                .executeUpdate();
        }).assertSuccessful();
    }

    // makes it possible to fail when an expression is expected
    <T> T failWith(final String message) {
        fail(message);
        return null;
    }

    void logError(final Runnable assertion) {
        try {
            assertion.run();
        } catch (final AssertionError | ValidationException exc) {
            logError(exc.getMessage());
        }
    }

    public static void logError(final String error) {
        errors.add(error);
    }

    protected static void expectError(final String expectedError) {
        final var found = errors.remove(expectedError);
        if (!found) {
            logError("expected but not found: " + expectedError);
        }
    }

    protected final void assertNoErrors() {
        final var errorsToLog = new LinkedHashSet<>(errors);
        errors.clear();
        assertThat(errorsToLog).isEmpty();
    }

    protected <E extends BaseEntity> void updateLegacyIds(
            Map<Integer, E> entities,
            final String legacyIdTable,
            final String legacyIdColumn) {
        em.flush();
        entities.forEach((id, entity) -> em.createNativeQuery("""
                            UPDATE ${legacyIdTable}
                                SET ${legacyIdColumn} = :legacyId
                                WHERE uuid = :uuid
                        """
                        .replace("${legacyIdTable}", legacyIdTable)
                        .replace("${legacyIdColumn}", legacyIdColumn))
                .setParameter("legacyId", id)
                .setParameter("uuid", entity.getUuid())
                .executeUpdate()
        );
    }
}

class Columns {

    private final List<String> columnNames;

    public Columns(final String[] header) {
        columnNames = List.of(header);
    }

    int indexOf(final String columnName) {
        return columnNames.indexOf(columnName);
    }
}

class Record {

    private final Columns columns;
    private final String[] row;

    public Record(final Columns columns, final String[] row) {
        this.columns = columns;
        this.row = row;
    }

    String getString(final String columnName, final String defaultValue) {
        final var index = columns.indexOf(columnName);
        final var value = index >= 0 && index < row.length ? row[index].trim() : null;
        return value != null ? value : defaultValue;
    }

    String getString(final String columnName) {
        return row[columns.indexOf(columnName)].trim();
    }

    boolean isEmpty(final String columnName) {
        final String value = getString(columnName);
        return value == null || value.isBlank();
    }

    boolean getBoolean(final String columnName) {
        final String value = getString(columnName);
        return isNotBlank(value) &&
                (parseBoolean(value.trim()) || value.trim().startsWith("t"));
    }

    Integer getInteger(final String columnName) {
        final String value = getString(columnName);
        return isNotBlank(value) ? Integer.parseInt(value.trim()) : null;
    }

    BigDecimal getBigDecimal(final String columnName) {
        final String value = getString(columnName);
        if (isNotBlank(value)) {
            return new BigDecimal(value);
        }
        return null;
    }

    LocalDate getLocalDate(final String columnName) {
        final String dateString = getString(columnName);
        if (isNotBlank(dateString)) {
            return LocalDate.parse(dateString);
        }
        return null;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ContinueOnFailure {
}

class OrderedDependedTestsExtension implements TestWatcher, BeforeEachCallback {

    private static boolean previousTestsPassed = true;

    @Override
    public void testFailed(final ExtensionContext context, final Throwable cause) {
        previousTestsPassed = previousTestsPassed && context.getElement()
                .map(e -> e.isAnnotationPresent(ContinueOnFailure.class))
                .orElse(false);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        assumeThat(previousTestsPassed).isTrue();
    }
}

class WriteOnceMap<K, V> extends TreeMap<K, V> {

    @Override
    public V put(final K k, final V v) {
        assertThat(containsKey(k)).describedAs("overwriting " + get(k) + " index " + k + " with " + v).isFalse();
        return super.put(k, v);
    }
}
