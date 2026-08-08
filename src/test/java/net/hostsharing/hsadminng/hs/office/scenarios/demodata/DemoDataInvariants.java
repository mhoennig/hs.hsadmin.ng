package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.DemoArchetype.PersonType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the generated dataset has to satisfy to look real rather than merely well-formed.
 * Checked after generation, because a wrongly wired parameter passes every single request
 * and only shows up as an implausible combination in the finished dataset.
 */
final class DemoDataInvariants {

    record GeneratedCustomer(
            String partnerNumber,
            DemoArchetype archetype,
            boolean fromFixedPersonSet,
            Set<DemoMailingList> mailingLists,
            String partnerName,
            String tradeName,
            DemoValueGenerator.Person holder,
            LocalDate birthday,
            List<String> defaultPrefixes,
            LocalDate joiningDate,
            Integer shareCount,
            Integer assetValue,
            LocalDate depositDate) {}

    private DemoDataInvariants() {
    }

    static void assertAllHold(final List<GeneratedCustomer> customers, final Set<String> fixedPersonNames) {
        assertThat(customers).as("no customers generated").isNotEmpty();

        assertShowcaseCooperativeComesFirst(customers);
        assertPartnerNumbersAreUnique(customers);
        assertDebitorPrefixesAreUnique(customers);
        assertDrawnNamesStayClearOfTheFixedPersonSet(customers, fixedPersonNames);

        customers.forEach(DemoDataInvariants::assertCustomerIsPlausible);
    }

    private static void assertShowcaseCooperativeComesFirst(final List<GeneratedCustomer> customers) {
        assertThat(customers.get(0).tradeName())
                .as("the showcase cooperative has to be generated first")
                .endsWith(" " + DemoArchetype.CURATED_LEGAL_FORM);
    }

    private static void assertPartnerNumbersAreUnique(final List<GeneratedCustomer> customers) {
        assertThat(customers.stream().map(GeneratedCustomer::partnerNumber).distinct().count())
                .as("partner numbers have to be unique")
                .isEqualTo(customers.size());
    }

    private static void assertDebitorPrefixesAreUnique(final List<GeneratedCustomer> customers) {
        assertThat(customers.stream().flatMap(c -> c.defaultPrefixes().stream()).distinct().count())
                .as("debitor default prefixes have to be unique")
                .isEqualTo(customers.stream().mapToLong(c -> c.defaultPrefixes().size()).sum());
    }

    private static void assertDrawnNamesStayClearOfTheFixedPersonSet(
            final List<GeneratedCustomer> customers,
            final Set<String> fixedPersonNames) {
        if (fixedPersonNames.isEmpty()) {
            return;
        }

        assertThat(customers.stream()
                .filter(customer -> !customer.fromFixedPersonSet())
                .map(c -> c.holder().fullName()))
                .as("drawn names must not collide with the fixed person set, "
                        + "or its by-name lookup becomes ambiguous")
                .doesNotContainAnyElementsOf(fixedPersonNames);
    }

    private static void assertCustomerIsPlausible(final GeneratedCustomer customer) {
        val describedAs = "customer " + customer.partnerNumber();

        assertThat(customer.holder().salutation())
                .as(describedAs + ": salutation has to match the gender of the given name")
                .isEqualTo(salutationFor(customer.holder().gender()));
        assertThat(customer.defaultPrefixes())
                .as(describedAs + ": every customer needs at least one debitor")
                .isNotEmpty();

        assertTradeNameAndBirthdayMatchThePersonType(describedAs, customer);
        assertMembershipIsConsistent(describedAs, customer);
    }

    /** The fixed persons carry no gender, thus they carry no salutation either. */
    private static String salutationFor(final WordLists.Gender gender) {
        return gender == null ? null : (gender == WordLists.Gender.F ? "Frau" : "Herr");
    }

    private static void assertTradeNameAndBirthdayMatchThePersonType(
            final String describedAs,
            final GeneratedCustomer customer) {
        if (customer.archetype().personType() == PersonType.NATURAL_PERSON) {
            assertThat(customer.tradeName()).as(describedAs + ": a natural person has no trade name").isNull();
            assertThat(customer.birthday()).as(describedAs + ": a natural person has a birthday").isNotNull();
        } else {
            assertThat(customer.tradeName()).as(describedAs + ": a legal person has a trade name").isNotNull();
            assertThat(customer.birthday()).as(describedAs + ": only natural persons have a birthday").isNull();
        }
    }

    private static void assertMembershipIsConsistent(
            final String describedAs,
            final GeneratedCustomer customer) {
        if (customer.joiningDate() == null) {
            assertThat(customer.mailingLists().stream().filter(DemoMailingList::membersOnly).toList())
                    .as(describedAs + ": a members-only mailing list needs a membership")
                    .isEmpty();
            return;
        }

        assertThat(customer.shareCount())
                .as(describedAs + ": a membership needs subscribed shares")
                .isNotNull().isPositive();
        assertThat(customer.assetValue())
                .as(describedAs + ": the deposit has to match the subscribed shares")
                .isEqualTo(customer.shareCount() * GenerateDemoDataset.SHARE_VALUE_IN_EUR);
        assertThat(customer.depositDate())
                .as(describedAs + ": shares are never paid in before the membership starts")
                .isAfterOrEqualTo(customer.joiningDate());
    }
}
