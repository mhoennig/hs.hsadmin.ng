package org.hostsharing.hsadminng.repository;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
@Transactional
public class MembershipRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    public void hasUncancelledMembershipForCustomerIsTrueForCustomerWithUncancelledMembership() {
        // given
        final Customer givenCustomerWithUncancelledMembership = createCustomerWithMembership("2011-08-18", null);

        // when
        boolean actual = membershipRepository.hasUncancelledMembershipForCustomer(givenCustomerWithUncancelledMembership.getId());

        // then
        assertThat(actual).isTrue();
    }

    @Test
    public void hasUncancelledMembershipForCustomerIsFalseForCustomerWithoutMembership() {
        // given
        final Customer givenCustomerWithoutMembership = createCustomer();

        // when
        boolean actual = membershipRepository.hasUncancelledMembershipForCustomer(givenCustomerWithoutMembership.getId());

        // then
        assertThat(actual).isFalse();
    }

    @Test
    public void hasUncancelledMembershipForCustomerIsFalseForCustomerWithCancelledMembership() {
        // given
        final Customer givenCustomerWithCancelledMembership = createCustomerWithMembership("2011-08-18", "2017-12-31");

        // when
        boolean actual = membershipRepository.hasUncancelledMembershipForCustomer(givenCustomerWithCancelledMembership.getId());

        // then
        assertThat(actual).isFalse();
    }

    // --- only test fixture below ---

    private Customer createCustomer() {
        final Customer customer = new Customer();
        customer.setPrefix(RandomStringUtils.randomAlphabetic(3).toLowerCase());
        customer.setReference(RandomUtils.nextInt(10001, 19999));
        customer.setName(RandomStringUtils.randomAlphabetic(10));
        customer.setContractualAddress(RandomStringUtils.randomAlphabetic(10));
        customerRepository.save(customer);
        return customer;
    }

    private Customer createCustomerWithMembership(final String from, final String to) {
        final Customer customer = createCustomer();
        final Membership membership = new Membership();
        membership.setCustomer(customer);
        membership.setMemberUntil(LocalDate.parse(from));
        if (to != null) {
            membership.setMemberFrom(LocalDate.parse(to));
        }
        membershipRepository.save(membership);
        return customer;
    }
}
