package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Objects;

/**
 * Mapper for the entity Asset and its DTO AssetDTO.
 */
@Mapper(componentModel = "spring", uses = {MembershipMapper.class})
public interface AssetMapper extends EntityMapper<AssetDTO, Asset> {

    @Mapping(source = "membership.id", target = "membershipId")
    @Mapping(target = "membershipDisplayReference", ignore = true)
    AssetDTO toDto(Asset asset);

    @AfterMapping
    default void setMembershipDisplayReference(final @MappingTarget AssetDTO dto, final Asset entity) {
        // TODO: rather use method extracted from MembershipMaper.setMembershipDisplayReference() to avoid duplicate code
        final Membership membership = entity.getMembership();
        final Customer customer = membership.getCustomer();
        dto.setMembershipDisplayReference(customer.getReference()
            + ":" + customer.getPrefix()
            + " [" + customer.getName() + "] "
            + membership.getAdmissionDocumentDate().toString() + " - "
            + Objects.toString(membership.getCancellationDocumentDate(), ""));
    }

    @Mapping(source = "membershipId", target = "membership")
    Asset toEntity(AssetDTO assetDTO);

    default Asset fromId(Long id) {
        if (id == null) {
            return null;
        }
        Asset asset = new Asset();
        asset.setId(id);
        return asset;
    }
}
