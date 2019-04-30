// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity SepaMandate and its DTO SepaMandateDTO.
 */
@Mapper(componentModel = "spring", uses = { CustomerMapper.class })
public interface SepaMandateMapper extends EntityMapper<SepaMandateDTO, SepaMandate> {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.prefix", target = "customerPrefix")
    SepaMandateDTO toDto(SepaMandate sepaMandate);

    @Mapping(source = "customerId", target = "customer")
    SepaMandate toEntity(SepaMandateDTO sepaMandateDTO);

    default SepaMandate fromId(Long id) {
        if (id == null) {
            return null;
        }
        SepaMandate sepaMandate = new SepaMandate();
        sepaMandate.setId(id);
        return sepaMandate;
    }
}
