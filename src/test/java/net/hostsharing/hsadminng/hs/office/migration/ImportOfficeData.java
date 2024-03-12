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
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeReasonForTermination;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerDetailsEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relationship.HsOfficeRelationshipEntity;
import net.hostsharing.hsadminng.hs.office.relationship.HsOfficeRelationshipType;
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
        "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:admin}",
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

    static int relationshipId = 2000000;

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

    private static Map<Integer, HsOfficeRelationshipEntity> relationships = new WriteOnceMap<>();
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
                    17=partner(null null, null),
                    20=partner(null null, null),
                    22=partner(null null, null),
                    99=partner(null null, null)
                }
                """);
        assertThat(toFormattedString(contacts)).isEqualTo("{}");
        assertThat(toFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                    17=debitor(D-1001700: null null, null: mih),
                    20=debitor(D-1002000: null null, null: xyz),
                    22=debitor(D-1102200: null null, null: xxx),
                    99=debitor(D-1999900: null null, null: zzz)
                }
                """);
        assertThat(toFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                    17=Membership(M-1001700, null null, null, D-1001700, [2000-12-06,), NONE),
                    20=Membership(M-1002000, null null, null, D-1002000, [2000-12-06,2016-01-01), UNKNOWN),
                    22=Membership(M-1102200, null null, null, D-1102200, [2021-04-01,), NONE)
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
    @Order(1021)
    void buildDebitorRelationships() {
        debitors.forEach( (id, debitor) -> {
            final var debitorRel = HsOfficeRelationshipEntity.builder()
                    .relType(HsOfficeRelationshipType.DEBITOR)
                    .relAnchor(debitor.getPartner().getPartnerRole().getRelHolder())
                    .relHolder(debitor.getPartner().getPartnerRole().getRelHolder()) //  just 1 debitor/partner in legacy hsadmin
                    .contact(debitor.getBillingContact())
                    .build();
            if (debitorRel.getRelAnchor() != null && debitorRel.getRelHolder() != null &&
                    debitorRel.getContact() != null ) {
                relationships.put(relationshipId++, debitorRel);
            }
        });
    }

    @Test
    @Order(1029)
    void verifyContacts() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toFormattedString(partners)).isEqualToIgnoringWhitespace("""
                {
                    17=partner(NP Mellies, Michael: Herr Michael Mellies ),
                    20=partner(LP JM GmbH: Herr Philip Meyer-Contract , JM GmbH),
                    22=partner(?? Test PS: Petra Schmidt , Test PS),
                    99=partner(null null, null)
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
                    1401=contact(label='Frau Frauke Fanninga ', emailAddresses='ff@example.org')
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
                    1401=person(personType='NP', tradeName='', familyName='Fanninga', givenName='Frauke')
                }
                """);
        assertThat(toFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                    17=debitor(D-1001700: NP Mellies, Michael: mih), 
                    20=debitor(D-1002000: LP JM GmbH: xyz), 
                    22=debitor(D-1102200: ?? Test PS: xxx),
                    99=debitor(D-1999900: null null, null: zzz)
                }
                """);
        assertThat(toFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                    17=Membership(M-1001700, NP Mellies, Michael, D-1001700, [2000-12-06,), NONE),
                    20=Membership(M-1002000, LP JM GmbH, D-1002000, [2000-12-06,2016-01-01), UNKNOWN),
                    22=Membership(M-1102200, ?? Test PS, D-1102200, [2021-04-01,), NONE)
                }
                """);
        assertThat(toFormattedString(relationships)).isEqualToIgnoringWhitespace("""
                {
                    2000000=rel(relAnchor='LP Hostsharing eG', relType='PARTNER', relHolder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000001=rel(relAnchor='LP Hostsharing eG', relType='PARTNER', relHolder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000002=rel(relAnchor='LP Hostsharing eG', relType='PARTNER', relHolder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000003=rel(relAnchor='LP Hostsharing eG', relType='PARTNER', relHolder='null null, null'),
                    2000004=rel(relAnchor='NP Mellies, Michael', relType='OPERATIONS', relHolder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000005=rel(relAnchor='NP Mellies, Michael', relType='REPRESENTATIVE', relHolder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000006=rel(relAnchor='LP JM GmbH', relType='EX_PARTNER', relHolder='LP JM e.K.', contact='JM e.K.'),
                    2000007=rel(relAnchor='LP JM GmbH', relType='OPERATIONS', relHolder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000008=rel(relAnchor='LP JM GmbH', relType='VIP_CONTACT', relHolder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000009=rel(relAnchor='LP JM GmbH', relType='SUBSCRIBER', relMark='operations-announce', relHolder='LP JM GmbH', contact='Herr Andrew Meyer-Operation , JM GmbH'),
                    2000010=rel(relAnchor='LP JM GmbH', relType='REPRESENTATIVE', relHolder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000011=rel(relAnchor='LP JM GmbH', relType='SUBSCRIBER', relMark='members-announce', relHolder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000012=rel(relAnchor='LP JM GmbH', relType='SUBSCRIBER', relMark='customers-announce', relHolder='LP JM GmbH', contact='Herr Philip Meyer-Contract , JM GmbH'),
                    2000013=rel(relAnchor='LP JM GmbH', relType='VIP_CONTACT', relHolder='LP JM GmbH', contact='Frau Tammy Meyer-VIP , JM GmbH'),
                    2000014=rel(relAnchor='?? Test PS', relType='OPERATIONS', relHolder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000015=rel(relAnchor='?? Test PS', relType='REPRESENTATIVE', relHolder='?? Test PS', contact='Petra Schmidt , Test PS'),
                    2000016=rel(relAnchor='NP Mellies, Michael', relType='SUBSCRIBER', relMark='operations-announce', relHolder='NP Fanninga, Frauke', contact='Frau Frauke Fanninga '),
                    2000017=rel(relAnchor='NP Mellies, Michael', relType='DEBITOR', relHolder='NP Mellies, Michael', contact='Herr Michael Mellies '),
                    2000018=rel(relAnchor='LP JM GmbH', relType='DEBITOR', relHolder='LP JM GmbH', contact='Frau Dr. Jenny Meyer-Billing , JM GmbH'),
                    2000019=rel(relAnchor='?? Test PS', relType='DEBITOR', relHolder='?? Test PS', contact='Petra Schmidt , Test PS')
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
                234234=bankAccount(holder='Michael Mellies', iban='DE37500105177419788228', bic='INGDDEFFXXX'),
                235600=bankAccount(holder='JM e.K.', iban='DE02300209000106531065', bic='CMCIDEDD'),
                235662=bankAccount(holder='JM GmbH', iban='DE49500105174516484892', bic='INGDDEFFXXX')
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
    @Order(1049)
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
                    30000=CoopAssetsTransaction(1001700, 2000-12-06, DEPOSIT, 1280.00, for subscription A),
                    31000=CoopAssetsTransaction(1002000, 2000-12-06, DEPOSIT, 128.00, for subscription B),
                    32000=CoopAssetsTransaction(1001700, 2005-01-10, DEPOSIT, 2560.00, for subscription C),
                    33001=CoopAssetsTransaction(1001700, 2005-01-10, TRANSFER, -512.00, for transfer to 10),
                    33002=CoopAssetsTransaction(1002000, 2005-01-10, ADOPTION, 512.00, for transfer from 7),
                    34001=CoopAssetsTransaction(1002000, 2016-12-31, CLEARING, -8.00, for cancellation D),
                    34002=CoopAssetsTransaction(1002000, 2016-12-31, DISBURSAL, -100.00, for cancellation D),
                    34003=CoopAssetsTransaction(1002000, 2016-12-31, LOSS, -20.00, for cancellation D)
                }
                """);
    }

    @Test
    @Order(2000)
    void verifyAllPartnersHavePersons() {
        partners.forEach((id, p) -> {
            if ( id != 99 ) {
                assertThat(p.getContact()).describedAs("partner " + id + " without contact").isNotNull();
                assertThat(p.getContact().getLabel()).describedAs("partner " + id + " without valid contact").isNotNull();
                assertThat(p.getPerson()).describedAs("partner " + id + " without person").isNotNull();
                assertThat(p.getPerson().getPersonType()).describedAs("partner " + id + " without valid person").isNotNull();
            }
        });
    }

    @Test
    @Order(2009)
    void removeEmptyRelationships() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberetely invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        relationships.forEach( (id, r) -> {
            // such a record
            if (r.getContact() == null || r.getContact().getLabel() == null ||
               r.getRelHolder() == null | r.getRelHolder().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });
        assertThat(idsToRemove.size()).isEqualTo(1); // only from partner #99 (partner+contractual roles)
        idsToRemove.forEach(id -> relationships.remove(id));
    }

    @Test
    @Order(2002)
    void removeEmptyPartners() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        partners.forEach( (id, r) -> {
            // such a record
            if (r.getContact() == null || r.getContact().getLabel() == null ||
                    r.getPerson() == null | r.getPerson().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });
        assertThat(idsToRemove.size()).isEqualTo(1); // only from partner #99
        idsToRemove.forEach(id -> partners.remove(id));
    }

    @Test
    @Order(2003)
    void removeEmptyDebitors() {
        assumeThatWeAreImportingControlledTestData();

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        debitors.forEach( (id, r) -> {
            // such a record
            if (r.getBillingContact() == null || r.getBillingContact().getLabel() == null ||
                    r.getPartner().getPerson() == null | r.getPartner().getPerson().getPersonType() == null ) {
                idsToRemove.add(id);
            }
        });
        assertThat(idsToRemove.size()).isEqualTo(1); // only from partner #99
        idsToRemove.forEach(id -> debitors.remove(id));
    }

    @Test
    @Order(3000)
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
            relationships.forEach(this::persist);
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            partners.forEach(this::persist);
            updateLegacyIds(partners, "hs_office_partner_legacy_id", "bp_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            debitors.forEach(this::persist);
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
            em.createNativeQuery("delete from hs_office_relationship where true").executeUpdate();
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

                    final var partnerRelationship = HsOfficeRelationshipEntity.builder()
                            .relHolder(person)
                            .relType(HsOfficeRelationshipType.PARTNER)
                            .relAnchor(mandant)
                            .contact(null) // is set during contacts import depending on assigned roles
                            .build();
                    relationships.put(relationshipId++, partnerRelationship);

                    final var partner = HsOfficePartnerEntity.builder()
                            .partnerNumber(rec.getInteger("member_id"))
                            .details(HsOfficePartnerDetailsEntity.builder().build())
                            .partnerRole(partnerRelationship)
                            .contact(null) // is set during contacts import depending on assigned roles
                            .person(person)
                            .build();
                    partners.put(rec.getInteger("bp_id"), partner);

                    final var debitor = HsOfficeDebitorEntity.builder()
                            .partner(partner)
                            .debitorNumberSuffix((byte) 0)
                            .defaultPrefix(rec.getString("member_code").replace("hsh00-", ""))
                            .partner(partner)
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
                                .reasonForTermination(
                                        isBlank(rec.getString("member_until"))
                                                ? HsOfficeReasonForTermination.NONE
                                                : HsOfficeReasonForTermination.UNKNOWN)
                                .mainDebitor(debitor)
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
                    final var member = memberships.get(rec.getInteger("bp_id"));

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
                    final var member = memberships.get(rec.getInteger("bp_id"));

                    final var assetTypeMapping = new HashMap<String, HsOfficeCoopAssetsTransactionType>() {

                        {
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

                    final var partnerPerson = partner.getPerson();
                    if (containsPartnerRole(rec)) {
                        initPerson(partner.getPerson(), rec);
                    }

                    HsOfficePersonEntity contactPerson = partnerPerson;
                    if (!StringUtils.equals(rec.getString("firma"), partnerPerson.getTradeName()) ||
                            !StringUtils.equals(rec.getString("first_name"), partnerPerson.getGivenName()) ||
                            !StringUtils.equals(rec.getString("last_name"), partnerPerson.getFamilyName())) {
                        contactPerson = initPerson(HsOfficePersonEntity.builder().build(), rec);
                    }

                    final var contact = HsOfficeContactEntity.builder().build();
                    initContact(contact, rec);

                    if (containsPartnerRole(rec)) {
                        assertThat(partner.getContact()).isNull();
                        partner.setContact(contact);
                        partner.getPartnerRole().setContact(contact);
                    }
                    if (containsRole(rec, "billing")) {
                        assertThat(debitor.getBillingContact()).isNull();
                        debitor.setBillingContact(contact);
                    }
                    if (containsRole(rec, "operation")) {
                        addRelationship(partnerPerson, contactPerson, contact, HsOfficeRelationshipType.OPERATIONS);
                    }
                    if (containsRole(rec, "contractual")) {
                        addRelationship(partnerPerson, contactPerson, contact, HsOfficeRelationshipType.REPRESENTATIVE);
                    }
                    if (containsRole(rec, "ex-partner")) {
                        addRelationship(partnerPerson, contactPerson, contact, HsOfficeRelationshipType.EX_PARTNER);
                    }
                    if (containsRole(rec, "vip-contact")) {
                        addRelationship(partnerPerson, contactPerson, contact, HsOfficeRelationshipType.VIP_CONTACT);
                    }
                    for (String subscriberRole: SUBSCRIBER_ROLES) {
                        if (containsRole(rec, subscriberRole)) {
                            addRelationship(partnerPerson, contactPerson, contact, HsOfficeRelationshipType.SUBSCRIBER)
                                    .setRelMark(subscriberRole.split(":")[1])
                            ;
                        }
                    }
                    verifyContainsOnlyKnownRoles(rec.getString("roles"));
                });

        optionallyAddMissingContractualRelationships();
    }

    private static void optionallyAddMissingContractualRelationships() {
        final var contractualMissing = new HashSet<Integer>();
        partners.forEach( (id, partner) -> {
            final var partnerPerson = partner.getPerson();
            if (relationships.values().stream()
                    .filter(rel -> rel.getRelAnchor() == partnerPerson && rel.getRelType() == HsOfficeRelationshipType.REPRESENTATIVE)
                    .findFirst().isEmpty()) {
                contractualMissing.add(partner.getPartnerNumber());
            }
        });
    }
    private static boolean containsRole(final Record rec, final String role) {
        final var roles = rec.getString("roles");
        return ("," + roles + ",").contains("," + role + ",");
    }

    private static boolean containsPartnerRole(final Record rec) {
        return containsRole(rec, "partner");
    }

    private static HsOfficeRelationshipEntity addRelationship(
            final HsOfficePersonEntity partnerPerson,
            final HsOfficePersonEntity contactPerson,
            final HsOfficeContactEntity contact,
            final HsOfficeRelationshipType representative) {
        final var rel = HsOfficeRelationshipEntity.builder()
                .relAnchor(partnerPerson)
                .relHolder(contactPerson)
                .contact(contact)
                .relType(representative)
                .build();
        relationships.put(relationshipId++, rel);
        return rel;
    }

    private HsOfficePersonEntity initPerson(final HsOfficePersonEntity person, final Record contactRecord) {
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

    Byte getByte(final String columnName) {
        final String value = getString(columnName);
        return isNotBlank(value) ? Byte.valueOf(value.trim()) : 0;
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
