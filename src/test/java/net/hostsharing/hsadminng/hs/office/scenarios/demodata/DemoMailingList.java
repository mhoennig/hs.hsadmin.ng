package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import java.util.stream.Stream;

/**
 * The mailing lists of the cooperative, named by the qualifier {@code mark} of a SUBSCRIBER relation.
 * A customer takes part as a whole and then delegates one or two people, rather than every person
 * deciding for itself, which is what puts a member on the list once instead of five times.
 */
public enum DemoMailingList {

    // mark, membersOnly, customerParticipationPercentage, technical
    GENERALVERSAMMLUNG("generalversammlung", true, 95, false),
    MEMBERS_ANNOUNCE("members-announce", true, 90, false),
    MEMBERS_DISCUSSION("members-discussion", true, 30, false),
    CUSTOMERS_ANNOUNCE("customers-announce", false, 85, false),
    CUSTOMERS_DISCUSS("customers-discuss", false, 25, false),
    OPERATIONS_ANNOUNCE("operations-announce", false, 55, true),
    OPERATIONS_DISCUSSION("operations-discussion", false, 20, true);

    private final String mark;
    private final boolean membersOnly;
    private final int customerParticipationPercentage;
    private final boolean technical;

    DemoMailingList(
            final String mark,
            final boolean membersOnly,
            final int customerParticipationPercentage,
            final boolean technical) {
        this.mark = mark;
        this.membersOnly = membersOnly;
        this.customerParticipationPercentage = customerParticipationPercentage;
        this.technical = technical;
    }

    public String mark() {
        return mark;
    }

    public boolean membersOnly() {
        return membersOnly;
    }

    public int customerParticipationPercentage() {
        return customerParticipationPercentage;
    }

    /** A technical list is read by the operations contacts, not by the people who run the business. */
    public boolean technical() {
        return technical;
    }

    static Stream<DemoMailingList> openTo(final boolean withMembership) {
        return Stream.of(values()).filter(list -> withMembership || !list.membersOnly());
    }
}
