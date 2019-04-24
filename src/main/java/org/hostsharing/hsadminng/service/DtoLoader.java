package org.hostsharing.hsadminng.service;

import java.util.Optional;

public interface DtoLoader<T> {
    Optional<? extends T> findOne(Long id);
}
