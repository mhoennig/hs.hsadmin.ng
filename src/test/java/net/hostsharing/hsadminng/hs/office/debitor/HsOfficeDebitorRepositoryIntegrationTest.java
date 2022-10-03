package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsOfficeDebitorRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeDebitorRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    Set<HsOfficeDebitorEntity> tempDebitors = new HashSet<>();

    @Nested
    class CreateDebitor {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = debitorRepo.count();
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newDebitor = toCleanup(HsOfficeDebitorEntity.builder()
                        .uuid(UUID.randomUUID())
                        .debitorNumber(20001)
                        .partner(rawReference(givenPartner))
                        .billingContact(rawReference(givenContact))
                        .build());
                return debitorRepo.save(newDebitor);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeDebitorEntity::getUuid).isNotNull();
            assertThatDebitorIsPersisted(result.returnedValue());
            assertThat(debitorRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> {
                final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Fourth").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newDebitor = toCleanup(HsOfficeDebitorEntity.builder()
                        .uuid(UUID.randomUUID())
                        .debitorNumber(20002)
                        .partner(rawReference(givenPartner))
                        .billingContact(rawReference(givenContact))
                        .build());
                return debitorRepo.save(newDebitor);
            }).assertSuccessful();

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_debitor#20002Fourthe.G.-forthcontact.admin",
                    "hs_office_debitor#20002Fourthe.G.-forthcontact.owner",
                    "hs_office_debitor#20002Fourthe.G.-forthcontact.tenant"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromSkippingNull(
                    initialGrantNames,

                    "{ grant perm * on hs_office_debitor#20002Fourthe.G.-forthcontact to role hs_office_debitor#20002Fourthe.G.-forthcontact.owner by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.owner to role global#global.admin by system and assume }",

                    "{ grant perm edit on hs_office_debitor#20002Fourthe.G.-forthcontact to role hs_office_debitor#20002Fourthe.G.-forthcontact.admin by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.admin to role hs_office_debitor#20002Fourthe.G.-forthcontact.owner by system and assume }",

                    "{ grant perm view on hs_office_debitor#20002Fourthe.G.-forthcontact to role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant to role hs_office_contact#forthcontact.admin by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant to role hs_office_debitor#20002Fourthe.G.-forthcontact.admin by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant to role hs_office_partner#Fourthe.G.-forthcontact.admin by system and assume }",
                    "{ grant role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant to role hs_office_person#Fourthe.G..admin by system and assume }",
                    "{ grant role hs_office_partner#Fourthe.G.-forthcontact.tenant to role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_contact#forthcontact.tenant to role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_person#Fourthe.G..tenant to role hs_office_debitor#20002Fourthe.G.-forthcontact.tenant by system and assume }",

                    null));
        }

        private void assertThatDebitorIsPersisted(final HsOfficeDebitorEntity saved) {
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindByOptionalName {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then
            allTheseDebitorsAreReturned(
                    result,
                    "debitor(10001: First GmbH)",
                    "debitor(10002: Second e.K.)",
                    "debitor(10003: Third OHG)");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "hs_office_partner#FirstGmbH-firstcontact.admin",
                "hs_office_person#FirstGmbH.admin",
                "hs_office_contact#firstcontact.admin",
        })
        public void relatedPersonAdmin_canViewRelatedDebitors(final String assumedRole) {
            // given:
            context("superuser-alex@hostsharing.net", assumedRole);

            // when:
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then:
            exactlyTheseDebitorsAreReturned(result, "debitor(10001: First GmbH)");
        }

        @Test
        public void unrelatedUser_canNotViewAnyDebitor() {
            // given:
            context("selfregistered-test-user@hostsharing.org");

            // when:
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then:
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByDebitorNumberLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByDebitorNumber(10003);

            // then
            exactlyTheseDebitorsAreReturned(result, "debitor(10003: Third OHG)");
        }
    }

    @Nested
    class FindByNameLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByOptionalNameLike("third contact");

            // then
            exactlyTheseDebitorsAreReturned(result, "debitor(10003: Third OHG)");
        }
    }

    @Nested
    class UpdateDebitor {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);
            final var givenNewContact = contactRepo.findContactByOptionalLabelLike("sixth contact").get(0);
            final String givenNewVatId = "NEW-VAT-ID";
            final String givenNewVatCountryCode = "NC";
            final boolean givenNewVatBusiness = !givenDebitor.isVatBusiness();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setBillingContact(rawReference(givenNewContact));
                givenDebitor.setVatId(givenNewVatId);
                givenDebitor.setVatCountryCode(givenNewVatCountryCode);
                givenDebitor.setVatBusiness(givenNewVatBusiness);
                return toCleanup(debitorRepo.save(givenDebitor));
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#sixthcontact.admin");
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#fifthcontact.admin");
        }

        @Test
        public void partnerAdmin_canNotUpdateRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eighth");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_partner#Fourthe.G.-forthcontact.admin");
                givenDebitor.setVatId("NEW-VAT-ID");
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_debitor uuid");
        }

        @Test
        public void contactAdmin_canNotUpdateRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "ninth");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_contact#ninthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_contact#ninthcontact.admin");
                givenDebitor.setVatId("NEW-VAT-ID");
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_debitor uuid");
        }

        private void assertThatDebitorActuallyInDatabase(final HsOfficeDebitorEntity saved) {
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved).usingRecursiveComparison().isEqualTo(saved);
        }

        private void assertThatDebitorIsVisibleForUserWithRole(
                final HsOfficeDebitorEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatDebitorActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatDebitorIsNotVisibleForUserWithRole(
                final HsOfficeDebitorEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = debitorRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyDebitor() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "tenth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                debitorRepo.deleteByUuid(givenDebitor.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return debitorRepo.findByUuid(givenDebitor.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedPerson_canNotDeleteTheirRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-Fourthe.G.@example.com");
                assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isPresent();

                debitorRepo.deleteByUuid(givenDebitor.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_debitor");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.findByUuid(givenDebitor.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingADebitorAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "twelfth");
            assertThat(rawRoleRepo.findAll().size()).as("precondition failed: unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 3);
            assertThat(rawGrantRepo.findAll().size()).as("precondition failed: unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 12);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.deleteByUuid(givenDebitor.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsOfficePartnerEntity rawReference(final HsOfficePartnerEntity givenPartner) {
        return em.getReference(HsOfficePartnerEntity.class, givenPartner.getUuid());
    }

    private HsOfficeContactEntity rawReference(final HsOfficeContactEntity givenContact) {
        return em.getReference(HsOfficeContactEntity.class, givenContact.getUuid());
    }

    private HsOfficeDebitorEntity givenSomeTemporaryDebitor(final String partner, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike(partner).get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike(contact).get(0);
            final var newDebitor = HsOfficeDebitorEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitorNumber(20000)
                    .partner(rawReference(givenPartner))
                    .billingContact(rawReference(givenContact))
                    .build();

            toCleanup(newDebitor);

            return debitorRepo.save(newDebitor);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeDebitorEntity toCleanup(final HsOfficeDebitorEntity tempDebitor) {
        tempDebitors.add(tempDebitor);
        return tempDebitor;
    }

    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        tempDebitors.forEach(tempDebitor -> {
            System.out.println("DELETING temporary debitor: " + tempDebitor.toString());
            debitorRepo.deleteByUuid(tempDebitor.getUuid());
        });
    }

    void exactlyTheseDebitorsAreReturned(final List<HsOfficeDebitorEntity> actualResult, final String... debitorNames) {
        assertThat(actualResult)
                .extracting(HsOfficeDebitorEntity::toString)
                .containsExactlyInAnyOrder(debitorNames);
    }

    void allTheseDebitorsAreReturned(final List<HsOfficeDebitorEntity> actualResult, final String... debitorNames) {
        assertThat(actualResult)
                .extracting(HsOfficeDebitorEntity::toString)
                .contains(debitorNames);
    }
}
