package net.hostsharing.hsadminng;

public interface EntityPatcher<R> {

    void apply(R resource);
}
