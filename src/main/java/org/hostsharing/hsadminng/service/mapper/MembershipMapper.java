// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Membership and its DTO MembershipDTO.
 */
@Mapper(componentModel = "spring", uses = { CustomerMapper.class })
public interface MembershipMapper extends EntityMapper<MembershipDTO, Membership> {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.prefix", target = "customerPrefix")
    MembershipDTO toDto(Membership membership);

    @Mapping(target = "shares", ignore = true)
    @Mapping(target = "assets", ignore = true)
    @Mapping(source = "customerId", target = "customer")
    Membership toEntity(MembershipDTO membershipDTO);

    default Membership fromId(Long id) {
        if (id == null) {
            return null;
        }
        Membership membership = new Membership();
        membership.setId(id);
        return membership;
    }
}
