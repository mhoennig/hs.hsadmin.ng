package net.hostsharing.hsadminng.hs.office.migration;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionEntity;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType;
import net.hostsharing.hsadminng.hs.office.coopshares.HsOfficeCoopSharesTransactionEntity;
import net.hostsharing.hsadminng.hs.office.coopshares.HsOfficeCoopSharesTransactionType;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipStatus;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerDetailsEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.hs.office.sepamandate.HsOfficeSepaMandateEntity;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.Fail.fail;

/*
 * This 'test' includes the complete legacy 'office' data import.
 *
 * There is no code in 'main' because the import is not needed a normal runtime.
 * There is some test data in Java resources to verify the data conversion.
 * For a real import a main method will be added later
 * which reads CSV files from the file system.
 *
 * When run on a Hostsharing database, it needs the following settings (hsh99_... just examples).
 *
 * In a real Hostsharing environment, these are created via (the old) hsadmin:

    CREATE USER hsh99_admin WITH PASSWORD 'password';
    CREATE DATABASE hsh99_hsadminng  ENCODING 'UTF8' TEMPLATE template0;
    REVOKE ALL ON DATABASE hsh99_hsadminng FROM public; -- why does hsadmin do that?
    ALTER DATABASE hsh99_hsadminng OWNER TO hsh99_admin;

    CREATE USER hsh99_restricted WITH PASSWORD 'password';

    \c hsh99_hsadminng

    GRANT ALL PRIVILEGES ON SCHEMA public to hsh99_admin;

 * Additionally, we need these settings (because the Hostsharing DB-Admin has no CREATE right):

    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    -- maybe something like that is needed for the 2nd user
    -- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public to hsh99_restricted;

 * Then copy this to a file named .environment (excluded from git) and fill in your specific values:

   export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:6432/hsh99_hsadminng
   export HSADMINNG_POSTGRES_ADMIN_USERNAME=hsh99_admin
   export HSADMINNG_POSTGRES_ADMIN_PASSWORD=password
   export HSADMINNG_POSTGRES_RESTRICTED_USERNAME=hsh99_restricted
   export HSADMINNG_SUPERUSER=some-precreated-superuser@example.org

 * To finally import the office data, run:
 *
 *   import-office-tables # comes from .aliases file and uses .environment
 */
