package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Objects;

/**
 * Mapper for the entity Membership and its DTO MembershipDTO.
 */
@Mapper(componentModel = "spring", uses = {CustomerMapper.class})
public interface MembershipMapper extends EntityMapper<MembershipDTO, Membership> {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.prefix", target = "customerPrefix")
    @Mapping(target = "membershipDisplayReference", ignore = true)
    MembershipDTO toDto(Membership membership);

    // TODO BLOG HOWTO: multi-field display reference for selection lists
    //  also change the filed in the option list in *-update.html
    @AfterMapping
    default void setMembershipDisplayReference(final @MappingTarget MembershipDTO dto, final Membership entity) {
        final Customer customer = entity.getCustomer();
        dto.setMembershipDisplayReference(customer.getReference()
            + ":" + customer.getPrefix()
            + " [" + customer.getName() + "] "
            + entity.getAdmissionDocumentDate().toString() + " - "
            + Objects.toString(entity.getCancellationDocumentDate(), ""));
    }

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
