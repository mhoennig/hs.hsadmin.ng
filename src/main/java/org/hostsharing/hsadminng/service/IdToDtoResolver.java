// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import java.util.Optional;

public interface IdToDtoResolver<T> {

    Optional<T> findOne(Long id);
}
