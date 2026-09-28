package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import net.hostsharing.hsadminng.hs.accounts.scenarios.CreateGlobalAdminSubjectWithoutAccount;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.DemoArchetype.PersonType;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.DemoDataInvariants.GeneratedCustomer;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.DemoValueGenerator.PostalAddress;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.CreateMembership;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsDepositTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares.CreateCoopSharesSubscriptionTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.CreatePartner;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Generates a demo dataset of the size configured via {@value CUSTOMER_COUNT_PROPERTY}.
 * The concept and the reasons behind the reserved number ranges are documented in the PR#270 document.
 */
public class GenerateDemoDataset extends UseCase<GenerateDemoDataset> {

    /** Who a customer is, drawn before anything is created, so that the creation steps stop asking. */
    private record DrawnCustomer(
            String partnerNumber,
            DemoArchetype archetype,
            DemoValueGenerator.Person holder,
            String tradeName,
            LocalDate birthday,
            PostalAddress address,
            DemoPersonSet.FixedCustomer fixedCustomer,
            String ownerFamilyName) {

        boolean isNaturalPerson() {
            return archetype.personType() == PersonType.NATURAL_PERSON;
        }

        boolean isFromFixedPersonSet() {
            return fixedCustomer != null;
        }

        String partnerName() {
            return isNaturalPerson() ? holder.fullName() : tradeName;
        }

        String contactCaption() {
            return GenerateDemoDataset.contactCaptionOf(partnerName(), MAIN_CONTACT_PURPOSE, partnerNumber);
        }
    }

    /** A person together with the contact created for it, which its subscriptions reuse. */
    private record CustomerPerson(DemoValueGenerator.Person person, String contactCaption) {}

    /** The persons of one customer, kept apart by which mailing lists they can reach. */
    private record CustomerPersons(
            List<CustomerPerson> customerPersons,
            List<CustomerPerson> operationsContacts) {}

    /** The office relation types the generator creates, each with what its contact is there for. */
    private enum DemoRelationType {

        REPRESENTATIVE("Vertretung"),
        OPERATIONS("Technik"),
        OPERATIONS_ALERT("Alarm");

        private final String contactPurpose;

        DemoRelationType(final String contactPurpose) {
            this.contactPurpose = contactPurpose;
        }

        String contactPurpose() {
            return contactPurpose;
        }
    }

    /** {@link #NONE} spares every caller a null check, because most archetypes have no membership. */
    private record DemoMembership(LocalDate joiningDate, Integer shareCount, LocalDate depositDate) {

        static final DemoMembership NONE = new DemoMembership(null, null, null);

        boolean exists() {
            return joiningDate != null;
        }

        Integer assetValue() {
            return shareCount == null ? null : shareCount * SHARE_VALUE_IN_EUR;
        }
    }

    public static final String CUSTOMER_COUNT_PROPERTY = "hsadminng.demodata.customerCount";
    public static final String SEED_PROPERTY = "hsadminng.demodata.seed";

    static final int DEFAULT_CUSTOMER_COUNT = 5;
    static final long DEFAULT_SEED = 20260805L;

    public static final String DEMO_ADMIN_SUBJECT_NAME = "hsh-demo_admin";
    // real subject UUIDs come from Keycloak; a generated dataset has none, so this one is just fixed
    static final String DEMO_ADMIN_SUBJECT_UUID = "de300000-0000-0000-0000-000000000001";

    // reserved block, far above production partner numbers and above the other scenario test-data
    static final int FIRST_PARTNER_NUMBER = 90000;

    static final String MEMBER_NUMBER_SUFFIX = "00";

    private static final Set<String> PREFIXES_USED_BY_OTHER_TEST_DATA =
            Set.of("abc", "fir", "for", "mim", "sec", "thi", "tst", "tsx", "tsy", "tsz");
    static final int SHARE_VALUE_IN_EUR = 64;

    // hs_office.bankaccount.holder is varchar(64); an overflow surfaces as HTTP 409, not as a validation error
    static final int BANKACCOUNT_HOLDER_MAX_LENGTH = 64;
    // hs_office.contact.caption is varchar(128), and overflows the same way
    static final int CONTACT_CAPTION_MAX_LENGTH = 128;

