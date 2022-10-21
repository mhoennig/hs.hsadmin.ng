package net.hostsharing.hsadminng.mapper;

public interface EntityPatcher<R> {

    void apply(R resource);
}
