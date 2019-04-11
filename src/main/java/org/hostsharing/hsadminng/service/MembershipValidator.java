package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.springframework.stereotype.Service;

@Service
public class MembershipValidator {
    public void validate(final MembershipDTO membershipDTO) {
        if (membershipDTO.getUntilDate() != null && !membershipDTO.getUntilDate().isAfter(membershipDTO.getSinceDate())) {
            throw new BadRequestAlertException("Invalid untilDate", Membership.ENTITY_NAME, "untilDateMustBeAfterSinceDate");
        }
    }
}
