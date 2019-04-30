// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for the entity Customer and its DTO CustomerDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface CustomerMapper extends EntityMapper<CustomerDTO, Customer> {

    static String displayLabel(Customer customer) {
        return customer.getName()
                + " [" + customer.getReference() + ":" + customer.getPrefix() + "]";
    }

    @Mapping(target = "displayLabel", ignore = true)
    CustomerDTO toDto(Customer customer);

    @AfterMapping
    default void setDisplayLabel(final @MappingTarget CustomerDTO dto, final Customer entity) {
        dto.setDisplayLabel(displayLabel(entity));
    }

    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "sepamandates", ignore = true)
    Customer toEntity(CustomerDTO customerDTO);

    default Customer fromId(Long id) {
        if (id == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setId(id);
        return customer;
    }
}
