// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
@Transactional
public class CustomerRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void sequenceStartsAbove1000000ToSpareIdsForSampleData() {
        // given
        final Customer givenCustomer = createCustomer();

        // when
        customerRepository.save(givenCustomer);

        // then
        assertThat(givenCustomer.getId()).isGreaterThan(1000000);
    }

    // --- only test fixture below ---

    static Customer createCustomer() {
        final Customer customer = new Customer();
        customer.setPrefix(RandomStringUtils.randomAlphabetic(3).toLowerCase());
        customer.setReference(RandomUtils.nextInt(10001, 19999));
        customer.setName(RandomStringUtils.randomAlphabetic(10));
        customer.setContractualAddress(RandomStringUtils.randomAlphabetic(10));
        customer.setKind(CustomerKind.NATURAL);
        customer.setVatRegion(VatRegion.DOMESTIC);
        return customer;
    }
}
