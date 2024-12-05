package net.hostsharing.hsadminng.hs.migration;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionEntity;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType;
import net.hostsharing.hsadminng.hs.office.coopshares.HsOfficeCoopSharesTransactionEntity;
import net.hostsharing.hsadminng.hs.office.coopshares.HsOfficeCoopSharesTransactionType;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipStatus;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerDetailsEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelation;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.hs.office.sepamandate.HsOfficeSepaMandateEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.Fail.fail;

/// Actual import of office data tables without config, for use as superclas of ImportOfficeData and ImportHostingAssets.
public abstract class BaseOfficeDataImport extends CsvDataImport {

    private static final String[] SUBSCRIBER_ROLES = new String[] {
            "subscriber:operations-discussion",
            "subscriber:operations-announce",
            "subscriber:generalversammlung",
            "subscriber:members-announce",
            "subscriber:members-discussion",
            "subscriber:customers-announce"
    };
    private static final String[] KNOWN_ROLES = ArrayUtils.addAll(
            new String[] { "partner", "vip-contact", "ex-partner", "billing", "contractual", "operation", "silent" },
            SUBSCRIBER_ROLES);

    // at least as the number of lines in business_partners.csv from test-data, but less than real data partner count
    public static final int MAX_NUMBER_OF_TEST_DATA_PARTNERS = 100;
    public static final int DELIBERATELY_BROKEN_BUSINESS_PARTNER_ID = 199;

    static int INITIAL_RELATION_ID = 2000000;
    static int relationId = INITIAL_RELATION_ID;

    private static final List<Integer> IGNORE_BUSINESS_PARTNERS = Arrays.asList(
            -1
    );

    private static final List<Integer> IGNORE_CONTACTS = Arrays.asList(
            -1
    );

    private static final Map<Integer, HsOfficePersonType> PERSON_TYPES_BY_CONTACT = Map.of(
        90072, HsOfficePersonType.NATURAL_PERSON,
        90641, HsOfficePersonType.LEGAL_PERSON,
        90368, HsOfficePersonType.LEGAL_PERSON,
        90564, HsOfficePersonType.NATURAL_PERSON,
        -1, HsOfficePersonType.LEGAL_PERSON
    );

    static Map<Integer, HsOfficeContactRealEntity> contacts = new WriteOnceMap<>();
    static Map<Integer, HsOfficePersonRealEntity> persons = new WriteOnceMap<>();
    static Map<Integer, HsOfficePartnerEntity> partners = new WriteOnceMap<>();
    static Map<Integer, HsOfficeDebitorEntity> debitors = new WriteOnceMap<>();
    static Map<Integer, HsOfficeMembershipEntity> memberships = new WriteOnceMap<>();

    static Map<Integer, HsOfficeRelation> relations = new WriteOnceMap<>();
    static Map<Integer, HsOfficeSepaMandateEntity> sepaMandates = new WriteOnceMap<>();
    static Map<Integer, HsOfficeBankAccountEntity> bankAccounts = new WriteOnceMap<>();
    static Map<Integer, HsOfficeCoopSharesTransactionEntity> coopShares = new WriteOnceMap<>();
    static Map<Integer, HsOfficeCoopAssetsTransactionEntity> coopAssets = new WriteOnceMap<>();

    protected static void reset() {
        contacts.clear();
        persons.clear();
        partners.clear();
        debitors.clear();
        memberships.clear();
        relations.clear();
        sepaMandates.clear();
        bankAccounts.clear();
        coopShares.clear();
        coopAssets.clear();
        relationId = INITIAL_RELATION_ID;
    }

    @BeforeAll
    static void resetOfficeImports() {
        reset();
    }

    @Test
    @Order(1)
    void verifyInitialDatabase() {
        // SQL DELETE for thousands of records takes too long, so we make sure, we only start with initial or test data
        final var contactCount = (Integer) em.createNativeQuery("select count(*) from hs_office.contact", Integer.class)
                .getSingleResult();
        assertThat(contactCount).isLessThan(20);
    }

