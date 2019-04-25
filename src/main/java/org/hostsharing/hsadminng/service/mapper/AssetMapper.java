package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the entity Asset and its DTO AssetDTO.
 */
@Mapper(componentModel = "spring", uses = {MembershipMapper.class})
public interface AssetMapper extends EntityMapper<AssetDTO, Asset> {

    @Mapping(source = "membership.id", target = "membershipId")
    @Mapping(source = "membership.admissionDocumentDate", target = "membershipDisplayReference")
    AssetDTO toDto(Asset asset);

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
