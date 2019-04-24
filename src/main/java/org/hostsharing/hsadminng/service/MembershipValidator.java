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
        if (membershipDTO.getMemberUntilDate() != null && !membershipDTO.getMemberUntilDate().isAfter(membershipDTO.getMemberFromDate())) {
            throw new BadRequestAlertException("Invalid untilDate", Membership.ENTITY_NAME, "untilDateMustBeAfterSinceDate");
        }

        // It's known that this validation can cause a race condition if two memberships of the same customer are saved at
        // same time (overlapping transactions). This is ignored in this case because it's too unlikely to be worth the effort.
        if (membershipRepository.hasUncancelledMembershipForCustomer(membershipDTO.getCustomerId())) {
            throw new BadRequestAlertException("Another uncancelled membership exists", Membership.ENTITY_NAME, "anotherUncancelledMembershipExists");
        }
    }
}
