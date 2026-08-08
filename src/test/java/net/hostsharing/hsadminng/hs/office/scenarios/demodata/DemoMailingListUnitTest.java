package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.DemoDataInvariants.GeneratedCustomer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoMailingListUnitTest {

    @Test
    void opensTheMembersOnlyListsToMembersOnly() {
        assertThat(DemoMailingList.openTo(false).map(DemoMailingList::mark).toList())
                .containsExactly("customers-announce", "customers-discuss",
                        "operations-announce", "operations-discussion");
        assertThat(DemoMailingList.openTo(true).count()).isEqualTo(DemoMailingList.values().length);
    }

    @Test
    void marksOnlyTheOperationsListsAsTechnical() {
        assertThat(DemoMailingList.values())
                .filteredOn(DemoMailingList::technical)
                .extracting(DemoMailingList::mark)
                .containsExactly("operations-announce", "operations-discussion");
    }

    @Test
    void reachesTheAnnouncementListsMoreOftenThanTheDiscussionLists() {
        assertThat(DemoMailingList.MEMBERS_ANNOUNCE.customerParticipationPercentage())
                .isGreaterThan(DemoMailingList.MEMBERS_DISCUSSION.customerParticipationPercentage());
        assertThat(DemoMailingList.CUSTOMERS_ANNOUNCE.customerParticipationPercentage())
                .isGreaterThan(DemoMailingList.CUSTOMERS_DISCUSS.customerParticipationPercentage());
        assertThat(DemoMailingList.OPERATIONS_ANNOUNCE.customerParticipationPercentage())
                .isGreaterThan(DemoMailingList.OPERATIONS_DISCUSSION.customerParticipationPercentage());
    }

    @Test
    void detectsAMembersOnlyListWithoutMembership() {
        assertThatThrownBy(() -> DemoDataInvariants.assertAllHold(
                List.of(customer(null, Set.of(DemoMailingList.GENERALVERSAMMLUNG))), Set.of()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("a members-only mailing list needs a membership");
    }

    @Test
    void acceptsAnOpenListWithoutMembership() {
        DemoDataInvariants.assertAllHold(
                List.of(customer(null, Set.of(DemoMailingList.CUSTOMERS_ANNOUNCE))), Set.of());
    }

    /** The showcase cooperative has to come first, thus the minimal customer is that one. */
    private GeneratedCustomer customer(final LocalDate joiningDate, final Set<DemoMailingList> mailingLists) {
        val holder = new DemoValueGenerator.Person("Sabine", "Kraus", WordLists.Gender.F, "Frau", null);
        return new GeneratedCustomer(
                "P-90000", DemoArchetype.ASSOCIATION, false, mailingLists,
                "Beispiel eG", "Beispiel eG", holder, null, List.of("aaa"),
                joiningDate, null, null, null);
    }
}
