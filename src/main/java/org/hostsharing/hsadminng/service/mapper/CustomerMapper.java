// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Customer and its DTO CustomerDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface CustomerMapper extends EntityMapper<CustomerDTO, Customer> {

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
