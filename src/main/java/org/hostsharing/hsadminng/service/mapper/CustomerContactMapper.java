package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.CustomerContactDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity CustomerContact and its DTO CustomerContactDTO.
 */
@Mapper(componentModel = "spring", uses = {ContactMapper.class, CustomerMapper.class})
public interface CustomerContactMapper extends EntityMapper<CustomerContactDTO, CustomerContact> {

    @Mapping(source = "contact.id", target = "contactId")
    @Mapping(source = "contact.email", target = "contactEmail")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.prefix", target = "customerPrefix")
    CustomerContactDTO toDto(CustomerContact customerContact);

    @Mapping(source = "contactId", target = "contact")
    @Mapping(source = "customerId", target = "customer")
    CustomerContact toEntity(CustomerContactDTO customerContactDTO);

    default CustomerContact fromId(Long id) {
        if (id == null) {
            return null;
        }
        CustomerContact customerContact = new CustomerContact();
        customerContact.setId(id);
        return customerContact;
    }
}