    // a partner holds several contacts which differ in purpose only, and the caption is what a list shows
    static final String MAIN_CONTACT_PURPOSE = "Hauptkontakt";
    static final String BILLING_CONTACT_PURPOSE = "Rechnungsstelle";
    static final String MAILING_LIST_CONTACT_PURPOSE = "private Mailinglisten-Adresse";

    // a customer usually delegates one person to a list, now and then a second one
    static final int SECOND_SUBSCRIBER_PERCENTAGE = 22;

    // admins beyond those a customer named itself, expressed per customer
    static final int EXTRA_OPERATIONS_SUBSCRIBER_PERCENTAGE = 35;
    static final int EXTRA_OPERATIONS_ALERT_PERCENTAGE = 20;
    static final int EXTRA_ON_OPERATIONS_ANNOUNCE_PERCENTAGE = 92;
    static final int EXTRA_ON_OPERATIONS_DISCUSSION_PERCENTAGE = 45;

    // most subscribers use a private address for their lists, not the contact of their relation
    static final int PRIVATE_LIST_ADDRESS_PERCENTAGE = 75;
    // RFC 2606 reserves it, and a different domain than the customer's marks it as a private address
    static final String PRIVATE_LIST_ADDRESS_DOMAIN = "example.net";

    static final int NO_SEPA_MANDATE_PERCENTAGE = 8;
    static final int REPLACED_SEPA_MANDATE_PERCENTAGE = 8;

    private final int customerCount;
    private final List<DemoPersonSet.FixedPerson> fixedPersons;
    private final List<DemoPersonSet.FixedCustomer> fixedCustomers;
    private final Set<String> reservedPrefixes;
    private final DemoValueGenerator values;

    private final List<GeneratedCustomer> generated = new ArrayList<>();
    private final Map<String, String> personUuidByHumanKey = new LinkedHashMap<>();

    private String lastContactCaption;
    private int nextPrefixOrdinal;

    public GenerateDemoDataset(
            final ScenarioTest testSuite,
            final int customerCount,
            final long seed,
            final List<DemoPersonSet.FixedPerson> fixedPersons) {
        super(testSuite);
        this.customerCount = customerCount;
        this.fixedPersons = fixedPersons;
        this.fixedCustomers = DemoPersonSet.customersOf(fixedPersons);
        this.reservedPrefixes = Stream.concat(
                        PREFIXES_USED_BY_OTHER_TEST_DATA.stream(),
                        fixedCustomers.stream().map(DemoPersonSet.FixedCustomer::defaultPrefix).filter(Objects::nonNull))
                .collect(Collectors.toUnmodifiableSet());
        // their names must not be drawn as well, or the by-name lookup of the account tooling becomes ambiguous
        this.values = new DemoValueGenerator(seed, DemoPersonSet.reservedNamesOf(fixedPersons));
    }

    public static int configuredCustomerCount() {
        return Integer.getInteger(CUSTOMER_COUNT_PROPERTY, DEFAULT_CUSTOMER_COUNT);
    }

    public static long configuredSeed() {
        return Long.getLong(SEED_PROPERTY, DEFAULT_SEED);
    }

