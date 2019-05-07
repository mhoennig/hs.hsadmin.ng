// Licensed under Apache-2.0
package org.hostsharing.hsadminng.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
@Transactional
public class AssetRepositoryIntTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Test
    public void sequenceStartsAbove1000000ToSpareIdsForSampleData() {
        // given
        final Asset givenAsset = createArbitraryAsset();

        // when
        assetRepository.save(givenAsset);

        // then
        assertThat(givenAsset.getId()).isGreaterThan(1000000);
    }

    // --- only test fixture below ---

    private Customer createPersistentCustomer() {
        return customerRepository.save(CustomerRepositoryIntTest.createCustomer());
    }

    private Membership createPersistentMembership() {
        return membershipRepository
                .save(MembershipRepositoryIntTest.createMembership(createPersistentCustomer(), "2019-01-08", null));
    }

    static Asset createAsset(
            final Membership membership,
            final AssetAction action,
            final String amount,
            final String documentDate) {
        final Asset asset = new Asset();
        asset.setMembership(membership);
        asset.setAction(action);
        asset.setAmount(new BigDecimal(amount));
        asset.setDocumentDate(LocalDate.parse(documentDate));
        asset.setValueDate(LocalDate.parse(documentDate).plusDays(1));
        return asset;
    }

    private Asset createArbitraryAsset() {
        return createAsset(createPersistentMembership(), AssetAction.PAYMENT, "160.00", "2019-01-08");
    }
}
