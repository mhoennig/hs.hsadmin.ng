// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
@Transactional
public class ShareRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ShareRepository shareRepository;

    @Test
    public void sequenceStartsAbove1000000ToSpareIdsForSampleData() {
        // given
        final Share givenShare = createArbitraryShare();

        // when
        shareRepository.save(givenShare);

        // then
        assertThat(givenShare.getId()).isGreaterThan(1000000);
    }

    // --- only test fixture below ---

    private Customer createPersistentCustomer() {
        return customerRepository.save(CustomerRepositoryIntTest.createCustomer());
    }

    private Membership createPersistentMembership() {
        return membershipRepository
                .save(MembershipRepositoryIntTest.createMembership(createPersistentCustomer(), "2019-01-08", null));
    }

    static Share createShare(
            final Membership membership,
            final ShareAction action,
            final int quantity,
            final String documentDate) {
        final Share share = new Share();
        share.setMembership(membership);
        share.setAction(action);
        share.setQuantity(quantity);
        share.setDocumentDate(LocalDate.parse(documentDate));
        share.setValueDate(LocalDate.parse(documentDate).plusDays(1));
        return share;
    }

    private Share createArbitraryShare() {
        return createShare(createPersistentMembership(), ShareAction.SUBSCRIPTION, 1, "2019-01-08");
    }
}
