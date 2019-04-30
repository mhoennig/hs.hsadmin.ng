// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.ShareDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Share and its DTO ShareDTO.
 */
@Mapper(componentModel = "spring", uses = { MembershipMapper.class })
public interface ShareMapper extends EntityMapper<ShareDTO, Share> {

    @Mapping(source = "membership.id", target = "membershipId")
    @Mapping(source = "membership.admissionDocumentDate", target = "membershipAdmissionDocumentDate")
    ShareDTO toDto(Share share);

    @Mapping(source = "membershipId", target = "membership")
    Share toEntity(ShareDTO shareDTO);

    default Share fromId(Long id) {
        if (id == null) {
            return null;
        }
        Share share = new Share();
        share.setId(id);
        return share;
    }
}
