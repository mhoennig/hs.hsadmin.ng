package net.hostsharing.hsadminng.hs.booking.item;

import java.util.List;
import java.util.Set;

public interface Node {

    String nodeName();
    boolean belongsToAny(Set<String> groups);
    List<String> edges(final Set<String> inGroup);
}
