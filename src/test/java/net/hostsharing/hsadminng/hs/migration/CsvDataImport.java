package net.hostsharing.hsadminng.hs.migration;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

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

    @MockBean
    HttpServletRequest request;

    private static final List<AssertionError> errors = new ArrayList<>();

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
        return new InputStreamReader(requireNonNull(getClass().getClassLoader().getResourceAsStream(resourcePath)));
    }

    protected List<String[]> withoutHeader(final List<String[]> records) {
        return records.subList(1, records.size());
    }

    String[] trimAll(final String[] record) {
        for (int i = 0; i < record.length; ++i) {
            if (record[i] != null) {
                record[i] = record[i].trim();
            }
        }
        return record;
    }

    public <T extends RbacObject> T persist(final Integer id, final T entity) {
        try {
            final var asString = entity.toString();
            if ( asString.contains("'null null, null'") || asString.equals("person()")) {
                System.err.println("skipping to persist empty record-id " + id +  " #" + entity.hashCode() + ": " + entity);
                return entity;
            }
            //System.out.println("persisting #" + entity.hashCode() + ": " + entity);
            em.persist(entity);
            // uncomment for debugging purposes
            // em.flush(); // makes it slow, but produces better error messages
            // System.out.println("persisted #" + entity.hashCode() + " as " + entity.getUuid());
        } catch (Exception exc) {
            System.err.println("failed to persist #" + entity.hashCode() + ": " + entity);
            System.err.println(exc);
        }
        return entity;
    }

    protected <E> String toFormattedString(final Map<Integer, E> map) {
        if ( map.isEmpty() ) {
            return "{}";
        }
        return "{\n" +
                map.keySet().stream()
                        .map(id -> "   " + id + "=" + map.get(id).toString())
                        .map(e -> e.replaceAll("\n    ", " ").replace("\n", ""))
                        .sorted()
                        .collect(Collectors.joining(",\n")) +
                "\n}\n";
    }

    protected void deleteTestDataFromHsOfficeTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            // TODO.perf: could we instead skip creating test-data based on an env var?
            em.createNativeQuery("delete from hs_hosting_asset where true").executeUpdate();
            em.createNativeQuery("delete from hs_booking_item where true").executeUpdate();
            em.createNativeQuery("delete from hs_booking_project where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_coopassetstransaction where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_coopassetstransaction_legacy_id where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_coopsharestransaction where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_coopsharestransaction_legacy_id where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_membership where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_sepamandate where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_sepamandate_legacy_id where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_debitor where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_bankaccount where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_partner where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_partner_details where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_relation where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_contact where true").executeUpdate();
            em.createNativeQuery("delete from hs_office_person where true").executeUpdate();
        }).assertSuccessful();
    }

    protected void resetHsOfficeSequences() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            em.createNativeQuery("alter sequence hs_office_contact_legacy_id_seq restart with 1000000000;").executeUpdate();
            em.createNativeQuery("alter sequence hs_office_coopassetstransaction_legacy_id_seq restart with 1000000000;")
                    .executeUpdate();
            em.createNativeQuery("alter sequence public.hs_office_coopsharestransaction_legacy_id_seq restart with 1000000000;")
                    .executeUpdate();
            em.createNativeQuery("alter sequence public.hs_office_partner_legacy_id_seq restart with 1000000000;")
                    .executeUpdate();
            em.createNativeQuery("alter sequence public.hs_office_sepamandate_legacy_id_seq restart with 1000000000;")
                    .executeUpdate();
        });
    }

    protected void deleteFromTestTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            em.createNativeQuery("delete from test_domain where true").executeUpdate();
            em.createNativeQuery("delete from test_package where true").executeUpdate();
            em.createNativeQuery("delete from test_customer where true").executeUpdate();
        }).assertSuccessful();
    }

    protected void deleteFromRbacTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            em.createNativeQuery("delete from rbacuser_rv where name not like 'superuser-%'").executeUpdate();
            em.createNativeQuery("delete from tx_journal where true").executeUpdate();
            em.createNativeQuery("delete from tx_context where true").executeUpdate();
        }).assertSuccessful();
    }

    void logError(final Runnable assertion) {
        try {
            assertion.run();
        } catch (final AssertionError exc) {
            errors.add(exc);
        }
    }

    void logErrors() {
        assumeThat(errors).isEmpty();
    }
}

class Columns {

    private final List<String> columnNames;

    public Columns(final String[] header) {
        columnNames = List.of(header);
    }

    int indexOf(final String columnName) {
        int index = columnNames.indexOf(columnName);
        if (index < 0) {
            throw new RuntimeException("column name '" + columnName + "' not found in: " + columnNames);
        }
        return index;
    }
}

class Record {

    private final Columns columns;
    private final String[] row;

    public Record(final Columns columns, final String[] row) {
        this.columns = columns;
        this.row = row;
    }

    String getString(final String columnName) {
        return row[columns.indexOf(columnName)];
    }

    boolean isEmpty(final String columnName) {
        final String value = getString(columnName);
        return value == null || value.isBlank();
    }

    boolean getBoolean(final String columnName) {
        final String value = getString(columnName);
        return isNotBlank(value) &&
                ( parseBoolean(value.trim()) || value.trim().startsWith("t"));
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

class OrderedDependedTestsExtension implements TestWatcher, BeforeEachCallback {

    private static boolean previousTestsPassed = true;

    public void testFailed(ExtensionContext context, Throwable cause) {
        previousTestsPassed = false;
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