    @Test
    @Order(1010)
    void importBusinessPartners() {

        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/office/business_partners.csv")) {
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
        assertThat(toJsonFormattedString(partners)).isEqualToIgnoringWhitespace("""
                {
                   100=partner(P-10003: null null, null),
                   120=partner(P-10020: null null, null),
                   122=partner(P-11022: null null, null),
                   132=partner(P-10152: null null, null),
                   190=partner(P-19090: null null, null),
                   199=partner(P-19999: null null, null),
                   213=partner(P-10000: null null, null),
                   541=partner(P-11018: null null, null),
                   542=partner(P-11019: null null, null)
                }
                """);
        assertThat(toJsonFormattedString(contacts)).isEqualTo("{}");
        assertThat(toJsonFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                   100=debitor(D-1000300: rel(anchor='null null, null', type='DEBITOR'), mim),
                   120=debitor(D-1002000: rel(anchor='null null, null', type='DEBITOR'), xyz),
                   122=debitor(D-1102200: rel(anchor='null null, null', type='DEBITOR'), xxx),
                   132=debitor(D-1015200: rel(anchor='null null, null', type='DEBITOR'), rar),
                   190=debitor(D-1909000: rel(anchor='null null, null', type='DEBITOR'), yyy),
                   199=debitor(D-1999900: rel(anchor='null null, null', type='DEBITOR'), zzz),
                   213=debitor(D-1000000: rel(anchor='null null, null', type='DEBITOR'), hsh),
                   541=debitor(D-1101800: rel(anchor='null null, null', type='DEBITOR'), wws),
                   542=debitor(D-1101900: rel(anchor='null null, null', type='DEBITOR'), dph)
                }
                """);
        assertThat(toJsonFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                   100=Membership(M-1000300, P-10003, [2000-12-06,), ACTIVE),
                   120=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                   122=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE),
                   132=Membership(M-1015200, P-10152, [2003-07-12,), ACTIVE),
                   541=Membership(M-1101800, P-11018, [2021-05-17,), ACTIVE),
                   542=Membership(M-1101900, P-11019, [2021-05-25,), ACTIVE)
                }
                """);
    }

    @Test
    @Order(1020)
    void importContacts() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/office/contacts.csv")) {
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

        assertThat(toJsonFormattedString(partners)).isEqualToIgnoringWhitespace("""
                {
                   100=partner(P-10003: ?? Michael Mellis, Herr Michael Mellis, Michael Mellis),
                   120=partner(P-10020: LP JM GmbH, Herr Philip Meyer-Contract, JM GmbH),
                   122=partner(P-11022: ?? Test PS, Petra Schmidt, Test PS),
                   132=partner(P-10152: ?? Ragnar IT-Beratung, Herr Ragnar Richter, Ragnar IT-Beratung),
                   190=partner(P-19090: NP Camus, Cecilia, Frau Cecilia Camus),
                   199=partner(P-19999: null null, null),
                   213=partner(P-10000: LP Hostsharing e.G., Firma Hostmaster Hostsharing, Hostsharing e.G.),
                   541=partner(P-11018: ?? Wasserwerk Südholstein, Frau Christiane Milberg, Wasserwerk Südholstein),
                   542=partner(P-11019: ?? Das Perfekte Haus, Herr Richard Wiese, Das Perfekte Haus)
                }
                """);
        assertThat(toJsonFormattedString(contacts)).isEqualToIgnoringWhitespace("""
                {
                   100=contact(caption='Herr Michael Mellis, Michael Mellis', emailAddresses='{ "main": "michael@Mellis.example.org"}'),
                   1200=contact(caption='JM e.K.', emailAddresses='{ "main": "jm-ex-partner@example.org"}'),
                   1201=contact(caption='Frau Dr. Jenny Meyer-Billing, JM GmbH', emailAddresses='{ "main": "jm-billing@example.org"}'),
                   1202=contact(caption='Herr Andrew Meyer-Operation, JM GmbH', emailAddresses='{ "main": "am-operation@example.org"}'),
                   1203=contact(caption='Herr Philip Meyer-Contract, JM GmbH', emailAddresses='{ "main": "pm-partner@example.org"}'),
                   1204=contact(caption='Frau Tammy Meyer-VIP, JM GmbH', emailAddresses='{ "main": "tm-vip@example.org"}'),
                   1301=contact(caption='Petra Schmidt, Test PS', emailAddresses='{ "main": "ps@example.com"}'),
                   132=contact(caption='Herr Ragnar Richter, Ragnar IT-Beratung', emailAddresses='{ "main": "hostsharing@ragnar-richter.de"}'),
                   1401=contact(caption='Frau Frauke Fanninga', emailAddresses='{ "main": "ff@example.org"}'),
                   1501=contact(caption='Frau Cecilia Camus', emailAddresses='{ "main": "cc@example.org"}'),
                   212=contact(caption='Firma Hostmaster Hostsharing, Hostsharing e.G.', emailAddresses='{ "main": "hostmaster@hostsharing.net"}'),
                   90436=contact(caption='Frau Christiane Milberg, Wasserwerk Südholstein', emailAddresses='{ "main": "rechnung@ww-sholst.example.org"}'),
                   90437=contact(caption='Herr Richard Wiese, Das Perfekte Haus', emailAddresses='{ "main": "admin@das-perfekte-haus.example.org"}'),
                   90438=contact(caption='Herr Karim Metzger, Wasswerwerk Südholstein', emailAddresses='{ "main": "karim.metzger@ww-sholst.example.org"}'),
                   90590=contact(caption='Herr Inhaber R. Wiese, Das Perfekte Haus', emailAddresses='{ "main": "515217@kkemail.example.org"}'),
                   90629=contact(caption='Ragnar Richter', emailAddresses='{ "main": "mail@ragnar-richter..example.org"}'),
                   90677=contact(caption='Eike Henning', emailAddresses='{ "main": "hostsharing@eike-henning..example.org"}'),
                   90698=contact(caption='Jan Henning', emailAddresses='{ "main": "mail@jan-henning.example.org"}')
                }
                """);
        assertThat(toJsonFormattedString(persons)).isEqualToIgnoringWhitespace("""
                {
                   100=person(personType='??', tradeName='Michael Mellis', salutation='Herr', familyName='Mellis', givenName='Michael'),
                   1200=person(personType='LP', tradeName='JM e.K.'),
                   1201=person(personType='LP', tradeName='JM GmbH', salutation='Frau', title='Dr.', familyName='Meyer-Billing', givenName='Jenny'),
                   1202=person(personType='LP', tradeName='JM GmbH', salutation='Herr', familyName='Meyer-Operation', givenName='Andrew'),
                   1203=person(personType='LP', tradeName='JM GmbH', salutation='Herr', familyName='Meyer-Contract', givenName='Philip'),
                   1204=person(personType='LP', tradeName='JM GmbH', salutation='Frau', familyName='Meyer-VIP', givenName='Tammy'),
                   1301=person(personType='??', tradeName='Test PS', familyName='Schmidt', givenName='Petra'),
                   132=person(personType='??', tradeName='Ragnar IT-Beratung', salutation='Herr', familyName='Richter', givenName='Ragnar'),
                   1401=person(personType='NP', salutation='Frau', familyName='Fanninga', givenName='Frauke'),
                   1501=person(personType='NP', salutation='Frau', familyName='Camus', givenName='Cecilia'),
                   212=person(personType='LP', tradeName='Hostsharing e.G.', salutation='Firma', familyName='Hostsharing', givenName='Hostmaster'),
                   90436=person(personType='??', tradeName='Wasserwerk Südholstein', salutation='Frau', familyName='Milberg', givenName='Christiane'),
                   90437=person(personType='??', tradeName='Das Perfekte Haus', salutation='Herr', familyName='Wiese', givenName='Richard'),
                   90438=person(personType='??', tradeName='Wasswerwerk Südholstein', salutation='Herr', familyName='Metzger', givenName='Karim'),
                   90590=person(personType='??', tradeName='Das Perfekte Haus', salutation='Herr', familyName='Wiese', givenName='Inhaber R.'),
                   90629=person(personType='NP', familyName='Richter', givenName='Ragnar'),
                   90677=person(personType='NP', familyName='Henning', givenName='Eike'),
                   90698=person(personType='NP', familyName='Henning', givenName='Jan')
                }
                """);
        assertThat(toJsonFormattedString(debitors)).isEqualToIgnoringWhitespace("""
                {
                   100=debitor(D-1000300: rel(anchor='?? Michael Mellis', type='DEBITOR', holder='?? Michael Mellis'), mim),
                   120=debitor(D-1002000: rel(anchor='LP JM GmbH', type='DEBITOR', holder='LP JM GmbH'), xyz),
                   122=debitor(D-1102200: rel(anchor='?? Test PS', type='DEBITOR', holder='?? Test PS'), xxx),
                   132=debitor(D-1015200: rel(anchor='?? Ragnar IT-Beratung', type='DEBITOR', holder='?? Ragnar IT-Beratung'), rar),
                   190=debitor(D-1909000: rel(anchor='NP Camus, Cecilia', type='DEBITOR', holder='NP Camus, Cecilia'), yyy),
                   199=debitor(D-1999900: rel(anchor='null null, null', type='DEBITOR'), zzz),
                   213=debitor(D-1000000: rel(anchor='LP Hostsharing e.G.', type='DEBITOR', holder='LP Hostsharing e.G.'), hsh),
                   541=debitor(D-1101800: rel(anchor='?? Wasserwerk Südholstein', type='DEBITOR', holder='?? Wasserwerk Südholstein'), wws),
                   542=debitor(D-1101900: rel(anchor='?? Das Perfekte Haus', type='DEBITOR', holder='?? Das Perfekte Haus'), dph)
                }
                """);
        assertThat(toJsonFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                   100=Membership(M-1000300, P-10003, [2000-12-06,), ACTIVE),
                   120=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                   122=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE),
                   132=Membership(M-1015200, P-10152, [2003-07-12,), ACTIVE),
                   541=Membership(M-1101800, P-11018, [2021-05-17,), ACTIVE),
                   542=Membership(M-1101900, P-11019, [2021-05-25,), ACTIVE)
                }
                """);
        assertThat(toJsonFormattedString(relations)).isEqualToIgnoringWhitespace("""
                {
                   2000000=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000001=rel(anchor='?? Michael Mellis', type='DEBITOR', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000002=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000003=rel(anchor='?? Ragnar IT-Beratung', type='DEBITOR', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000004=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='LP Hostsharing e.G.', contact='Firma Hostmaster Hostsharing, Hostsharing e.G.'),
                   2000005=rel(anchor='LP Hostsharing e.G.', type='DEBITOR', holder='LP Hostsharing e.G.', contact='Firma Hostmaster Hostsharing, Hostsharing e.G.'),
                   2000006=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000007=rel(anchor='?? Wasserwerk Südholstein', type='DEBITOR', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000008=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000009=rel(anchor='?? Das Perfekte Haus', type='DEBITOR', holder='?? Das Perfekte Haus', contact='Herr Inhaber R. Wiese, Das Perfekte Haus'),
                   2000010=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract, JM GmbH'),
                   2000011=rel(anchor='LP JM GmbH', type='DEBITOR', holder='LP JM GmbH', contact='Frau Dr. Jenny Meyer-Billing, JM GmbH'),
                   2000012=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='?? Test PS', contact='Petra Schmidt, Test PS'),
                   2000013=rel(anchor='?? Test PS', type='DEBITOR', holder='?? Test PS', contact='Petra Schmidt, Test PS'),
                   2000014=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus'),
                   2000015=rel(anchor='NP Camus, Cecilia', type='DEBITOR', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus'),
                   2000016=rel(anchor='LP Hostsharing e.G.', type='PARTNER', holder='null null, null'),
                   2000017=rel(anchor='null null, null', type='DEBITOR'),
                   2000018=rel(anchor='LP Hostsharing e.G.', type='OPERATIONS_ALERT', holder='LP Hostsharing e.G.', contact='Firma Hostmaster Hostsharing, Hostsharing e.G.'),
                   2000019=rel(anchor='LP Hostsharing e.G.', type='OPERATIONS', holder='LP Hostsharing e.G.', contact='Firma Hostmaster Hostsharing, Hostsharing e.G.'),
                   2000020=rel(anchor='LP Hostsharing e.G.', type='REPRESENTATIVE', holder='LP Hostsharing e.G.', contact='Firma Hostmaster Hostsharing, Hostsharing e.G.'),
                   2000021=rel(anchor='?? Michael Mellis', type='OPERATIONS_ALERT', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000022=rel(anchor='?? Michael Mellis', type='OPERATIONS', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000023=rel(anchor='?? Michael Mellis', type='REPRESENTATIVE', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000024=rel(anchor='?? Michael Mellis', type='SUBSCRIBER', mark='operations-discussion', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000025=rel(anchor='?? Michael Mellis', type='SUBSCRIBER', mark='operations-announce', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000026=rel(anchor='?? Michael Mellis', type='SUBSCRIBER', mark='generalversammlung', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000027=rel(anchor='?? Michael Mellis', type='SUBSCRIBER', mark='members-announce', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000028=rel(anchor='?? Michael Mellis', type='SUBSCRIBER', mark='members-discussion', holder='?? Michael Mellis', contact='Herr Michael Mellis, Michael Mellis'),
                   2000029=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS_ALERT', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000030=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000031=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='operations-discussion', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000032=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='operations-announce', holder='?? Ragnar IT-Beratung', contact='Herr Ragnar Richter, Ragnar IT-Beratung'),
                   2000033=rel(anchor='LP JM GmbH', type='EX_PARTNER', holder='LP JM e.K.', contact='JM e.K.'),
                   2000034=rel(anchor='LP JM GmbH', type='OPERATIONS_ALERT', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation, JM GmbH'),
                   2000035=rel(anchor='LP JM GmbH', type='OPERATIONS', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation, JM GmbH'),
                   2000036=rel(anchor='LP JM GmbH', type='VIP_CONTACT', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation, JM GmbH'),
                   2000037=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='operations-announce', holder='LP JM GmbH', contact='Herr Andrew Meyer-Operation, JM GmbH'),
                   2000038=rel(anchor='LP JM GmbH', type='REPRESENTATIVE', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract, JM GmbH'),
                   2000039=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='members-announce', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract, JM GmbH'),
                   2000040=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='customers-announce', holder='LP JM GmbH', contact='Herr Philip Meyer-Contract, JM GmbH'),
                   2000041=rel(anchor='LP JM GmbH', type='VIP_CONTACT', holder='LP JM GmbH', contact='Frau Tammy Meyer-VIP, JM GmbH'),
                   2000042=rel(anchor='?? Test PS', type='OPERATIONS_ALERT', holder='?? Test PS', contact='Petra Schmidt, Test PS'),
                   2000043=rel(anchor='?? Test PS', type='OPERATIONS', holder='?? Test PS', contact='Petra Schmidt, Test PS'),
                   2000044=rel(anchor='?? Test PS', type='REPRESENTATIVE', holder='?? Test PS', contact='Petra Schmidt, Test PS'),
                   2000045=rel(anchor='LP JM GmbH', type='SUBSCRIBER', mark='operations-announce', holder='NP Fanninga, Frauke', contact='Frau Frauke Fanninga'),
                   2000046=rel(anchor='NP Camus, Cecilia', type='OPERATIONS_ALERT', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus'),
                   2000047=rel(anchor='NP Camus, Cecilia', type='OPERATIONS', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus'),
                   2000048=rel(anchor='NP Camus, Cecilia', type='REPRESENTATIVE', holder='NP Camus, Cecilia', contact='Frau Cecilia Camus'),
                   2000049=rel(anchor='?? Wasserwerk Südholstein', type='REPRESENTATIVE', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000050=rel(anchor='?? Wasserwerk Südholstein', type='SUBSCRIBER', mark='generalversammlung', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000051=rel(anchor='?? Wasserwerk Südholstein', type='SUBSCRIBER', mark='members-announce', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000052=rel(anchor='?? Wasserwerk Südholstein', type='SUBSCRIBER', mark='members-discussion', holder='?? Wasserwerk Südholstein', contact='Frau Christiane Milberg, Wasserwerk Südholstein'),
                   2000053=rel(anchor='?? Das Perfekte Haus', type='OPERATIONS_ALERT', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000054=rel(anchor='?? Das Perfekte Haus', type='OPERATIONS', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000055=rel(anchor='?? Das Perfekte Haus', type='REPRESENTATIVE', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000056=rel(anchor='?? Das Perfekte Haus', type='SUBSCRIBER', mark='operations-discussion', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000057=rel(anchor='?? Das Perfekte Haus', type='SUBSCRIBER', mark='operations-announce', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000058=rel(anchor='?? Das Perfekte Haus', type='SUBSCRIBER', mark='generalversammlung', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000059=rel(anchor='?? Das Perfekte Haus', type='SUBSCRIBER', mark='members-announce', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000060=rel(anchor='?? Das Perfekte Haus', type='SUBSCRIBER', mark='members-discussion', holder='?? Das Perfekte Haus', contact='Herr Richard Wiese, Das Perfekte Haus'),
                   2000061=rel(anchor='?? Wasserwerk Südholstein', type='OPERATIONS_ALERT', holder='?? Wasswerwerk Südholstein', contact='Herr Karim Metzger, Wasswerwerk Südholstein'),
                   2000062=rel(anchor='?? Wasserwerk Südholstein', type='OPERATIONS', holder='?? Wasswerwerk Südholstein', contact='Herr Karim Metzger, Wasswerwerk Südholstein'),
                   2000063=rel(anchor='?? Wasserwerk Südholstein', type='SUBSCRIBER', mark='operations-discussion', holder='?? Wasswerwerk Südholstein', contact='Herr Karim Metzger, Wasswerwerk Südholstein'),
                   2000064=rel(anchor='?? Wasserwerk Südholstein', type='SUBSCRIBER', mark='operations-announce', holder='?? Wasswerwerk Südholstein', contact='Herr Karim Metzger, Wasswerwerk Südholstein'),
                   2000065=rel(anchor='?? Ragnar IT-Beratung', type='REPRESENTATIVE', holder='NP Richter, Ragnar', contact='Ragnar Richter'),
                   2000066=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='generalversammlung', holder='NP Richter, Ragnar', contact='Ragnar Richter'),
                   2000067=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='members-announce', holder='NP Richter, Ragnar', contact='Ragnar Richter'),
                   2000068=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='members-discussion', holder='NP Richter, Ragnar', contact='Ragnar Richter'),
                   2000069=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS_ALERT', holder='NP Henning, Eike', contact='Eike Henning'),
                   2000070=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS', holder='NP Henning, Eike', contact='Eike Henning'),
                   2000071=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='operations-discussion', holder='NP Henning, Eike', contact='Eike Henning'),
                   2000072=rel(anchor='?? Ragnar IT-Beratung', type='SUBSCRIBER', mark='operations-announce', holder='NP Henning, Eike', contact='Eike Henning'),
                   2000073=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS_ALERT', holder='NP Henning, Jan', contact='Jan Henning'),
                   2000074=rel(anchor='?? Ragnar IT-Beratung', type='OPERATIONS', holder='NP Henning, Jan', contact='Jan Henning')
                }
                """);
    }

    @Test
    @Order(1030)
    void importSepaMandates() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/office/sepa_mandates.csv")) {
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

        assertThat(toJsonFormattedString(bankAccounts)).isEqualToIgnoringWhitespace("""
                {
                   132=bankAccount(DE37500105177419788228: holder='Michael Mellis', bic='GENODEF1HH2'),
                   234234=bankAccount(DE37500105177419788228: holder='Michael Mellis', bic='INGDDEFFXXX'),
                   235600=bankAccount(DE02300209000106531065: holder='JM e.K.', bic='CMCIDEDD'),
                   235662=bankAccount(DE49500105174516484892: holder='JM GmbH', bic='INGDDEFFXXX'),
                   30=bankAccount(DE02300209000106531065: holder='Ragnar Richter', bic='GENODEM1GLS'),
                   386=bankAccount(DE49500105174516484892: holder='Wasserwerk Suedholstein', bic='NOLADE21WHO'),
                   387=bankAccount(DE89370400440532013000: holder='Richard Wiese Das Perfekte Haus', bic='COBADEFFXXX')
                }
                """);
        assertThat(toJsonFormattedString(sepaMandates)).isEqualToIgnoringWhitespace("""
                {
                   132=SEPA-Mandate(DE37500105177419788228, HS-10003-20140801, 2013-12-01, [2013-12-01,)),
                   234234=SEPA-Mandate(DE37500105177419788228, MH12345, 2004-06-12, [2004-06-15,)),
                   235600=SEPA-Mandate(DE02300209000106531065, JM33344, 2004-01-15, [2004-01-20,2005-06-28)),
                   235662=SEPA-Mandate(DE49500105174516484892, JM33344, 2005-06-28, [2005-07-01,)),
                   30=SEPA-Mandate(DE02300209000106531065, HS-10152-20140801, 2013-12-01, [2013-12-01,2016-02-16)),
                   386=SEPA-Mandate(DE49500105174516484892, HS-11018-20210512, 2021-05-12, [2021-05-17,)),
                   387=SEPA-Mandate(DE89370400440532013000, HS-11019-20210519, 2021-05-19, [2021-05-25,))
                }
                """);
    }

    @Test
    @Order(1040)
    void importCoopShares() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/office/share_transactions.csv")) {
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

        assertThat(toJsonFormattedString(coopShares)).isEqualToIgnoringWhitespace("""
                {
                   241=CoopShareTransaction(M-1000300: 2011-12-05, SUBSCRIPTION, 16, 1000300),
                   279=CoopShareTransaction(M-1015200: 2013-10-21, SUBSCRIPTION, 1, 1015200),
                   33451=CoopShareTransaction(M-1002000: 2000-12-06, SUBSCRIPTION, 2, 1002000, initial share subscription),
                   33701=CoopShareTransaction(M-1000300: 2005-01-10, SUBSCRIPTION, 40, 1000300, increase),
                   33810=CoopShareTransaction(M-1002000: 2016-12-31, CANCELLATION, 22, 1002000, membership ended),
                   3=CoopShareTransaction(M-1000300: 2000-12-06, SUBSCRIPTION, 80, 1000300, initial share subscription),
                   523=CoopShareTransaction(M-1000300: 2020-12-08, SUBSCRIPTION, 96, 1000300, Kapitalerhoehung),
                   562=CoopShareTransaction(M-1101800: 2021-05-17, SUBSCRIPTION, 4, 1101800, Beitritt),
                   563=CoopShareTransaction(M-1101900: 2021-05-25, SUBSCRIPTION, 1, 1101900, Beitritt),
                   721=CoopShareTransaction(M-1000300: 2023-10-10, SUBSCRIPTION, 96, 1000300, Kapitalerhoehung),
                   90=CoopShareTransaction(M-1015200: 2003-07-12, SUBSCRIPTION, 1, 1015200)
                }
                """);
    }

    @Test
    @Order(1050)
    void importCoopAssets() {
        try (Reader reader = resourceReader(MIGRATION_DATA_PATH + "/office/asset_transactions.csv")) {
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

        assertThat(toJsonFormattedString(coopAssets)).isEqualToIgnoringWhitespace("""
                {
                   1093=CoopAssetsTransaction(M-1000300: 2023-10-05, DEPOSIT, 3072, 1000300, Kapitalerhoehung - Ueberweisung),
                   1094=CoopAssetsTransaction(M-1000300: 2023-10-06, DEPOSIT, 3072, 1000300, Kapitalerhoehung - Ueberweisung),
                   31000=CoopAssetsTransaction(M-1002000: 2000-12-06, DEPOSIT, 128.00, 1002000, for subscription B),
                   32000=CoopAssetsTransaction(M-1000300: 2005-01-10, DEPOSIT, 2560.00, 1000300, for subscription C),
                   33001=CoopAssetsTransaction(M-1000300: 2005-01-10, TRANSFER, -512.00, 1000300, for transfer to 10, M-1002000:ADO:+512.00),
                   33002=CoopAssetsTransaction(M-1002000: 2005-01-10, ADOPTION, 512.00, 1002000, for transfer from 7),
                   34001=CoopAssetsTransaction(M-1002000: 2016-12-31, CLEARING, -8.00, 1002000, for cancellation D),
                   34002=CoopAssetsTransaction(M-1002000: 2016-12-31, DISBURSAL, -100.00, 1002000, for cancellation D),
                   34003=CoopAssetsTransaction(M-1002000: 2016-12-31, LOSS, -20.00, 1002000, for cancellation D),
                   35001=CoopAssetsTransaction(M-1909000: 2024-01-15, DEPOSIT, 128.00, 1909000, for subscription E),
                   35002=CoopAssetsTransaction(M-1909000: 2024-01-20, REVERSAL, -128.00, 1909000, chargeback for subscription E, M-1909000:DEP:+128.00),
                   358=CoopAssetsTransaction(M-1000300: 2000-12-06, DEPOSIT, 5120, 1000300, for subscription A),
                   442=CoopAssetsTransaction(M-1015200: 2003-07-07, DEPOSIT, 64, 1015200),
                   577=CoopAssetsTransaction(M-1000300: 2011-12-12, DEPOSIT, 1024, 1000300),
                   632=CoopAssetsTransaction(M-1015200: 2013-10-21, DEPOSIT, 64, 1015200),
                   885=CoopAssetsTransaction(M-1000300: 2020-12-15, DEPOSIT, 6144, 1000300, Einzahlung),
                   924=CoopAssetsTransaction(M-1101800: 2021-05-21, DEPOSIT, 256, 1101800, Beitritt - Lastschrift),
                   925=CoopAssetsTransaction(M-1101900: 2021-05-31, DEPOSIT, 64, 1101900, Beitritt - Lastschrift)
                }
                """);
    }

    @Test
    @Order(1099)
    void verifyMemberships() {
        assumeThatWeAreImportingControlledTestData();

        assertThat(toJsonFormattedString(memberships)).isEqualToIgnoringWhitespace("""
                {
                   100=Membership(M-1000300, P-10003, [2000-12-06,), ACTIVE),
                   120=Membership(M-1002000, P-10020, [2000-12-06,2016-01-01), UNKNOWN),
                   122=Membership(M-1102200, P-11022, [2021-04-01,), ACTIVE),
                   132=Membership(M-1015200, P-10152, [2003-07-12,), ACTIVE),
                   190=Membership(M-1909000, P-19090, empty, INVALID),
                   541=Membership(M-1101800, P-11018, [2021-05-17,), ACTIVE),
                   542=Membership(M-1101900, P-11019, [2021-05-25,), ACTIVE)
                }
                """);
    }

    @Test
    @Order(2000)
    void verifyAllPartnersHavePersons() {
        partners.forEach((id, p) -> {
            final var partnerRel = p.getPartnerRel();
            assertThat(partnerRel).describedAs("partner " + id + " without partnerRel").isNotNull();
            if (id != DELIBERATELY_BROKEN_BUSINESS_PARTNER_ID) {
                logError(() -> {
                    assertThat(partnerRel.getContact()).describedAs("partner " + id + " without partnerRel.contact")
                            .isNotNull();
                    assertThat(partnerRel.getContact().getCaption()).describedAs(
                            "partner " + id + " without valid partnerRel.contact").isNotNull();
                });
                logError(() -> {
                    assertThat(partnerRel.getHolder()).describedAs("partner " + id + " without partnerRel.relHolder")
                            .isNotNull();
                    assertThat(partnerRel.getHolder().getPersonType()).describedAs(
                            "partner " + id + " without valid partnerRel.relHolder").isNotNull();
                });
            }
        });
    }

    @Test
    @Order(3001)
    void removeSelfRepresentativeRelations() {

        // this happens if a natural person is marked as 'contractual' for itself
        final var idsToRemove = new HashSet<Integer>();
        relations.forEach((id, r) -> {
            if (r.getType() == HsOfficeRelationType.REPRESENTATIVE && r.getHolder() == r.getAnchor()) {
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

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        relations.forEach((id, r) -> {
            if (r.getContact() == null || r.getContact().getCaption() == null ||
                    r.getHolder() == null || r.getHolder().getPersonType() == null) {
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

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        partners.forEach((id, r) -> {
            final var partnerRole = r.getPartnerRel();

            // such a record is in test data to test error messages
            if (partnerRole.getContact() == null || partnerRole.getContact().getCaption() == null ||
                    partnerRole.getHolder() == null | partnerRole.getHolder().getPersonType() == null) {
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

        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        debitors.forEach((id, d) -> {
            final var debitorRel = d.getDebitorRel();
            if (debitorRel.getContact() == null || debitorRel.getContact().getCaption() == null ||
                    debitorRel.getAnchor() == null || debitorRel.getAnchor().getPersonType() == null ||
                    debitorRel.getHolder() == null || debitorRel.getHolder().getPersonType() == null) {
                idsToRemove.add(id);
            }
        });
        idsToRemove.forEach(id -> debitors.remove(id));

        assumeThatWeAreImportingControlledTestData();
        assertThat(idsToRemove.size()).isEqualTo(1); // only from partner #99
    }

    @Test
    @Order(3005)
    void removeEmptyPersons() {
        // avoid a error when persisting the deliberately invalid partner entry #99
        final var idsToRemove = new HashSet<Integer>();
        persons.forEach((id, p) -> {
            if (p.getPersonType() == null ||
                    (p.getFamilyName() == null && p.getGivenName() == null && p.getTradeName() == null)) {
                idsToRemove.add(id);
            }
        });
        idsToRemove.forEach(id -> persons.remove(id));

        assumeThatWeAreImportingControlledTestData();
        assertThat(idsToRemove.size()).isEqualTo(0);
    }

    @Test
    @Order(9000)
    @ContinueOnFailure
    void logCollectedErrorsBeforePersist() {
        assertNoErrors();
    }

    @Test
    @Order(9010)
    void persistOfficeEntities() {

        System.out.println("PERSISTING office data to database '" + jdbcUrl + "' as user '" + postgresAdminUser + "'");
        deleteTestDataFromHsOfficeTables();
        resetHsOfficeSequences();
        deleteFromTestTables();
        deleteFromCommonTables();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            contacts.forEach(this::persist);
           updateLegacyIds(contacts, "hs_office.contact_legacy_id", "contact_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            persons.forEach(this::persist);
            relations.forEach((id, rel) -> this.persist(id, rel.getAnchor()));
            relations.forEach((id, rel) -> this.persist(id, rel.getHolder()));
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
            updateLegacyIds(partners, "hs_office.partner_legacy_id", "bp_id");
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
            updateLegacyIds(sepaMandates, "hs_office.sepamandate_legacy_id", "sepa_mandate_id");
        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            coopShares.forEach(this::persist);
            updateLegacyIds(coopShares, "hs_office.coopsharetx_legacy_id", "member_share_id");

        }).assertSuccessful();

        jpaAttempt.transacted(() -> {
            context(rbacSuperuser);
            coopAssets.forEach(this::persist);
            updateLegacyIds(coopAssets, "hs_office.coopassettx_legacy_id", "member_asset_id");
        }).assertSuccessful();

    }

    @Test
    @Order(9190)
    void verifyMembershipsActuallyPersisted() {
        final var biCount = (Integer) em.createNativeQuery("select count(*) from hs_office.membership", Integer.class)
                .getSingleResult();
        assertThat(biCount).isGreaterThan(isImportingControlledTestData() ? 5 : 300);
    }

    private static boolean isImportingControlledTestData() {
        return partners.size() <= MAX_NUMBER_OF_TEST_DATA_PARTNERS;
    }

    private static void assumeThatWeAreImportingControlledTestData() {
        assumeThat(partners.size()).isLessThanOrEqualTo(MAX_NUMBER_OF_TEST_DATA_PARTNERS);
    }

    @Test
    @Order(9999)
    @ContinueOnFailure
    void logCollectedErrors() {
        this.assertNoErrors();
    }

    private void importBusinessPartners(final String[] header, final List<String[]> records) {

        final var columns = new Columns(header);

        records.stream()
                .map(this::trimAll)
                .map(row -> new Record(columns, row))
                .forEach(rec -> {
                    final Integer bpId = rec.getInteger("bp_id");
                    if (IGNORE_BUSINESS_PARTNERS.contains(bpId)) {
                        return;
                    }

                    final var person = HsOfficePersonRealEntity.builder().build();

                    final var partnerRel = addRelation(
                            HsOfficeRelationType.PARTNER,
                            null, // is set after contacts when the person for 'Hostsharing eG' is known
                            person,
                            null  // is set during contacts import depending on assigned roles
                    );

                    final var partner = HsOfficePartnerEntity.builder()
                            .partnerNumber(rec.getInteger("member_id"))
                            .details(HsOfficePartnerDetailsEntity.builder().build())
                            .partnerRel(partnerRel)
                            .build();
                    partners.put(bpId, partner);

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
                    debitors.put(bpId, debitor);

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
                        memberships.put(bpId, membership);
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
                    if (IGNORE_BUSINESS_PARTNERS.contains(bpId)) {
                        return;
                    }

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
                                            : HsOfficeCoopSharesTransactionType.REVERSAL
                            )
                            .shareCount(rec.getInteger("quantity"))
                            .comment(rec.getString("comment"))
                            .reference(member.getMemberNumber().toString())
                            .build();

                    if (shareTransaction.getTransactionType() == HsOfficeCoopSharesTransactionType.REVERSAL) {
                        final var negativeValue = -shareTransaction.getShareCount();
                        final var revertedShareTx = coopShares.values().stream().filter(a ->
                                        a.getTransactionType() != HsOfficeCoopSharesTransactionType.REVERSAL &&
                                                a.getMembership() == shareTransaction.getMembership() &&
                                                a.getShareCount() == negativeValue)
                                .findAny()
                                .orElseThrow(() -> new IllegalStateException(
                                        "cannot determine share reverse entry for reversal " + shareTransaction));
                        shareTransaction.setRevertedShareTx(revertedShareTx);
                    }
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

                    if (this.IGNORE_BUSINESS_PARTNERS.contains(bpId)) {
                        return;
                    }

                    final var member = ofNullable(memberships.get(bpId))
                            .orElseGet(() -> createOnDemandMembership(bpId));

                    final var assetTypeMapping = new HashMap<String, HsOfficeCoopAssetsTransactionType>() {

                        {
                            put("ADJUSTMENT", HsOfficeCoopAssetsTransactionType.REVERSAL);
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
                            .reference(member.getMemberNumber().toString())
                            .build();
                    coopAssets.put(rec.getInteger("member_asset_id"), assetTransaction);
                });


        coopAssets.entrySet().forEach(entry -> {
            final var legacyId = entry.getKey();
            final var assetTransaction = entry.getValue();
            if (assetTransaction.getTransactionType() == HsOfficeCoopAssetsTransactionType.REVERSAL) {
                connectToRelatedRevertedAssetTx(legacyId, assetTransaction);
            }
            if (assetTransaction.getTransactionType() == HsOfficeCoopAssetsTransactionType.TRANSFER) {
                connectToRelatedAdoptionAssetTx(legacyId, assetTransaction);
            }
        });
    }

    private static void connectToRelatedRevertedAssetTx(final int legacyId, final HsOfficeCoopAssetsTransactionEntity assetTransaction) {
        final var negativeValue = assetTransaction.getAssetValue().negate();
        final var revertedAssetTx = coopAssets.values().stream().filter(a ->
                        a.getTransactionType() != HsOfficeCoopAssetsTransactionType.REVERSAL &&
                                a.getMembership() == assetTransaction.getMembership() &&
                                a.getAssetValue().equals(negativeValue))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(
                        "cannot determine asset reverse entry for reversal " + assetTransaction));
        assetTransaction.setRevertedAssetTx(revertedAssetTx);
        //revertedAssetTx.setAssetReversalTx(assetTransaction);
    }

    private static void connectToRelatedAdoptionAssetTx(final int legacyId, final HsOfficeCoopAssetsTransactionEntity assetTransaction) {
        final var negativeValue = assetTransaction.getAssetValue().negate();
        final var adoptionAssetTx = coopAssets.values().stream().filter(a ->
                        a.getTransactionType() == HsOfficeCoopAssetsTransactionType.ADOPTION &&
                                (!a.getValueDate().equals(LocalDate.of( 2014 , 12 , 31)) || a.getComment().contains(Integer.toString(assetTransaction.getMembership().getMemberNumber()/100))) &&
                                a.getMembership() != assetTransaction.getMembership() &&
                                a.getValueDate().equals(assetTransaction.getValueDate()) &&
                                a.getAssetValue().equals(negativeValue))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(
                        "cannot determine asset adoption entry for reversal " + assetTransaction));
        assetTransaction.setAdoptionAssetTx(adoptionAssetTx);
        //adoptionAssetTx.setAssetTransferTx(assetTransaction);
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

                    if (this.IGNORE_BUSINESS_PARTNERS.contains(rec.getInteger("bp_id"))) {
                        return;
                    }

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

                    if (IGNORE_CONTACTS.contains(contactId)) {
                        return;
                    }
                    if (IGNORE_BUSINESS_PARTNERS.contains(bpId)) {
                        return;
                    }

                    if (rec.getString("roles").isBlank()) {
                        fail("empty roles assignment not allowed for contact_id: " + contactId);
                    }

                    final var partner = partners.get(bpId);
                    final var debitor = debitors.get(bpId);

                    final var partnerPerson = partner.getPartnerRel().getHolder();
                    if (containsPartnerRel(rec)) {
                        addPerson(partnerPerson, rec);
                    }

                    HsOfficePersonRealEntity contactPerson = partnerPerson;
                    if (!StringUtils.equals(rec.getString("firma"), partnerPerson.getTradeName()) ||
                            partnerPerson.getPersonType() != determinePersonType(rec) ||
                            !StringUtils.equals(rec.getString("title"), partnerPerson.getTitle()) ||
                            !StringUtils.equals(rec.getString("salut"), partnerPerson.getSalutation()) ||
                            !StringUtils.equals(rec.getString("first_name"), partnerPerson.getGivenName()) ||
                            !StringUtils.equals(rec.getString("last_name"), partnerPerson.getFamilyName())) {
                        contactPerson = addPerson(HsOfficePersonRealEntity.builder().build(), rec);
                    }

                    final var contact = HsOfficeContactRealEntity.builder().build();
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
                        addRelation(HsOfficeRelationType.OPERATIONS_ALERT, partnerPerson, contactPerson, contact);
                        addRelation(HsOfficeRelationType.OPERATIONS, partnerPerson, contactPerson, contact);
                    }
                    if (containsRole(rec, "silent")) {
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
                    for (String subscriberRole : SUBSCRIBER_ROLES) {
                        if (containsRole(rec, subscriberRole)) {
                            addRelation(HsOfficeRelationType.SUBSCRIBER, partnerPerson, contactPerson, contact)
                                    .setMark(subscriberRole.split(":")[1])
                            ;
                        }
                    }
                    verifyContainsOnlyKnownRoles(rec.getString("roles"));
                });

        assertNoMissingContractualRelations();
        useHostsharingAsPartnerAnchor();
    }

    private static void assertNoMissingContractualRelations() {
        final var contractualMissing = new HashSet<Integer>();
        partners.forEach((id, partner) -> {
            final var partnerPerson = partner.getPartnerRel().getHolder();
            if (relations.values().stream()
                    .filter(rel -> rel.getAnchor() == partnerPerson && rel.getType() == HsOfficeRelationType.REPRESENTATIVE)
                    .findFirst().isEmpty()) {
                contractualMissing.add(partner.getPartnerNumber());
            }
        });
        if (isImportingControlledTestData()) {
            assertThat(contractualMissing).containsOnly(19999); // deliberately wrong partner entry
        } else {
            assertThat(contractualMissing).as("partners without contractual contact found").isEmpty();
        }
    }

    private static void useHostsharingAsPartnerAnchor() {
        final var mandant = persons.values().stream()
                .filter(p -> p.getTradeName().startsWith("Hostsharing e"))
                .findFirst()
                .orElseThrow();
        relations.values().stream()
                .filter(r -> r.getType() == HsOfficeRelationType.PARTNER)
                .forEach(r -> r.setAnchor(mandant));
    }

    private static boolean containsRole(final Record rec, final String role) {
        final var roles = rec.getString("roles");
        return ("," + roles + ",").contains("," + role + ",");
    }

    private static boolean containsPartnerRel(final Record rec) {
        return containsRole(rec, "partner");
    }

    private static HsOfficeRelationRealEntity addRelation(
            final HsOfficeRelationType type,
            final HsOfficePersonRealEntity anchor,
            final HsOfficePersonRealEntity holder,
            final HsOfficeContactRealEntity contact) {
        final var rel = HsOfficeRelationRealEntity.builder()
                .anchor(anchor)
                .holder(holder)
                .contact(contact)
                .type(type)
                .build();
        relations.put(relationId++, rel);
        return rel;
    }

    private HsOfficePersonRealEntity addPerson(final HsOfficePersonRealEntity person, final Record contactRecord) {
        person.setSalutation(contactRecord.getString("salut"));
        person.setTitle(contactRecord.getString("title"));
        person.setGivenName(contactRecord.getString("first_name"));
        person.setFamilyName(contactRecord.getString("last_name"));
        person.setTradeName(contactRecord.getString("firma"));
        person.setPersonType(determinePersonType(contactRecord));

        persons.put(contactRecord.getInteger("contact_id"), person);
        return person;
    }

    private static HsOfficePersonType determinePersonType(final Record contactRecord) {
        String roles = contactRecord.getString("roles");
        String country = contactRecord.getString("country");
        String familyName = contactRecord.getString("last_name");
        String givenName = contactRecord.getString("first_name");
        String tradeName = contactRecord.getString("firma");

        if (PERSON_TYPES_BY_CONTACT.containsKey(contactRecord.getInteger("contact_id"))) {
            return PERSON_TYPES_BY_CONTACT.get(contactRecord.getInteger("contact_id"));
        }

        if (tradeName.isBlank() || tradeName.startsWith("verstorben")) {
            return HsOfficePersonType.NATURAL_PERSON;
        } else
            // contractual && !partner with a firm and a natural person name
            // should actually be split up into two persons
            // but the legacy database consists such records

            if (endsWithWord(tradeName, "OHG", "GbR", "KG", "UG", "PartGmbB", "mbB")) {
                return HsOfficePersonType.INCORPORATED_FIRM; // Personengesellschaft. Gesellschafter haften persönlich.
            } else if (containsWord(tradeName, "e.K.", "e.G.", "eG", "gGmbH", "GmbH", "mbH", "AG", "e.V.", "eV", "e.V")
                || tradeName.toLowerCase().contains("haftungsbeschränkt")
                || tradeName.toLowerCase().contains("stiftung")
                || tradeName.toLowerCase().contains("stichting")
                || tradeName.toLowerCase().contains("foundation")
                || tradeName.toLowerCase().contains("schule")
                || tradeName.toLowerCase().contains("verein")
                || tradeName.toLowerCase().contains("gewerkschaft")
                || tradeName.toLowerCase().contains("gesellschaft")
                || tradeName.toLowerCase().contains("kirche")
                || tradeName.toLowerCase().contains("fraktion")
                || tradeName.toLowerCase().contains("landkreis")
                || tradeName.toLowerCase().contains("behörde")
                || tradeName.toLowerCase().contains("bundesamt")
                || tradeName.toLowerCase().contains("bezirksamt")
                ) {
                return HsOfficePersonType.LEGAL_PERSON; // Haftungsbeschränkt
            } else if (roles.contains("contractual") && !roles.contains("partner") &&
                   !familyName.isBlank() && !givenName.isBlank()) {
                // REPRESENTATIVES are always natural persons
                return HsOfficePersonType.NATURAL_PERSON;
            } else {
                return HsOfficePersonType.UNKNOWN_PERSON_TYPE;
            }
    }

    private static boolean endsWithWord(final String value, final String... endings) {
        final var lowerCaseValue = value.toLowerCase();
        for (String ending : endings) {
            if (lowerCaseValue.endsWith(" " + ending.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWord(final String value, final String... endings) {
        final var lowerCaseValue = value.toLowerCase();
        for (String ending : endings) {
            if (lowerCaseValue.equals(ending.toLowerCase()) ||
                lowerCaseValue.startsWith(ending.toLowerCase() + " ") ||
                lowerCaseValue.contains(" " + ending.toLowerCase() + " ") ||
                lowerCaseValue.endsWith(" " + ending.toLowerCase())) {
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

    private HsOfficeContactRealEntity initContact(final HsOfficeContactRealEntity contact, final Record contactRecord) {

        contact.setCaption(toCaption(
                contactRecord.getString("salut"),
                contactRecord.getString("title"),
                contactRecord.getString("first_name"),
                contactRecord.getString("last_name"),
                contactRecord.getString("firma")));
        contact.putEmailAddresses(Map.of("main", contactRecord.getString("email")));
        contact.putPostalAddress(toAddress(contactRecord));
        contact.putPhoneNumbers(toPhoneNumbers(contactRecord));

        contacts.put(contactRecord.getInteger("contact_id"), contact);
        return contact;
    }

    private Map<String, String> toPhoneNumbers(final Record rec) {
        final var phoneNumbers = new LinkedHashMap<String, String>();
        if (isNotBlank(rec.getString("phone_private")))
            phoneNumbers.put("phone_private", rec.getString("phone_private"));
        if (isNotBlank(rec.getString("phone_office")))
            phoneNumbers.put("phone_office", rec.getString("phone_office"));
        if (isNotBlank(rec.getString("phone_mobile")))
            phoneNumbers.put("phone_mobile", rec.getString("phone_mobile"));
        if (isNotBlank(rec.getString("fax")))
            phoneNumbers.put("fax", rec.getString("fax"));
        return phoneNumbers;
    }

    private Map<String, String> toAddress(final Record rec) {
        final var result = new LinkedHashMap<String, String>();
        final var name = toName(
                rec.getString("salut"),
                rec.getString("title"),
                rec.getString("first_name"),
                rec.getString("last_name"));
        if (isNotBlank(name))
            result.put("name", name);
        if (isNotBlank(rec.getString("firma")))
            result.put("firm", name);

        List.of("co", "street", "zipcode", "city", "country").forEach(key -> {
            if (isNotBlank(rec.getString(key)))
                result.put(key, rec.getString(key));
        });
        return result;
    }

    private String toCaption(
            final String salut,
            final String title,
            final String firstname,
            final String lastname,
            final String firm) {
        final var result = new StringBuilder();
        if (isNotBlank(salut))
            result.append((isBlank(result) ? "" : " ") + salut);
        if (isNotBlank(title))
            result.append((isBlank(result) ? "" : " ") + title);
        if (isNotBlank(firstname))
            result.append((isBlank(result) ? "" : " ") + firstname);
        if (isNotBlank(lastname))
            result.append((isBlank(result) ? "" : " ") + lastname);
        if (isNotBlank(firm)) {
            result.append((isBlank(result) ? "" : ", ") + firm);
        }
        return result.toString();
    }

    private String toName(final String salut, final String title, final String firstname, final String lastname) {
        return toCaption(salut, title, firstname, lastname, null);
    }
}
