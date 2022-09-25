package net.hostsharing.hsadminng;

public interface EntityPatch<R> {

    void apply(R resource);
}
