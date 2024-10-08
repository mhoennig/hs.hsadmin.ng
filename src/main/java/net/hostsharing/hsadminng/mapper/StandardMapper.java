package net.hostsharing.hsadminng.mapper;

import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A nicer API for ModelMapper in standard mode.
 */
@Component
public class StandardMapper extends Mapper {

    public StandardMapper(@Autowired final EntityManagerWrapper em) {
        super(em);
        getConfiguration().setAmbiguityIgnored(true);
    }
}
