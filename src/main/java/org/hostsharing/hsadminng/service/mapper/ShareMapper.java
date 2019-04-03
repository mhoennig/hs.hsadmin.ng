package org.hostsharing.hsadminng.service.mapper;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.service.dto.ShareDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Share and its DTO ShareDTO.
 */
@Mapper(componentModel = "spring", uses = {MembershipMapper.class})
public interface ShareMapper extends EntityMapper<ShareDTO, Share> {

    @Mapping(source = "member.id", target = "memberId")
    ShareDTO toDto(Share share);

    @Mapping(source = "memberId", target = "member")
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
