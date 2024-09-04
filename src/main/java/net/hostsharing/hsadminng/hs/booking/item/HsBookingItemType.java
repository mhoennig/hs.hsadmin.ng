package net.hostsharing.hsadminng.hs.booking.item;

import java.util.List;
import java.util.Set;

import static java.util.Optional.ofNullable;

public enum HsBookingItemType implements Node {
    PRIVATE_CLOUD,
    CLOUD_SERVER(PRIVATE_CLOUD),
    MANAGED_SERVER(PRIVATE_CLOUD),
    MANAGED_WEBSPACE(MANAGED_SERVER),
    DOMAIN_SETUP;

    private final HsBookingItemType parentItemType;

    HsBookingItemType() {
        this.parentItemType = null;
    }

    HsBookingItemType(final HsBookingItemType parentItemType) {
        this.parentItemType = parentItemType;
    }

    @Override
    public List<String> edges(final Set<String> inGroups) {
        return ofNullable(parentItemType)
                .map(p -> (nodeName() + " *--> " + p.nodeName()))
                .stream().toList();
    }

    @Override
    public boolean belongsToAny(final Set<String> groups) {
        return true; // we currently do not filter booking item types
    }

    @Override
    public String nodeName() {
        return "BI_" + name();
    }

}
