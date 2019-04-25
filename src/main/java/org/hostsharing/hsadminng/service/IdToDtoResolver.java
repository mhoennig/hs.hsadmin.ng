package org.hostsharing.hsadminng.service;

import java.util.Optional;

public interface IdToDtoResolver<T> {
    Optional<T> findOne(Long id);
}
