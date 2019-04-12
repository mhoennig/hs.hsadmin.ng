package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MembershipValidator {

    @Autowired
    private MembershipRepository membershipRepository;

    public void validate(final MembershipDTO membershipDTO) {
        if (membershipDTO.getUntilDate() != null && !membershipDTO.getUntilDate().isAfter(membershipDTO.getSinceDate())) {
            throw new BadRequestAlertException("Invalid untilDate", Membership.ENTITY_NAME, "untilDateMustBeAfterSinceDate");
        }

        if (membershipRepository.hasUncancelledMembershipForCustomer(membershipDTO.getCustomerId())) {
            throw new BadRequestAlertException("Another uncancelled membership exists", Membership.ENTITY_NAME, "anotherUncancelledMembershipExists");
        }
    }
}
