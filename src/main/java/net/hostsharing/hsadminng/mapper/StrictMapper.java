package net.hostsharing.hsadminng.mapper;

import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.modelmapper.convention.MatchingStrategies.STRICT;

/**
 * A nicer API for ModelMapper in strict mode.
 *
 * <p>This makes sure that resource.whateverUuid does not accidentally get mapped to entity.uuid,
 * if resource.uuid does not exist.</p>
 */
@Component
public class StrictMapper extends Mapper {

    public StrictMapper(@Autowired final EntityManagerWrapper em) {
        super(em);
        getConfiguration().setMatchingStrategy(STRICT);
    }
}
