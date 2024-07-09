package net.hostsharing.hsadminng.hs.booking.item;

import java.util.List;

import static java.util.Optional.ofNullable;

public enum HsBookingItemType implements Node {
    PRIVATE_CLOUD,
    CLOUD_SERVER(PRIVATE_CLOUD),
    MANAGED_SERVER(PRIVATE_CLOUD),
    MANAGED_WEBSPACE(MANAGED_SERVER),
    DOMAIN_DNS_SETUP, // TODO.spec: experimental
    DOMAIN_EMAIL_SUBMISSION_SETUP; // TODO.spec: experimental

    private final HsBookingItemType parentItemType;

    HsBookingItemType() {
        this.parentItemType = null;
    }

    HsBookingItemType(final HsBookingItemType parentItemType) {
        this.parentItemType = parentItemType;
    }

    @Override
    public List<String> edges() {
        return ofNullable(parentItemType)
                .map(p -> (nodeName() + " *--> " + p.nodeName()))
                .stream().toList();
    }

    @Override
    public String nodeName() {
        return "BI_" + name();
    }

}
