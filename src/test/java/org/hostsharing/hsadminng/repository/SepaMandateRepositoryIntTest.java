// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.SepaMandate;

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
public class SepaMandateRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SepaMandateRepository sepaMandateRepository;

    @Test
    public void sequenceStartsAbove1000000ToSpareIdsForSampleData() {
        // given
        final SepaMandate givenSepaMandate = createSepaMandate(createPersistentCustomer(), "DUMMY_REF", "2019-01-08", null);

        // when
        sepaMandateRepository.save(givenSepaMandate);

        // then
        assertThat(givenSepaMandate.getId()).isGreaterThan(1000000);
    }

    // --- only test fixture below ---

    private Customer createPersistentCustomer() {
        return customerRepository.save(CustomerRepositoryIntTest.createCustomer());
    }

    static SepaMandate createSepaMandate(final Customer customer, final String reference, final String from, final String to) {
        final SepaMandate sepaMandate = new SepaMandate();
        sepaMandate.setCustomer(customer);
        sepaMandate.setReference(reference);
        sepaMandate.setIban("NL57ABNA2228161411");
        sepaMandate.setBic("ABNANL2A");
        sepaMandate.setGrantingDocumentDate(LocalDate.parse(from));
        sepaMandate.setValidFromDate(LocalDate.parse(from).plusDays(1));
        if (to != null) {
            sepaMandate.setRevokationDocumentDate(LocalDate.parse(to));
            sepaMandate.setValidUntilDate(LocalDate.parse(to).plusDays(7));
        }
        return sepaMandate;
    }
}
