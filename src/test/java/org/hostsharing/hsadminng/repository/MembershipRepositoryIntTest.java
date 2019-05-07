// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;

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
public class MembershipRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    public void sequenceStartsAbove1000000ToSpareIdsForSampleData() {
        // given
        final Membership givenMembership = createMembership(createPersistentCustomer(), "2019-01-01", null);

        // when
        membershipRepository.save(givenMembership);

        // then
        assertThat(givenMembership.getId()).isGreaterThan(1000000);
    }

    @Test
    public void hasUncancelledMembershipForCustomerIsTrueForCustomerWithUncancelledMembership() {
        // given
        final Customer givenCustomerWithUncancelledMembership = createPersistentCustomerWithMembership("2011-08-18", null);

        // when
        boolean actual = membershipRepository
                .hasUncancelledMembershipForCustomer(givenCustomerWithUncancelledMembership.getId());

        // then
        assertThat(actual).isTrue();
    }

    @Test
    public void hasUncancelledMembershipForCustomerIsFalseForCustomerWithoutMembership() {
        // given
        final Customer givenCustomerWithoutMembership = createPersistentCustomer();

        // when
        boolean actual = membershipRepository.hasUncancelledMembershipForCustomer(givenCustomerWithoutMembership.getId());

        // then
        assertThat(actual).isFalse();
    }

    @Test
    public void hasUncancelledMembershipForCustomerIsFalseForCustomerWithCancelledMembership() {
        // given
        final Customer givenCustomerWithCancelledMembership = createPersistentCustomerWithMembership(
                "2011-08-18",
                "2017-12-31");

        // when
        boolean actual = membershipRepository.hasUncancelledMembershipForCustomer(givenCustomerWithCancelledMembership.getId());

        // then
        assertThat(actual).isFalse();
    }

    // --- only test fixture below ---

    private Customer createPersistentCustomer() {
        return customerRepository.save(CustomerRepositoryIntTest.createCustomer());
    }

    private Customer createPersistentCustomerWithMembership(final String from, final String to) {
        final Customer customer = createPersistentCustomer();
        final Membership membership = createMembership(customer, from, to);
        membershipRepository.save(membership);
        return customer;
    }

    static Membership createMembership(final Customer customer, final String from, final String to) {
        final Membership membership = new Membership();
        membership.setCustomer(customer);
        membership.setMemberFromDate(LocalDate.parse(from));
        if (to != null) {
            membership.setMemberUntilDate(LocalDate.parse(to));
        }
        membership.setAdmissionDocumentDate(membership.getMemberFromDate().minusDays(7));
        return membership;
    }
}
