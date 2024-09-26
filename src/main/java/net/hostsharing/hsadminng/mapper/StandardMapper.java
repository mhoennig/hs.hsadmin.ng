package net.hostsharing.hsadminng.mapper;

import org.springframework.stereotype.Component;

/**
 * A nicer API for ModelMapper in standard mode.
 */
@Component
public class StandardMapper extends Mapper {

    public StandardMapper() {
        getConfiguration().setAmbiguityIgnored(true);
    }
}
