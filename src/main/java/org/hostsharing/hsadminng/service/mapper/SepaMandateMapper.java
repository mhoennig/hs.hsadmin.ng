// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for the entity SepaMandate and its DTO SepaMandateDTO.
 */
@Mapper(componentModel = "spring", uses = { CustomerMapper.class })
public interface SepaMandateMapper extends EntityMapper<SepaMandateDTO, SepaMandate> {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "customerDisplayLabel", ignore = true)
    SepaMandateDTO toDto(SepaMandate sepaMandate);

    @AfterMapping
    default void setDisplayLabels(final @MappingTarget SepaMandateDTO dto, final SepaMandate entity) {
        dto.setCustomerDisplayLabel(CustomerMapper.displayLabel(entity.getCustomer()));
    }

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
