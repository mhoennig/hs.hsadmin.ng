package net.hostsharing.hsadminng.hs.office.partner;

public interface EntityPatch<R> {

    void apply(R resource);
}
