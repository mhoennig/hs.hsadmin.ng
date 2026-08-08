package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import java.util.List;
import java.util.stream.Stream;

/**
 * Weighted customer profiles; without them 500 structurally identical partners are recognizable
 * as synthetic at a glance. The weights and characteristics are documented in the PR#270 document.
 */
public enum DemoArchetype {

    // weight, personType, legalForm (null = drawn), representatives, operationsContacts, membership
    FREELANCER(30, PersonType.NATURAL_PERSON, null, 0, 0, 0, 1, true),
    SMALL_COMPANY(25, PersonType.LEGAL_PERSON, "GmbH", 1, 2, 1, 2, true),
    ASSOCIATION(10, PersonType.LEGAL_PERSON, "e. V.", 2, 4, 0, 1, true),
    AGENCY(10, PersonType.LEGAL_PERSON, "GmbH", 1, 2, 1, 3, true),
    LARGER_COMPANY(8, PersonType.LEGAL_PERSON, "GmbH & Co. KG", 2, 4, 2, 5, true),
    FORMER_PARTNER(8, PersonType.LEGAL_PERSON, null, 0, 1, 0, 1, false),
    COMMUNITY_OF_HEIRS(4, PersonType.UNINCORPORATED_FIRM, null, 1, 3, 0, 1, true),
    PUBLIC_INSTITUTION(3, PersonType.PUBLIC_INSTITUTION, null, 1, 2, 1, 2, false),
    SPECIAL_CASE(2, PersonType.LEGAL_PERSON, null, 0, 1, 0, 1, false);

    public enum PersonType {
        NATURAL_PERSON, LEGAL_PERSON, UNINCORPORATED_FIRM, PUBLIC_INSTITUTION
    }

    /** The showcase cooperative; always generated, because the member view of the demo builds on it. */
    public static final String CURATED_LEGAL_FORM = "eG";

    private final int weight;
    private final PersonType personType;
    private final String legalForm;
    private final int minRepresentatives;
    private final int maxRepresentatives;
    private final int minOperationsContacts;
    private final int maxOperationsContacts;
    private final boolean withMembership;

    DemoArchetype(
            final int weight,
            final PersonType personType,
            final String legalForm,
            final int minRepresentatives,
            final int maxRepresentatives,
            final int minOperationsContacts,
            final int maxOperationsContacts,
            final boolean withMembership) {
        this.weight = weight;
        this.personType = personType;
        this.legalForm = legalForm;
        this.minRepresentatives = minRepresentatives;
        this.maxRepresentatives = maxRepresentatives;
        this.minOperationsContacts = minOperationsContacts;
        this.maxOperationsContacts = maxOperationsContacts;
        this.withMembership = withMembership;
    }

    public PersonType personType() {
        return personType;
    }

    public String legalForm() {
        return legalForm;
    }

    public boolean withMembership() {
        return withMembership;
    }

    public int representatives(final DemoValueGenerator values) {
        return values.intBetween(minRepresentatives, maxRepresentatives);
    }

    /** Agencies bill several end customers, everyone else has a single debitor. */
    public int debitors(final DemoValueGenerator values) {
        return this == AGENCY ? 2 : 1;
    }

    public int operationsContacts(final DemoValueGenerator values) {
        return values.intBetween(minOperationsContacts, maxOperationsContacts);
    }

    /** Associations and public institutions are not named after an owner. */
    public boolean mayBeOwnerManaged() {
        return this != ASSOCIATION && this != PUBLIC_INSTITUTION && this != COMMUNITY_OF_HEIRS;
    }

    public boolean hasRegistrationData() {
        return personType == PersonType.LEGAL_PERSON;
    }

    public static List<WordLists.Weighted<DemoArchetype>> weighted() {
        return Stream.of(values())
                .map(archetype -> new WordLists.Weighted<>(archetype, archetype.weight))
                .toList();
    }
}