@Tag("import")
@DataJpaTest(properties = {
        "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:15.5-bookworm:///spring_boot_testcontainers}",
        "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:ADMIN}",
        "spring.datasource.password=${HSADMINNG_POSTGRES_ADMIN_PASSWORD:password}",
        "hsadminng.superuser=${HSADMINNG_SUPERUSER:superuser-alex@hostsharing.net}"
})
@DirtiesContext
@Import({ Context.class, JpaAttempt.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(OrderedDependedTestsExtension.class)
public class ImportOfficeData extends ContextBasedTest {

    private static final String[] SUBSCRIBER_ROLES = new String[] {
            "subscriber:operations-discussion",
            "subscriber:operations-announce",
            "subscriber:members-announce",
            "subscriber:members-discussion",
            "subscriber:customers-announce"
    };
    private static final String[] KNOWN_ROLES = ArrayUtils.addAll(
            new String[]{"partner", "vip-contact", "ex-partner", "billing", "contractual", "operation"},
            SUBSCRIBER_ROLES);

    static int relationId = 2000000;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String postgresAdminUser;

    @Value("${hsadminng.superuser}")
    private String rbacSuperuser;

    private static Map<Integer, HsOfficeContactEntity> contacts = new WriteOnceMap<>();
    private static Map<Integer, HsOfficePersonEntity> persons = new WriteOnceMap<>();
    private static Map<Integer, HsOfficePartnerEntity> partners = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeDebitorEntity> debitors = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeMembershipEntity> memberships = new WriteOnceMap<>();

    private static Map<Integer, HsOfficeRelationEntity> relations = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeSepaMandateEntity> sepaMandates = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeBankAccountEntity> bankAccounts = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeCoopSharesTransactionEntity> coopShares = new WriteOnceMap<>();
    private static Map<Integer, HsOfficeCoopAssetsTransactionEntity> coopAssets = new WriteOnceMap<>();

    @PersistenceContext
    EntityManager em;

    @Autowired
    TransactionTemplate txTemplate;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    @Test
    @Order(1010)
    void importBusinessPartners() {

        try (Reader reader = resourceReader("migration/business-partners.csv")) {
            final var lines = readAllLines(reader);
            importBusinessPartners(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1019)
    void verifyBusinessPartners() {
        assumeThatWeAreImportingControlledTestData();

        // no contacts yet => mostly null values
        assertThat(toFormattedString(partners)).isEqualToIgnoringWhitespace("""
                {
                    17=partner(P-10017: null null, null),
                    20=partner(P-10020: null null, null),
                    22=partner(P-11022: null null, null),
                    90=partner(P-19090: null null, null),
                    99=partner(P-19999: null null, null)
                }
                """);
        assertThat(toFormattedString(contacts)).isEqualTo("{}");
        assertThat(toFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                    17=debitor(D-1001700: rel(anchor='null null, null', type='DEBITOR'), mih),
                    20=debitor(D-1002000: rel(anchor='null null, null', type='DEBITOR'), xyz),
                    22=debitor(D-1102200: rel(anchor='null null, null', type='DEBITOR'), xxx),
                    90=debitor(D-1909000: rel(anchor='null null, null', type='DEBITOR'), yyy),
                    99=debitor(D-1999900: rel(anchor='null null, null', type='DEBITOR'), zzz)
                }
                """);
        assertThat(toFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                    17=Membership(M-1001700, P-10017, [2000-12-06,), ACTIVE),
                    20=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                    22=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE)
                }
                """);
    }

    @Test
    @Order(1020)
    void importContacts() {

        try (Reader reader = resourceReader("migration/contacts.csv")) {
            final var lines = readAllLines(reader);
            importContacts(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1029)
    void verifyContacts() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(partners)).isEqualToIgnoringWhitespace("""
                {
                    17=partner(P-10017: NP Mellies, Michael, Herr Michael Mellies ),
                    20=partner(P-10020: LP JM GmbH, Herr Philip Meyer-Contract , JM GmbH),
                    22=partner(P-11022: ?? Test PS, Petra Schmidt , Test PS),
                    90=partner(P-19090: NP Camus, Cecilia, Frau Cecilia Camus ),
                    99=partner(P-19999: null null, null)
                }
                """);
        assertThat(toFormattedString(contacts)).isEqualToIgnoringWhitespace("""
                {
                    1101=contact(label='Herr Michael Mellies ', emailAddresses='mih@example.org'),
                    1200=contact(label='JM e.K.', emailAddresses='jm-ex-partner@example.org'),
                    1201=contact(label='Frau Dr. Jenny Meyer-Billing , JM GmbH', emailAddresses='jm-billing@example.org'),
                    1202=contact(label='Herr Andrew Meyer-Operation , JM GmbH', emailAddresses='am-operation@example.org'),
                    1203=contact(label='Herr Philip Meyer-Contract , JM GmbH', emailAddresses='pm-partner@example.org'),
                    1204=contact(label='Frau Tammy Meyer-VIP , JM GmbH', emailAddresses='tm-vip@example.org'),
                    1301=contact(label='Petra Schmidt , Test PS', emailAddresses='ps@example.com'),
                    1401=contact(label='Frau Frauke Fanninga ', emailAddresses='ff@example.org'),
                    1501=contact(label='Frau Cecilia Camus ', emailAddresses='cc@example.org')
                }
                """);
        assertThat(toFormattedString(persons)).isEqualToIgnoringWhitespace("""
                {
                    1=person(personType='LP', tradeName='Hostsharing eG'),
                    1101=person(personType='NP', tradeName='', familyName='Mellies', givenName='Michael'),
                    1200=person(personType='LP', tradeName='JM e.K.', familyName='', givenName=''),
                    1201=person(personType='LP', tradeName='JM GmbH', familyName='Meyer-Billing', givenName='Jenny'),
                    1202=person(personType='LP', tradeName='JM GmbH', familyName='Meyer-Operation', givenName='Andrew'),
                    1203=person(personType='LP', tradeName='JM GmbH', familyName='Meyer-Contract', givenName='Philip'),
                    1204=person(personType='LP', tradeName='JM GmbH', familyName='Meyer-VIP', givenName='Tammy'),
                    1301=person(personType='??', tradeName='Test PS', familyName='Schmidt', givenName='Petra'),
                    1401=person(personType='NP', tradeName='', familyName='Fanninga', givenName='Frauke'),
                    1501=person(personType='NP', tradeName='', familyName='Camus', givenName='Cecilia')
                }
                """);
        assertThat(toFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                    17=debitor(D-1001700: rel(anchor='NP Mellies, Michael', type='DEBITOR', holder='NP Mellies, Michael'), mih),
                    20=debitor(D-1002000: rel(anchor='LP JM GmbH', type='DEBITOR', holder='LP JM GmbH'), xyz),
                    22=debitor(D-1102200: rel(anchor='?? Test PS', type='DEBITOR', holder='?? Test PS'), xxx),
                    90=debitor(D-1909000: rel(anchor='NP Camus, Cecilia', type='DEBITOR', holder='NP Camus, Cecilia'), yyy),
                    99=debitor(D-1999900: rel(anchor='null null, null', type='DEBITOR'), zzz)
                }
                """);
        assertThat(toFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                    17=Membership(M-1001700, P-10017, [2000-12-06,), ACTIVE),
                    20=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                    22=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE)
                }
                """);
        assertThat(toFormattedString(relations)).isEqualToIgnoringWhitespace("""
                {
                    2000000=rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000001=rel(anchor='NP Mellies, Michael', type='DEBITOR', holder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000002=rel(anchor='LP Hostsharing eG', type='PARTNER', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000003=rel(anchor='LP JM GmbH', type='DEBITOR', holder='LP JM GmbH', contact='Frau Dr. Jenny Meyer-Billing , JM GmbH'),
                    2000004=rel(anchor='LP Hostsharing eG', type='PARTNER', holder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000005=rel(anchor='?? Test PS', type='DEBITOR', holder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000006=rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus '),
                    2000007=rel(anchor='NP Camus, Cecilia', type='DEBITOR', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus '),
                    2000008=rel(anchor='LP Hostsharing eG', type='PARTNER', holder='null null, null'),
                    2000009=rel(anchor='null null, null', type='DEBITOR'),
                    2000010=rel(anchor='NP Mellies, Michael', type='OPERATIONS', holder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000011=rel(anchor='NP Mellies, Michael', type='REPRESENTATIVE', holder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000012=rel(anchor='LP JM GmbH', type='EX_PARTNER', holder='LP JM e.K.', contact='JM e.K.'),
                    2000013=rel(anchor='LP JM GmbH', type='OPERATIONS', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000014=rel(anchor='LP JM GmbH', type='VIP_CONTACT', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000015=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='operations-announce', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000016=rel(anchor='LP JM GmbH', type='REPRESENTATIVE', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000017=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='members-announce', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000018=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='customers-announce', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000019=rel(anchor='LP JM GmbH', type='VIP_CONTACT', holder='LP JM GmbH', contact='Frau Tammy Meyer-VIP , JM GmbH'),
                    2000020=rel(anchor='?? Test PS', type='OPERATIONS', holder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000021=rel(anchor='?? Test PS', type='REPRESENTATIVE', holder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000022=rel(anchor='NP Mellies, Michael', type='SUBSCRIBER', mark='operations-announce', holder='NP Fanninga, Frauke', contact='Frau Frauke Fanninga '),
                    2000023=rel(anchor='NP Camus, Cecilia', type='OPERATIONS', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus '),
                    2000024=rel(anchor='NP Camus, Cecilia', type='REPRESENTATIVE', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus ')
                }
                """);
    }

    @Test
    @Order(1030)
    void importSepaMandates() {

        try (Reader reader = resourceReader("migration/sepa-mandates.csv")) {
            final var lines = readAllLines(reader);
            importSepaMandates(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1039)
    void verifySepaMandates() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(bankAccounts)).isEqualToIgnoringWhitespace("""
            {
                234234=bankAccount(DE37500105177419788228: holder='Michael Mellies', bic='INGDDEFFXXX'),
                235600=bankAccount(DE02300209000106531065: holder='JM e.K.', bic='CMCIDEDD'),
                235662=bankAccount(DE49500105174516484892: holder='JM GmbH', bic='INGDDEFFXXX')
            }
            """);
        assertThat(toFormattedString(sepaMandates)).isEqualToIgnoringWhitespace("""
            {
                234234=SEPA-Mandate(DE37500105177419788228, MH12345, 2004-06-12, [2004-06-15,)),
                235600=SEPA-Mandate(DE02300209000106531065, JM33344, 2004-01-15, [2004-01-20,2005-06-28)),
                235662=SEPA-Mandate(DE49500105174516484892, JM33344, 2005-06-28, [2005-07-01,))
            }
            """);
    }

    @Test
    @Order(1040)
    void importCoopShares() {
        try (Reader reader = resourceReader("migration/share-transactions.csv")) {
            final var lines = readAllLines(reader);
            importCoopShares(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1041)
    void verifyCoopShares() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(coopShares)).isEqualToIgnoringWhitespace("""
                {
                    33443=CoopShareTransaction(M-1001700, 2000-12-06, SUBSCRIPTION, 20, initial share subscription),
                    33451=CoopShareTransaction(M-1002000, 2000-12-06, SUBSCRIPTION, 2, initial share subscription),
                    33701=CoopShareTransaction(M-1001700, 2005-01-10, SUBSCRIPTION, 40, increase),
                    33810=CoopShareTransaction(M-1002000, 2016-12-31, CANCELLATION, 22, membership ended)
                }
                """);
    }

    @Test
    @Order(1050)
    void importCoopAssets() {

        try (Reader reader = resourceReader("migration/asset-transactions.csv")) {
            final var lines = readAllLines(reader);
            importCoopAssets(justHeader(lines), withoutHeader(lines));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1059)
    void verifyCoopAssets() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(coopAssets)).isEqualToIgnoringWhitespace("""
                {
                    30000=CoopAssetsTransaction(M-1001700: 2000-12-06, DEPOSIT, 1280.00, for subscription A),
                    31000=CoopAssetsTransaction(M-1002000: 2000-12-06, DEPOSIT, 128.00, for subscription B),
                    32000=CoopAssetsTransaction(M-1001700: 2005-01-10, DEPOSIT, 2560.00, for subscription C),
                    33001=CoopAssetsTransaction(M-1001700: 2005-01-10, TRANSFER, -512.00, for transfer to 10),
                    33002=CoopAssetsTransaction(M-1002000: 2005-01-10, ADOPTION, 512.00, for transfer from 7),
                    34001=CoopAssetsTransaction(M-1002000: 2016-12-31, CLEARING, -8.00, for cancellation D),
                    34002=CoopAssetsTransaction(M-1002000: 2016-12-31, DISBURSAL, -100.00, for cancellation D),
                    34003=CoopAssetsTransaction(M-1002000: 2016-12-31, LOSS, -20.00, for cancellation D),
                    35001=CoopAssetsTransaction(M-1909000: 2024-01-15, DEPOSIT, 128.00, for subscription E),
                    35002=CoopAssetsTransaction(M-1909000: 2024-01-20, ADJUSTMENT, -128.00, chargeback for subscription E)
                }
                """);
    }

    @Test
    @Order(1099)
    void verifyMemberships() {
        assumeThatWeAreImportingControlledTestData();
        assertThat(toFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                    17=Membership(M-1001700, P-10017, [2000-12-06,), ACTIVE),
                    20=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                    22=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE),
                    90=Membership(M-1909000, P-19090, empty, INVALID)
                }
                """);
    }

    @Test
    @Order(2000)
    void verifyAllPartnersHavePersons() {
        partners.forEach((id, p) -> {
            final var partnerRel = p.getPartnerRel();
            assertThat(partnerRel).describedAs("partner " + id + " without partnerRel").isNotNull();
            if ( id != 99 ) {
                assertThat(partnerRel.getContact()).describedAs("partner " + id + " without partnerRel.contact").isNotNull();
                assertThat(partnerRel.getContact().getLabel()).describedAs("partner " + id + " without valid partnerRel.contact").isNotNull();
                assertThat(partnerRel.getHolder()).describedAs("partner " + id + " without partnerRel.relHolder").isNotNull();
                assertThat(partnerRel.getHolder().getPersonType()).describedAs("partner " + id + " without valid partnerRel.relHolder").isNotNull();
            }
        });
    }

    @Test
    @Order(3001)
    void removeSelfRepresentativeRelations() {
        assumeThatWeAreImportingControlledTestData();

        // this happens if a natural person is marked as 'contractual' for itself
        final var idsToRemove = new HashSet<Integer>();
        relations.forEach( (id, r) -> {
            if (r.getHolder() == r.getAnchor() ) {
                idsToRemove.add(id);
            }
        });

        // remove self-representatives
        idsToRemove.forEach(id -> {
            System.out.println("removing self representative relation: " + relations.get(id).toString());
            relations.remove(id);
        });
    }

    @Test
    @Order(3002)
    void removeEmptyRelations() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        relations.forEach( (id, r) -> {
            if (r.getContact() == null || r.getContact().getLabel() == null ||
               r.getHolder() == null || r.getHolder().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });

        // expected relations created from partner #99 + Hostsharing eG itself
        idsToRemove.forEach(id -> {
            System.out.println("removing unused relation: " + relations.get(id).toString());
            relations.remove(id);
        });
    }

    @Test
    @Order(3003)
    void removeEmptyPartners() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        partners.forEach( (id, r) -> {
            final var partnerRole = r.getPartnerRel();

            // such a record is in test data to test error messages
            if (partnerRole.getContact() == null || partnerRole.getContact().getLabel() == null ||
                    partnerRole.getHolder() == null | partnerRole.getHolder().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });

        // expected partners created from partner #99 + Hostsharing eG itself
        idsToRemove.forEach(id -> {
            System.out.println("removing unused partner: " + partners.get(id).toString());
            partners.remove(id);
        });
    }

    @Test
    @Order(3004)
    void removeEmptyDebitors() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        debitors.forEach( (id, d) -> {
            final var debitorRel = d.getDebitorRel();
            if (debitorRel.getContact() == null || debitorRel.getContact().getLabel() == null ||
                    debitorRel.getAnchor() == null || debitorRel.getAnchor().getPersonType() == null ||
                    debitorRel.getHolder() == null || debitorRel.getHolder().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });
        assertThat(idsToRemove.size()).isEqualTo(1); // only from partner #99
        idsToRemove.forEach(id -> debitors.remove(id));
    }

    @Test
    @Order(9000)
    @Commit
    void persistEntities() {

        System.out.println("PERSISTING to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        deleteTestDataFromHsOfficeTables();
        resetFromHsOfficeSequences();
        deleteFromTestTables();
        deleteFromRbacTables();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            contacts.forEach(this::persist);
            updateLegacyIds(contacts, "hs_office_contact_legacy_id", "contact_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            persons.forEach(this::persist);
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            relations.forEach(this::persist);
        }).assertSuccessful();

        System.out.println("persisting " + partners.size() + " partners");
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            partners.forEach((id, partner) -> {
                // TODO: this is ugly and I don't know why it's suddenly necessary
                partner.getPartnerRel().setAnchor(em.merge(partner.getPartnerRel().getAnchor()));
                partner.getPartnerRel().setHolder(em.merge(partner.getPartnerRel().getHolder()));
                partner.getPartnerRel().setContact(em.merge(partner.getPartnerRel().getContact()));
                partner.setPartnerRel(em.merge(partner.getPartnerRel()));
                em.persist(partner);
            });
            updateLegacyIds(partners, "hs_office_partner_legacy_id", "bp_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            debitors.forEach((id, debitor) -> {
                debitor.setDebitorRel(em.merge(debitor.getDebitorRel()));
                persist(id, debitor);
            });
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            memberships.forEach(this::persist);
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            bankAccounts.forEach(this::persist);
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            sepaMandates.forEach(this::persist);
            updateLegacyIds(sepaMandates, "hs_office_sepamandate_legacy_id", "sepa_mandate_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            coopShares.forEach(this::persist);
            updateLegacyIds(coopShares, "hs_office_coopsharestransaction_legacy_id", "member_share_id");

        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            coopAssets.forEach(this::persist);
            updateLegacyIds(coopAssets, "hs_office_coopassetstransaction_legacy_id", "member_asset_id");
        }).assertSuccessful();

    }

    private void persist(final Integer id, final RbacObject entity) {
        try {
            //System.out.println("persisting #" + entity.hashCode() + ": " + entity);
            em.persist(entity);
            // uncomment for debugging purposes
            // em.flush();
            // System.out.println("persisted #" + entity.hashCode() + " as " + entity.getUuid());
        } catch (Exception exc) {
            System.err.println("failed to persist #" + entity.hashCode() + ": " + entity);
            System.err.println(exc);
        }

    }

    private static void assumeThatWeAreImportingControlledTestData() {
        assumeThat(partners.size()).isLessThan(100);
    }

    private void deleteTestDataFromHsOfficeTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
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

    private void resetFromHsOfficeSequences() {
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

    private void deleteFromTestTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            em.createNativeQuery("delete from test_domain where true").executeUpdate();
            em.createNativeQuery("delete from test_package where true").executeUpdate();
            em.createNativeQuery("delete from test_customer where true").executeUpdate();
        }).assertSuccessful();
    }

    private void deleteFromRbacTables() {
        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            em.createNativeQuery("delete from rbacuser_rv where name not like 'superuser-%'").executeUpdate();
            em.createNativeQuery("delete from tx_journal where true").executeUpdate();
            em.createNativeQuery("delete from tx_context where true").executeUpdate();
        }).assertSuccessful();
    }

    private <E extends RbacObject> void updateLegacyIds(
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

    private void importBusinessPartners(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        final var mandant = HsOfficePersonEntity.builder()
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("Hostsharing eG")
                .build();
        persons.put(1, mandant);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var person = HsOfficePersonEntity.builder().build();

                    final var partnerRel = addRelation(
                            HsOfficeRelationType.PARTNER, mandant, person,
                            null  // is set during contacts import depending on assigned roles
                    );

                    final var partner = HsOfficePartnerEntity.builder()
                            .partnerNumber(rec.getInteger("member_id"))
                            .details(HsOfficePartnerDetailsEntity.builder().build())
                            .partnerRel(partnerRel)
                            .build();
                    partners.put(rec.getInteger("bp_id"), partner);

                    final var debitorRel = addRelation(
                            HsOfficeRelationType.DEBITOR, partnerRel.getHolder(), // partner person
                            null, // will be set in contacts import
                            null // will beset in contacts import
                    );

                    final var debitor = HsOfficeDebitorEntity.builder()
                            .debitorNumberSuffix("00")
                            .partner(partner)
                            .debitorRel(debitorRel)
                            .defaultPrefix(rec.getString("member_code").replace("hsh00-", ""))
                            .billable(rec.isEmpty("free") || rec.getString("free").equals("f"))
                            .vatReverseCharge(rec.getBoolean("exempt_vat"))
                            .vatBusiness("GROSS".equals(rec.getString("indicator_vat"))) // TODO: remove
                            .vatId(rec.getString("uid_vat"))
                            .build();
                    debitors.put(rec.getInteger("bp_id"), debitor);

                    if (isNotBlank(rec.getString("member_since"))) {
                        assertThat(rec.getInteger("member_id")).isEqualTo(partner.getPartnerNumber());
                        final var membership = HsOfficeMembershipEntity.builder()
                                .partner(partner)
                                .memberNumberSuffix("00")
                                .validity(toPostgresDateRange(
                                        rec.getLocalDate("member_since"),
                                        rec.getLocalDate("member_until")))
                                .membershipFeeBillable(rec.isEmpty("member_role"))
                                .status(
                                        isBlank(rec.getString("member_until"))
                                                ? HsOfficeMembershipStatus.ACTIVE
                                                : HsOfficeMembershipStatus.UNKNOWN)
                                .build();
                        memberships.put(rec.getInteger("bp_id"), membership);
                    }
                });
    }

    private void importCoopShares(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var bpId = rec.getInteger("bp_id");
                    final var member = ofNullable(memberships.get(bpId))
                            .orElseGet(() -> createOnDemandMembership(bpId));

                    final var shareTransaction = HsOfficeCoopSharesTransactionEntity.builder()
                            .membership(member)
                            .valueDate(rec.getLocalDate("date"))
                            .transactionType(
                                    "SUBSCRIPTION".equals(rec.getString("action"))
                                            ? HsOfficeCoopSharesTransactionType.SUBSCRIPTION
                                            : "UNSUBSCRIPTION".equals(rec.getString("action"))
                                            ? HsOfficeCoopSharesTransactionType.CANCELLATION
                                            : HsOfficeCoopSharesTransactionType.ADJUSTMENT
                            )
                            .shareCount(rec.getInteger("quantity"))
                            .comment( rec.getString("comment"))
                            .build();

                    coopShares.put(rec.getInteger("member_share_id"), shareTransaction);
                });
    }

    private void importCoopAssets(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var bpId = rec.getInteger("bp_id");
                    final var member = ofNullable(memberships.get(bpId))
                            .orElseGet(() -> createOnDemandMembership(bpId));

                    final var assetTypeMapping = new HashMap<String, HsOfficeCoopAssetsTransactionType>() {

                        {
                            put("ADJUSTMENT", HsOfficeCoopAssetsTransactionType.ADJUSTMENT);
                            put("HANDOVER", HsOfficeCoopAssetsTransactionType.TRANSFER);
                            put("ADOPTION", HsOfficeCoopAssetsTransactionType.ADOPTION);
                            put("LOSS", HsOfficeCoopAssetsTransactionType.LOSS);
                            put("CLEARING", HsOfficeCoopAssetsTransactionType.CLEARING);
                            put("PRESCRIPTION", HsOfficeCoopAssetsTransactionType.LIMITATION);
                            put("PAYBACK", HsOfficeCoopAssetsTransactionType.DISBURSAL);
                            put("PAYMENT", HsOfficeCoopAssetsTransactionType.DEPOSIT);
                        }

                        public HsOfficeCoopAssetsTransactionType get(final String key) {
                            final var value = super.get(key);
                            if (value != null) {
                                return value;
                            }
                            throw new IllegalStateException("no mapping value found for: " + key);
                        }
                    };

                    final var assetTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
                            .membership(member)
                            .valueDate(rec.getLocalDate("date"))
                            .transactionType(assetTypeMapping.get(rec.getString("action")))
                            .assetValue(rec.getBigDecimal("amount"))
                            .comment(rec.getString("comment"))
                            .build();

                    coopAssets.put(rec.getInteger("member_asset_id"), assetTransaction);
                });
    }

    private static HsOfficeMembershipEntity createOnDemandMembership(final Integer bpId) {
        final var onDemandMembership = HsOfficeMembershipEntity.builder()
                .memberNumberSuffix("00")
                .membershipFeeBillable(false)
                .partner(partners.get(bpId))
                .status(HsOfficeMembershipStatus.INVALID)
                .build();
        memberships.put(bpId, onDemandMembership);
        return onDemandMembership;
    }

    private void importSepaMandates(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var debitor = debitors.get(rec.getInteger("bp_id"));

                    final var sepaMandate = HsOfficeSepaMandateEntity.builder()
                            .debitor(debitor)
                            .bankAccount(HsOfficeBankAccountEntity.builder()
                                    .holder(rec.getString("bank_customer"))
                                    // .bankName(rec.get("bank_name")) // not supported
                                    .iban(rec.getString("bank_iban"))
                                    .bic(rec.getString("bank_bic"))
                                    .build())
                            .reference(rec.getString("mandat_ref"))
                            .agreement(LocalDate.parse(rec.getString("mandat_signed")))
                            .validity(toPostgresDateRange(
                                    rec.getLocalDate("mandat_since"),
                                    rec.getLocalDate("mandat_until")))
                            .build();

                    sepaMandates.put(rec.getInteger("sepa_mandat_id"), sepaMandate);
                    bankAccounts.put(rec.getInteger("sepa_mandat_id"), sepaMandate.getBankAccount());
                });
    }

    private void importContacts(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final var contactId = rec.getInteger("contact_id");
                    final var bpId = rec.getInteger("bp_id");

                    if (rec.getString("roles").isBlank()) {
                        fail("empty roles assignment not allowed for contact_id: " + contactId);
                    }

                    final var partner = partners.get(bpId);
                    final var debitor = debitors.get(bpId);

                    final var partnerPerson = partner.getPartnerRel().getHolder();
                    if (containsPartnerRel(rec)) {
                        addPerson(partnerPerson, rec);
                    }

                    HsOfficePersonEntity contactPerson = partnerPerson;
                    if (!StringUtils.equals(rec.getString("firma"), partnerPerson.getTradeName()) ||
                            !StringUtils.equals(rec.getString("first_name"), partnerPerson.getGivenName()) ||
                            !StringUtils.equals(rec.getString("last_name"), partnerPerson.getFamilyName())) {
                        contactPerson = addPerson(HsOfficePersonEntity.builder().build(), rec);
                    }

                    final var contact = HsOfficeContactEntity.builder().build();
                    initContact(contact, rec);

                    if (containsPartnerRel(rec)) {
                        assertThat(partner.getPartnerRel().getContact()).isNull();
                        partner.getPartnerRel().setContact(contact);
                    }
                    if (containsRole(rec, "billing")) {
                        assertThat(debitor.getDebitorRel().getContact()).isNull();
                        debitor.getDebitorRel().setHolder(contactPerson);
                        debitor.getDebitorRel().setContact(contact);
                    }
                    if (containsRole(rec, "operation")) {
                        addRelation(HsOfficeRelationType.OPERATIONS, partnerPerson, contactPerson, contact);
                    }
                    if (containsRole(rec, "contractual")) {
                        addRelation(HsOfficeRelationType.REPRESENTATIVE, partnerPerson, contactPerson, contact);
                    }
                    if (containsRole(rec, "ex-partner")) {
                        addRelation(HsOfficeRelationType.EX_PARTNER, partnerPerson, contactPerson, contact);
                    }
                    if (containsRole(rec, "vip-contact")) {
                        addRelation(HsOfficeRelationType.VIP_CONTACT, partnerPerson, contactPerson, contact);
                    }
                    for (String subscriberRole: SUBSCRIBER_ROLES) {
                        if (containsRole(rec, subscriberRole)) {
                            addRelation(HsOfficeRelationType.SUBSCRIBER, partnerPerson, contactPerson, contact)
                                    .setMark(subscriberRole.split(":")[1])
                            ;
                        }
                    }
                    verifyContainsOnlyKnownRoles(rec.getString("roles"));
                });

        optionallyAddMissingContractualRelations();
    }

    private static void optionallyAddMissingContractualRelations() {
        final var contractualMissing = new HashSet<Integer>();
        partners.forEach( (id, partner) -> {
            final var partnerPerson = partner.getPartnerRel().getHolder();
            if (relations.values().stream()
                    .filter(rel -> rel.getAnchor() == partnerPerson && rel.getType() == HsOfficeRelationType.REPRESENTATIVE)
                    .findFirst().isEmpty()) {
                contractualMissing.add(partner.getPartnerNumber());
            }
        });
        assertThat(contractualMissing).containsOnly(19999); // deliberately wrong partner entry
    }
    private static boolean containsRole(final Record rec, final String role) {
        final var roles = rec.getString("roles");
        return ("," + roles + ",").contains("," + role + ",");
    }

    private static boolean containsPartnerRel(final Record rec) {
        return containsRole(rec, "partner");
    }

    private static HsOfficeRelationEntity addRelation(
            final HsOfficeRelationType type,
            final HsOfficePersonEntity anchor,
            final HsOfficePersonEntity holder,
            final HsOfficeContactEntity contact) {
        final var rel = HsOfficeRelationEntity.builder()
                .anchor(anchor)
                .holder(holder)
                .contact(contact)
                .type(type)
                .build();
        relations.put(relationId++, rel);
        return rel;
    }

    private HsOfficePersonEntity addPerson(final HsOfficePersonEntity person, final Record contactRecord) {
        // TODO: title+salutation: add to person
        person.setGivenName(contactRecord.getString("first_name"));
        person.setFamilyName(contactRecord.getString("last_name"));
        person.setTradeName(contactRecord.getString("firma"));
        determinePersonType(person, contactRecord.getString("roles"));

        persons.put(contactRecord.getInteger("contact_id"), person);
        return person;
    }

    private static void determinePersonType(final HsOfficePersonEntity person, final String roles) {
        if (person.getTradeName().isBlank()) {
            person.setPersonType(HsOfficePersonType.NATURAL_PERSON);
        } else
            // contractual && !partner with a firm and a natural person name
            // should actually be split up into two persons
            // but the legacy database consists such records
            if (roles.contains("contractual") && !roles.contains("partner") &&
                !person.getFamilyName().isBlank() && !person.getGivenName().isBlank()) {
            person.setPersonType(HsOfficePersonType.NATURAL_PERSON);
        } else if ( endsWithWord(person.getTradeName(), "e.K.", "e.G.", "eG", "GmbH", "AG", "KG")  ) {
            person.setPersonType(HsOfficePersonType.LEGAL_PERSON);
        } else if ( endsWithWord(person.getTradeName(), "OHG")  ) {
                person.setPersonType(HsOfficePersonType.INCORPORATED_FIRM);
            } else if ( endsWithWord(person.getTradeName(), "GbR")  ) {
                person.setPersonType(HsOfficePersonType.INCORPORATED_FIRM);
        } else {
            person.setPersonType(HsOfficePersonType.UNKNOWN_PERSON_TYPE);
        }
    }

    private static boolean endsWithWord(final String value, final String... endings) {
        final var lowerCaseValue = value.toLowerCase();
        for( String ending: endings ) {
            if (lowerCaseValue.endsWith(" " + ending.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void verifyContainsOnlyKnownRoles(final String roles) {
        final var allowedRolesSet = stream(KNOWN_ROLES).collect(Collectors.toSet());
        final var givenRolesSet = stream(roles.replace(" ", "").split(",")).collect(Collectors.toSet());
        final var unexpectedRolesSet = new HashSet<>(givenRolesSet);
        unexpectedRolesSet.removeAll(allowedRolesSet);
        assertThat(unexpectedRolesSet).isEmpty();
    }

    private HsOfficeContactEntity initContact(final HsOfficeContactEntity contact, final Record contactRecord) {

        contact.setLabel(toLabel(
                contactRecord.getString("salut"),
                contactRecord.getString("title"),
                contactRecord.getString("first_name"),
                contactRecord.getString("last_name"),
                contactRecord.getString("firma")));
        contact.setEmailAddresses(contactRecord.getString("email"));
        contact.setPostalAddress(toAddress(contactRecord));
        contact.setPhoneNumbers(toPhoneNumbers(contactRecord));

        contacts.put(contactRecord.getInteger("contact_id"), contact);
        return contact;
    }

    private <E> String toFormattedString(final Map<Integer, E> map) {
        if ( map.isEmpty() ) {
            return "{}";
        }
        return "{\n" +
                map.keySet().stream()
                        .map(id -> "   " + id + "=" + map.get(id).toString())
                        .collect(Collectors.joining(",\n")) +
                "\n}\n";
    }

    private String[] trimAll(final String[] record) {
        for (int i = 0; i < record.length; ++i) {
            if (record[i] != null) {
                record[i] = record[i].trim();
            }
        }
        return record;
    }

    private String toPhoneNumbers(final Record rec) {
        final var result = new StringBuilder("{\n");
        if (isNotBlank(rec.getString("phone_private")))
            result.append("    \"private\": " + "\"" + rec.getString("phone_private") + "\",\n");
        if (isNotBlank(rec.getString("phone_office")))
            result.append("    \"office\": " + "\"" + rec.getString("phone_office") + "\",\n");
        if (isNotBlank(rec.getString("phone_mobile")))
            result.append("    \"mobile\": " + "\"" + rec.getString("phone_mobile") + "\",\n");
        if (isNotBlank(rec.getString("fax")))
            result.append("    \"fax\": " + "\"" + rec.getString("fax") + "\",\n");
        return (result + "}").replace("\",\n}", "\"\n}");
    }

    private String toAddress(final Record rec) {
        final var result = new StringBuilder();
        final var name = toName(
                rec.getString("salut"),
                rec.getString("title"),
                rec.getString("first_name"),
                rec.getString("last_name"));
        if (isNotBlank(name))
            result.append(name + "\n");
        if (isNotBlank(rec.getString("firma")))
            result.append(rec.getString("firma") + "\n");
        if (isNotBlank(rec.getString("co")))
            result.append("c/o " + rec.getString("co") + "\n");
        if (isNotBlank(rec.getString("street")))
            result.append(rec.getString("street") + "\n");
        final var zipcodeAndCity = toZipcodeAndCity(rec);
        if (isNotBlank(zipcodeAndCity))
            result.append(zipcodeAndCity + "\n");
        return result.toString();
    }

    private String toZipcodeAndCity(final Record rec) {
        final var result = new StringBuilder();
        if (isNotBlank(rec.getString("country")))
            result.append(rec.getString("country") + " ");
        if (isNotBlank(rec.getString("zipcode")))
            result.append(rec.getString("zipcode") + " ");
        if (isNotBlank(rec.getString("city")))
            result.append(rec.getString("city"));
        return result.toString();
    }

    private String toLabel(
            final String salut,
            final String title,
            final String firstname,
            final String lastname,
            final String firm) {
        final var result = new StringBuilder();
        if (isNotBlank(salut))
            result.append(salut + " ");
        if (isNotBlank(title))
            result.append(title + " ");
        if (isNotBlank(firstname))
            result.append(firstname + " ");
        if (isNotBlank(lastname))
            result.append(lastname + " ");
        if (isNotBlank(firm)) {
            result.append( (isBlank(result) ? "" : ", ") + firm);
        }
        return result.toString();
    }

    private String toName(final String salut, final String title, final String firstname, final String lastname) {
        return toLabel(salut, title, firstname, lastname, null);
    }

    private Reader resourceReader(@NotNull final String resourcePath) {
        return new InputStreamReader(requireNonNull(getClass().getClassLoader().getResourceAsStream(resourcePath)));
    }

    private Reader fileReader(@NotNull final Path filePath) throws IOException {
        //        Path path = Paths.get(
        //                ClassLoader.getSystemResource("csv/twoColumn.csv").toURI())
        //    );
        return Files.newBufferedReader(filePath);
    }

    private static String[] justHeader(final List<String[]> lines) {
        return stream(lines.getFirst()).map(String::trim).toArray(String[]::new);
    }

    private List<String[]> withoutHeader(final List<String[]> records) {
        return records.subList(1, records.size());
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
        return isNotBlank(value) ? Integer.parseInt(value.trim()) : 0;
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