    @Override
    protected HttpResponse run() {
        createMandantPerson();
        createDemoAdminSubject();

        // beyond the minimal dataset a per-request report would grow into the hundred thousands of lines
        if (customerCount > DEFAULT_CUSTOMER_COUNT) {
            silently(this::createAllCustomers);
        } else {
            createAllCustomers();
        }

        writePersonUuids();

        final var contactCaption = lastContactCaption;
        return withTitle("Query the most recently generated demo partner", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/relations?relationType=PARTNER&contactData="
                        + uriEncoded(contactCaption))
                        .expecting(OK).expecting(JSON));
    }

    /**
     * A login for the demo itself: an API-key cannot be used from the frontend, and without the
     * test-data changesets there is no other USER subject to log in as.
     */
    private void createDemoAdminSubject() {
        new CreateGlobalAdminSubjectWithoutAccount(testSuite, asGlobalAgent())
                .given("subjectUuid", DEMO_ADMIN_SUBJECT_UUID)
                .given("subjectName", DEMO_ADMIN_SUBJECT_NAME)
                .thenExpect(CREATED);
    }

    /** The anchor of every PARTNER relation; in production it is real data, here it has to be generated. */
    private void createMandantPerson() {
        withTitle("Create the mandant person Hostsharing eG", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/persons", usingJsonBody("""
                        {
                            "personType": "LEGAL_PERSON",
                            "tradeName": "Hostsharing eG"
                        }
                        """))
                        .expecting(CREATED).expecting(JSON));
    }

    private void createAllCustomers() {
        // the showcase cooperative comes first, so that it exists even in the minimal CI dataset
        lastContactCaption = createCustomer(0, DemoArchetype.ASSOCIATION, DemoArchetype.CURATED_LEGAL_FORM, null);

        int index = 1;
        for (val fixedCustomer : fixedCustomers) {
            val archetype = archetypeOf(fixedCustomer);
            lastContactCaption = createCustomer(index++, archetype, archetype.legalForm(), fixedCustomer);
        }
        addFixedPersonsToCooperative();

        for (; index < customerCount + fixedCustomers.size(); index++) {
            val archetype = values.pick(DemoArchetype.weighted());
            lastContactCaption = createCustomer(index, archetype, archetype.legalForm(), null);
        }

        addExtraOperationsSubscribers();
    }

    /**
     * Admins who reach the operations lists without their customer having named them in the office data;
     * they get the relation they were missing, which is what a real subscriber list uncovers.
     */
    private void addExtraOperationsSubscribers() {
        val count = generated.size() * EXTRA_OPERATIONS_SUBSCRIBER_PERCENTAGE / 100;
        for (int n = 0; n < count; n++) {
            val customer = values.oneOf(generated);
            val person = values.uniquePerson();
            val relationType = values.percentageChance(EXTRA_OPERATIONS_ALERT_PERCENTAGE)
                    ? DemoRelationType.OPERATIONS_ALERT
                    : DemoRelationType.OPERATIONS;
            val subscriber = new CustomerPerson(person,
                    addRelation(customer.partnerName(), customer.partnerNumber(), relationType, person));

            if (values.percentageChance(EXTRA_ON_OPERATIONS_ANNOUNCE_PERCENTAGE)) {
                subscribe(customer.partnerName(), customer.partnerNumber(), subscriber,
                        DemoMailingList.OPERATIONS_ANNOUNCE);
            }
            if (values.percentageChance(EXTRA_ON_OPERATIONS_DISCUSSION_PERCENTAGE)) {
                subscribe(customer.partnerName(), customer.partnerNumber(), subscriber,
                        DemoMailingList.OPERATIONS_DISCUSSION);
            }
        }
    }

    /** A member prefix denotes a private customer, any longer login prefix an organisation. */
    private DemoArchetype archetypeOf(final DemoPersonSet.FixedCustomer fixedCustomer) {
        return fixedCustomer.isNaturalPerson() ? DemoArchetype.FREELANCER : DemoArchetype.SMALL_COMPANY;
    }

    /**
     * The fixed persons are members of the showcase cooperative besides being customers of their own,
     * which is what makes them findable by name in the member view of the demo.
     */
    private void addFixedPersonsToCooperative() {
        val cooperative = generated.get(0);
        fixedCustomers.stream().flatMap(fixedCustomer -> fixedCustomer.persons().stream())
                .forEach(fixedPerson -> addRelation(
                        cooperative.partnerName(), cooperative.partnerNumber(), DemoRelationType.REPRESENTATIVE,
                        values.personNamed(fixedPerson.givenName(), fixedPerson.familyName())));
    }

    /**
     * Both logins of one human share the person, so that the staff login of the account tooling
     * ends up on the very person which the member login already has.
     */
    private void writePersonUuids() {
        val csvFile = DemoPersonSet.configuredCsvPath();
        if (csvFile != null) {
            DemoPersonSet.writeWithPersonUuids(
                    DemoPersonSet.outputPathFor(csvFile), fixedPersons, personUuidByHumanKey);
        }
    }

    /** What the account tooling needs after the restore is already known here: the uuid of every fixed person. */
    private void rememberPersonUuids(final DemoPersonSet.FixedCustomer fixedCustomer) {
        fixedCustomer.persons().forEach(fixedPerson -> personUuidByHumanKey.put(
                fixedPerson.humanKey(), uuid("Person: " + fixedPerson.fullName()).toString()));
    }

    @Override
    protected void verify(final UseCase<GenerateDemoDataset>.HttpResponse response) {
        response.expectArrayElements(1);

        DemoDataInvariants.assertAllHold(generated, DemoPersonSet.fullNamesOf(fixedPersons));

        verify(
                "Verify the Showcase Cooperative Has Exactly One Membership",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/memberships?partnerNumber=P-" + FIRST_PARTNER_NUMBER)
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
        );
    }

    private String createCustomer(
            final int index,
            final DemoArchetype archetype,
            final String legalForm,
            final DemoPersonSet.FixedCustomer fixedCustomer) {
        val customer = drawCustomer(index, archetype, legalForm, fixedCustomer);

        createPartner(customer);
        val persons = addPersonRelations(customer);
        val defaultPrefixes = addDebitors(customer);
        val membership = addMembership(customer);
        val mailingLists = addSubscriptions(customer, persons, membership.exists());

        remember(customer, defaultPrefixes, membership, mailingLists);

        return customer.contactCaption();
    }

    /** The order of the draws belongs to the seed: swapping two of them yields a different dataset. */
    private DrawnCustomer drawCustomer(
            final int index,
            final DemoArchetype archetype,
            final String legalForm,
            final DemoPersonSet.FixedCustomer fixedCustomer) {
        val naturalPerson = archetype.personType() == PersonType.NATURAL_PERSON;
        val holder = naturalPerson && fixedCustomer != null
                ? values.personNamed(fixedCustomer.holder().givenName(), fixedCustomer.holder().familyName())
                : values.uniquePerson();
        val ownerManaged = drawOwnerManagedTradeName(archetype, legalForm, fixedCustomer, naturalPerson);
        val tradeName = tradeNameFor(naturalPerson, legalForm, fixedCustomer, ownerManaged);
        val address = values.postalAddress();
        val birthday = naturalPerson ? values.birthday(25, 75, LocalDate.of(2026, 1, 1)) : null;

        return new DrawnCustomer(
                "P-" + (FIRST_PARTNER_NUMBER + index),
                archetype,
                holder,
                tradeName,
                birthday,
                address,
                fixedCustomer,
                ownerManaged == null ? null : ownerManaged.ownerFamilyName());
    }

    /** An owner-managed firm is named after its owner, who then appears as a representative of the same surname. */
    private DemoValueGenerator.OwnerManagedTradeName drawOwnerManagedTradeName(
            final DemoArchetype archetype,
            final String legalForm,
            final DemoPersonSet.FixedCustomer fixedCustomer,
            final boolean naturalPerson) {
        if (fixedCustomer != null || naturalPerson || !archetype.mayBeOwnerManaged() || !values.isOwnerManaged()) {
            return null;
        }
        return values.uniqueOwnerManagedTradeName(legalForm != null ? legalForm : values.oneOfLegalForms());
    }

    private CustomerPersons addPersonRelations(final DrawnCustomer customer) {
        val customerPersons = new ArrayList<CustomerPerson>();
        val operationsContacts = new ArrayList<CustomerPerson>();

        if (customer.isNaturalPerson()) {
            customerPersons.add(new CustomerPerson(customer.holder(), customer.contactCaption()));
            if (customer.isFromFixedPersonSet()) {
                customerPersons.addAll(addFixedRepresentatives(customer));
            }
            return new CustomerPersons(customerPersons, operationsContacts);
        }

        customerPersons.addAll(customer.isFromFixedPersonSet()
                ? addFixedRepresentatives(customer)
                : addDrawnRepresentatives(customer));

        for (int n = 0; n < customer.archetype().operationsContacts(values); n++) {
            operationsContacts.add(
                    addRelationTo(customer, DemoRelationType.OPERATIONS, values.uniquePerson()));
        }

        return new CustomerPersons(customerPersons, operationsContacts);
    }

    private List<CustomerPerson> addDrawnRepresentatives(final DrawnCustomer customer) {
        val ownerFamilyName = customer.ownerFamilyName();
        val count = Math.max(customer.archetype().representatives(values), ownerFamilyName != null ? 1 : 0);

        val representatives = new ArrayList<CustomerPerson>();
        for (int n = 0; n < count; n++) {
            val representative = ownerFamilyName != null && n == 0
                    ? values.uniquePersonWithFamilyName(ownerFamilyName)
                    : values.uniquePerson();
            representatives.add(addRelationTo(customer, DemoRelationType.REPRESENTATIVE, representative));
        }
        return representatives;
    }

    private CustomerPerson addRelationTo(
            final DrawnCustomer customer,
            final DemoRelationType relationType,
            final DemoValueGenerator.Person person) {
        return new CustomerPerson(
                person,
                addRelation(customer.partnerName(), customer.partnerNumber(), relationType, person));
    }

    private List<String> addDebitors(final DrawnCustomer customer) {
        val defaultPrefixes = new ArrayList<String>();
        for (int n = 0; n < customer.archetype().debitors(values); n++) {
            val fixedPrefix = n == 0 && customer.isFromFixedPersonSet()
                    ? customer.fixedCustomer().defaultPrefix()
                    : null;
            defaultPrefixes.add(createDebitor(customer.partnerName(), customer.partnerNumber(), n, fixedPrefix));
        }
        return defaultPrefixes;
    }

    private DemoMembership addMembership(final DrawnCustomer customer) {
        if (!customer.archetype().withMembership()) {
            return DemoMembership.NONE;
        }
        val joiningDate = values.joiningDate(2000, 2025);
        createMembership(customer.partnerNumber(), customer.partnerName(), joiningDate);
        return subscribeShares(customer.partnerNumber(), joiningDate);
    }

    private void remember(
            final DrawnCustomer customer,
            final List<String> defaultPrefixes,
            final DemoMembership membership,
            final Set<DemoMailingList> mailingLists) {
        generated.add(new GeneratedCustomer(
                customer.partnerNumber(), customer.archetype(), customer.isFromFixedPersonSet(), mailingLists,
                customer.partnerName(), customer.tradeName(), customer.holder(), customer.birthday(), defaultPrefixes,
                membership.joiningDate(), membership.shareCount(), membership.assetValue(), membership.depositDate()));

        if (customer.isFromFixedPersonSet()) {
            rememberPersonUuids(customer.fixedCustomer());
        }
    }

    private String tradeNameFor(
            final boolean naturalPerson,
            final String legalForm,
            final DemoPersonSet.FixedCustomer fixedCustomer,
            final DemoValueGenerator.OwnerManagedTradeName ownerManaged) {
        if (naturalPerson) {
            return null;
        }
        if (fixedCustomer != null) {
            return fixedCustomer.tradeNameCore() + " " + legalForm;
        }
        if (ownerManaged != null) {
            return ownerManaged.tradeName();
        }
        return legalForm != null ? values.uniqueTradeName(legalForm) : values.uniqueTradeName();
    }

    /** Everybody behind a login prefix belongs to its customer, except the private customer who is that person. */
    private List<CustomerPerson> addFixedRepresentatives(final DrawnCustomer customer) {
        return customer.fixedCustomer().persons().stream()
                .skip(customer.isNaturalPerson() ? 1 : 0)
                .map(fixedPerson -> addRelationTo(customer, DemoRelationType.REPRESENTATIVE,
                        values.personNamed(fixedPerson.givenName(), fixedPerson.familyName())))
                .toList();
    }

    /**
     * A customer takes part in a list as a whole and delegates one or two people to it, because a
     * member appears on the list of its general assembly once, not once per representative.
     */
    private Set<DemoMailingList> addSubscriptions(
            final DrawnCustomer customer,
            final CustomerPersons persons,
            final boolean withMembership) {
        val subscribed = new LinkedHashSet<DemoMailingList>();

        DemoMailingList.openTo(withMembership)
                .filter(list -> values.percentageChance(list.customerParticipationPercentage()))
                .forEach(list -> {
                    // the technical lists reach the operations contacts, and the owner where there is none
                    val pool = list.technical() && !persons.operationsContacts().isEmpty()
                            ? persons.operationsContacts()
                            : persons.customerPersons();
                    val delegates = delegatesOf(pool);
                    delegates.forEach(delegate ->
                            subscribe(customer.partnerName(), customer.partnerNumber(), delegate, list));
                    if (!delegates.isEmpty()) {
                        subscribed.add(list);
                    }
                });

        return subscribed;
    }

    /** Usually one person of a customer subscribes, now and then a second one. */
    private List<CustomerPerson> delegatesOf(final List<CustomerPerson> pool) {
        if (pool.isEmpty()) {
            return List.of();
        }
        val first = values.oneOf(pool);
        if (pool.size() == 1 || !values.percentageChance(SECOND_SUBSCRIBER_PERCENTAGE)) {
            return List.of(first);
        }
        val second = values.oneOf(pool.stream().filter(candidate -> candidate != first).toList());
        return List.of(first, second);
    }

    private void subscribe(
            final String partnerName,
            final String partnerNumber,
            final CustomerPerson delegate,
            final DemoMailingList list) {
        val person = delegate.person();
        // an admin reads a technical list at work, thus under the domain of the customer, never privately
        val contactCaption = !list.technical() && values.percentageChance(PRIVATE_LIST_ADDRESS_PERCENTAGE)
                ? createPrivateListContact(person)
                : delegate.contactCaption();

        new AddDemoSubscription(testSuite,
                "SUBSCRIBER " + list.mark() + ": " + person.fullName() + " (" + partnerNumber + ")")
                .given("partnerPersonName", partnerName)
                .given("mailingList", list.mark())
                .given("holderGivenName", person.givenName())
                .given("holderFamilyName", person.familyName())
                .given("contactCaption", contactCaption)
                .thenExpect(HttpStatus.OK);
    }

    /** One private contact per person, reused for all of its lists, as a real subscriber would do. */
    private String createPrivateListContact(final DemoValueGenerator.Person person) {
        val caption = contactCaptionOf(person.fullName(), MAILING_LIST_CONTACT_PURPOSE);
        val address = values.postalAddress();

        obtainOnce("Contact: " + caption, () ->
                httpPost(asGlobalAgent(), "/api/hs/office/contacts", usingJsonBody("""
                        {
                            "caption": "%s",
                            "postalAddress": {
                                %s
                            },
                            "phoneNumbers": { "main": "%s" },
                            "emailAddresses": { "main": "%s" }
                        }
                        """.formatted(
                        caption,
                        postalAddressJson(address),
                        values.phoneNumber(address.location()),
                        values.emailAddress(person.fullName(), PRIVATE_LIST_ADDRESS_DOMAIN))))
                        .expecting(CREATED).expecting(JSON));
        return caption;
    }

    private void createPartner(final DrawnCustomer customer) {
        val holder = customer.holder();
        val address = customer.address();
        val domain = values.domainOf(customer.partnerName());

        // optional properties must be left unset rather than given as null, which the template resolver rejects
        val partner = new CreatePartner(
                testSuite, "Partner: " + customer.partnerNumber() + " - " + customer.partnerName())
                .given("partnerNumber", customer.partnerNumber())
                .given("personType", customer.archetype().personType().name())
                .given("contactCaption", customer.contactCaption())
                .given("postalAddress", postalAddressJson(address))
                .given("officePhoneNumber", values.phoneNumber(address.location()))
                .given("emailAddress", values.emailAddress(customer.isNaturalPerson() ? "post" : "info", domain));

        if (customer.isNaturalPerson()) {
            partner.given("givenName", holder.givenName())
                    .given("familyName", holder.familyName())
                    .given("birthday", customer.birthday().toString());
        } else {
            partner.given("tradeName", customer.tradeName());
        }
        if (customer.archetype().hasRegistrationData()) {
            partner.given("registrationOffice", "Registergericht " + address.location().city())
                    .given("registrationNumber", String.valueOf(values.intBetween(100000, 999999)));
        }

        partner.thenExpect(HttpStatus.OK);
    }

    private String addRelation(
            final String partnerPersonName,
            final String partnerNumber,
            final DemoRelationType relationType,
            final DemoValueGenerator.Person holder) {
        val address = values.postalAddress();
        val contactCaption = contactCaptionOf(holder.fullName(), relationType.contactPurpose(), partnerNumber);

        new AddDemoRelation(testSuite, relationType + ": " + contactCaption)
                .given("partnerPersonName", partnerPersonName)
                .given("relationType", relationType.name())
                .given("holderGivenName", holder.givenName())
                .given("holderFamilyName", holder.familyName())
                .given("contactCaption", contactCaption)
                .given("postalAddress", postalAddressJson(address))
                .given("phoneNumber", values.phoneNumber(address.location()))
                .given("emailAddress", values.emailAddress(holder.fullName(), values.domainOf(partnerPersonName)))
                .thenExpect(HttpStatus.OK);

        return contactCaption;
    }

    private String createDebitor(
            final String partnerPersonName,
            final String partnerNumber,
            final int debitorIndex,
            final String fixedPrefix) {
        val debitorNumber = debitorNumberOf(partnerNumber, debitorIndex);
        val billingContactCaption =
                contactCaptionOf(partnerPersonName, BILLING_CONTACT_PURPOSE, debitorNumber);
        val prefix = fixedPrefix != null ? fixedPrefix : nextDefaultPrefix();

        new CreateDemoDebitor(testSuite, "Debitor: " + billingContactCaption)
                .given("partnerPersonName", partnerPersonName)
                .given("debitorNumberSuffix", String.format("%02d", debitorIndex))
                .given("billable", true)
                .given("vatBusiness", values.percentageChance(80))
                .given("defaultPrefix", prefix)
                .given("billingContactCaption", billingContactCaption)
                .given("billingContactEmailAddress",
                        values.emailAddress("rechnung" + debitorIndex, values.domainOf(partnerPersonName)))
                .thenExpect(HttpStatus.OK);

        addSepaMandates(partnerPersonName, partnerNumber, debitorNumber, debitorIndex);

        return prefix;
    }

    private static String debitorNumberOf(final String partnerNumber, final int debitorIndex) {
        return "D-" + partnerNumber.substring("P-".length()) + String.format("%02d", debitorIndex);
    }

    /**
     * Most debitors pay by direct debit, a few never did, and a few changed their bank at some point,
     * which leaves an expired mandate next to the current one.
     */
    private void addSepaMandates(
            final String partnerPersonName,
            final String partnerNumber,
            final String debitorNumber,
            final int debitorIndex) {
        if (values.percentageChance(NO_SEPA_MANDATE_PERCENTAGE)) {
            return;
        }

        val currentValidFrom = values.joiningDate(2005, 2024);

        // a replaced mandate ends the day before the current one starts, as it does after a change of bank
        if (values.percentageChance(REPLACED_SEPA_MANDATE_PERCENTAGE)) {
            createSepaMandate(partnerPersonName, partnerNumber, debitorNumber, debitorIndex, "alt",
                    currentValidFrom.minusYears(values.intBetween(3, 12)), currentValidFrom.minusDays(1));
        }

        createSepaMandate(partnerPersonName, partnerNumber, debitorNumber, debitorIndex,
                "aktuell", currentValidFrom, null);
    }

    private void createSepaMandate(
            final String partnerPersonName,
            final String partnerNumber,
            final String debitorNumber,
            final int debitorIndex,
            final String label,
            final LocalDate validFrom,
            final LocalDate validTo) {
        val bankAccount = values.bankAccount(partnerPersonName);
        val reference = debitorNumber + "-" + label;

        val mandate = new CreateDemoSepaMandate(testSuite, "SEPA-Mandat: " + reference)
                .given("debitorNumber", debitorNumber)
                .given("mandateReference", reference)
                .given("bankAccountHolder", bankAccountHolder(
                        bankAccount.holder(), " (" + partnerNumber + "/" + debitorIndex + "/" + label + ")"))
                .given("iban", bankAccount.iban())
                .given("bic", bankAccount.bic())
                .given("agreement", validFrom.minusDays(values.intBetween(1, 20)).toString())
                .given("validFrom", validFrom.toString());
        if (validTo != null) {
            mandate.given("validTo", validTo.toString());
        }
        mandate.thenExpect(HttpStatus.OK);
    }

    static String bankAccountHolder(final String name, final String suffix) {
        return withinLength(name, suffix, BANKACCOUNT_HOLDER_MAX_LENGTH);
    }

    /** A private address belongs to the person rather than to a partner, thus it names no number. */
    static String contactCaptionOf(final String name, final String purpose) {
        return withinLength(name, " - " + purpose, CONTACT_CAPTION_MAX_LENGTH);
    }

    /** The number tells which partner or debitor the contact belongs to, the purpose what it is used for. */
    static String contactCaptionOf(final String name, final String purpose, final String number) {
        return contactCaptionOf(name, purpose + " (" + number + ")");
    }

    /** The suffix carries the identifying part, thus an overlong name gives way instead of it. */
    private static String withinLength(final String name, final String suffix, final int maxLength) {
        val room = maxLength - suffix.length();
        return (name.length() <= room ? name : name.substring(0, room).trim()) + suffix;
    }

    private String postalAddressJson(final PostalAddress address) {
        val nameSupplement = values.postalAddressNameSupplement();
        return (nameSupplement == null ? "" : "\"name\": \"" + nameSupplement + "\",\n")
                + """
                        "street": "%s",
                        "zipcode": "%s",
                        "city": "%s",
                        "country": "Germany"
                """.formatted(address.street(), address.location().zipcode(), address.location().city());
    }

    /**
     * Must match '^[a-z]{3}$' and stay globally unique, which rules out deriving it from the trade name.
     * A single leading letter would not do either: it yields 676 combinations for up to 750 debitors.
     */
    private String nextDefaultPrefix() {
        while (nextPrefixOrdinal < 26 * 26 * 26) {
            val ordinal = nextPrefixOrdinal++;
            val prefix = String.valueOf(new char[] {
                    (char) ('a' + (ordinal / (26 * 26)) % 26),
                    (char) ('a' + (ordinal / 26) % 26),
                    (char) ('a' + ordinal % 26) });
            if (!reservedPrefixes.contains(prefix)) {
                return prefix;
            }
        }
        throw new IllegalStateException("exhausted the debitor default prefixes");
    }

    private void createMembership(
            final String partnerNumber,
            final String partnerName,
            final LocalDate joiningDate) {
        val membershipFeeBillable = values.percentageChance(90);
        val createMembership = new CreateMembership(testSuite, "Membership: " + partnerName)
                .given("partnerNumber", partnerNumber)
                .given("validFrom", joiningDate.toString())
                .given("membershipFeeBillable", membershipFeeBillable);
        if (!membershipFeeBillable) {
            createMembership.given("membershipFeeComment", "exempt by resolution of the board");
        }
        createMembership.thenExpect(HttpStatus.OK);
    }

    /** Shares are subscribed on joining and paid in shortly after, never before the membership starts. */
    private DemoMembership subscribeShares(final String partnerNumber, final LocalDate joiningDate) {
        val memberNumber = "M-" + partnerNumber.substring("P-".length()) + MEMBER_NUMBER_SUFFIX;
        val shareCount = values.intBetween(1, 40);
        val depositDelayInDays = values.intBetween(0, 30);
        val depositDate = joiningDate.plusDays(depositDelayInDays);

        new CreateCoopSharesSubscriptionTransaction(testSuite)
                .given("memberNumber", memberNumber)
                .given("reference", "sign " + joiningDate)
                .given("shareCount", shareCount)
                .given("comment", "initial shares on joining")
                .given("transactionDate", joiningDate.toString())
                .thenExpect(HttpStatus.OK);

        new CreateCoopAssetsDepositTransaction(testSuite)
                .given("memberNumber", memberNumber)
                .given("reference", "sign " + joiningDate)
                .given("assetValue", shareCount * SHARE_VALUE_IN_EUR)
                .given("comment", "deposit for initial shares")
                .given("transactionDate", depositDate.toString())
                .thenExpect(HttpStatus.OK);

        return new DemoMembership(joiningDate, shareCount, depositDate);
    }
}
