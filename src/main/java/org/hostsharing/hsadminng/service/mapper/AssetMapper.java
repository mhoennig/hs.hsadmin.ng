// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.service.dto.AssetDTO;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for the entity Asset and its DTO AssetDTO.
 */
@Mapper(componentModel = "spring", uses = { MembershipMapper.class })
public interface AssetMapper extends EntityMapper<AssetDTO, Asset> {

    @Mapping(source = "membership.id", target = "membershipId")
    @Mapping(target = "membershipDisplayLabel", ignore = true)
    AssetDTO toDto(Asset asset);

    @AfterMapping
    default void setMembershipDisplayLabel(final @MappingTarget AssetDTO dto, final Asset entity) {
        dto.setMembershipDisplayLabel(MembershipMapper.displayLabel(entity.getMembership()));
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
