package net.hostsharing.hsadminng.hs.admin.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static net.hostsharing.hsadminng.hs.admin.partner.TestHsAdminPartner.testLtd;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, HsAdminPartnerRepository.class })
@DirtiesContext
class HsAdminPartnerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsAdminPartnerRepository partnerRepository;

    @Autowired
    EntityManager em;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateCustomer {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewCustomer() {
            // given
            context("alex@hostsharing.net", null);
            final var count = partnerRepository.count();

            // when

            final var result = attempt(em, () -> {
                return partnerRepository.save(testLtd);
            });

            // then
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminPartnerEntity::getUuid).isNotNull();
            assertThatPartnerIsPersisted(result.returnedValue());
            assertThat(partnerRepository.count()).isEqualTo(count + 1);
        }

        private void assertThatPartnerIsPersisted(final HsAdminPartnerEntity saved) {
            final var found = partnerRepository.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllCustomers {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("alex@hostsharing.net", null);

            // when
            final var result = partnerRepository.findPartnerByOptionalNameLike(null);

            // then
            allThesePartnersAreReturned(result, "Ixx AG", "Ypsilon GmbH", "Zett OHG");
        }

    }

    @Nested
    class FindByPrefixLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("alex@hostsharing.net", null);

            // when
            final var result = partnerRepository.findPartnerByOptionalNameLike("Yps");

            // then
            exactlyTheseCustomersAreReturned(result, "Ypsilon GmbH");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("customer-admin@xxx.example.com", null);

            // when:
            final var result = partnerRepository.findPartnerByOptionalNameLike("Yps");

            // then:
            exactlyTheseCustomersAreReturned(result);
        }
    }

    void exactlyTheseCustomersAreReturned(final List<HsAdminPartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .hasSize(partnerTradeNames.length)
                .extracting(HsAdminPartnerEntity::getPerson)
                .extracting(HsAdminPersonEntity::getTradeName)
                .containsExactlyInAnyOrder(partnerTradeNames);
    }

    void allThesePartnersAreReturned(final List<HsAdminPartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .extracting(HsAdminPartnerEntity::getPerson)
                .extracting(HsAdminPersonEntity::getTradeName)
                .contains(partnerTradeNames);
    }
}
