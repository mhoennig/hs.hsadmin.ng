package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.internal.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.bankaccount.TestHsOfficeBankAccount.hsOfficeBankAccount;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsOfficeBankAccountRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeBankAccountRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeBankAccountRepository bankaccountRepo;

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

    @Container
    Container postgres;

    @Nested
    class CreateBankAccount {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewBankAccount() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = bankaccountRepo.count();

            // when
            final var result = attempt(em, () -> bankaccountRepo.save(
                    hsOfficeBankAccount("some temp acc A", "DE37500105177419788228", "")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeBankAccountEntity::getUuid).isNotNull();
            assertThatBankAccountIsPersisted(result.returnedValue());
            assertThat(bankaccountRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewBankAccount() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var count = bankaccountRepo.count();

            // when
            final var result = attempt(em, () -> bankaccountRepo.save(
                    hsOfficeBankAccount("some temp acc B", "DE49500105174516484892", "INGDDEFFXXX")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeBankAccountEntity::getUuid).isNotNull();
            assertThatBankAccountIsPersisted(result.returnedValue());
            assertThat(bankaccountRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> bankaccountRepo.save(
                    hsOfficeBankAccount("some temp acc C", "DE25500105176934832579", "INGDDEFFXXX"))
            ).assumeSuccessful();

            // then
            final var roles = rawRoleRepo.findAll();
            assertThat(roleNamesOf(roles)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_bankaccount#sometempaccC.owner",
                    "hs_office_bankaccount#sometempaccC.admin",
                    "hs_office_bankaccount#sometempaccC.tenant",
                    "hs_office_bankaccount#sometempaccC.guest"
            ));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    "{ grant perm delete on hs_office_bankaccount#sometempaccC to role hs_office_bankaccount#sometempaccC.owner         by system and assume }",
                    "{ grant role hs_office_bankaccount#sometempaccC.owner     to role global#global.admin                              by system and assume }",
                    "{ grant role hs_office_bankaccount#sometempaccC.owner     to user selfregistered-user-drew@hostsharing.org         by global#global.admin and assume }",

                    "{ grant role hs_office_bankaccount#sometempaccC.admin     to role hs_office_bankaccount#sometempaccC.owner         by system and assume }",

                    "{ grant role hs_office_bankaccount#sometempaccC.tenant    to role hs_office_bankaccount#sometempaccC.admin         by system and assume }",

                    "{ grant perm view on hs_office_bankaccount#sometempaccC   to role hs_office_bankaccount#sometempaccC.guest         by system and assume }",
                    "{ grant role hs_office_bankaccount#sometempaccC.guest     to role hs_office_bankaccount#sometempaccC.tenant        by system and assume }",
                    null
            ));
        }

        private void assertThatBankAccountIsPersisted(final HsOfficeBankAccountEntity saved) {
            final var found = bankaccountRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllBankAccounts {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllBankAccounts() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = bankaccountRepo.findByOptionalHolderLike(null);

            // then
            allTheseBankAccountsAreReturned(
                    result,
                    "Anita Bessler",
                    "First GmbH",
                    "Fourth e.G.",
                    "Mel Bessler",
                    "Paul Winkler",
                    "Peter Smith",
                    "Second e.K.",
                    "Third OHG");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnBankAccount() {
            // given:
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = bankaccountRepo.findByOptionalHolderLike(null);

            // then:
            exactlyTheseBankAccountsAreReturned(result, givenBankAccount.getHolder());
        }
    }

    @Nested
    class FindByLabelLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllBankAccounts() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = bankaccountRepo.findByOptionalHolderLike(null);

            // then
            exactlyTheseBankAccountsAreReturned(
                    result,
                    "Anita Bessler",
                    "First GmbH",
                    "Fourth e.G.",
                    "Mel Bessler",
                    "Paul Winkler",
                    "Peter Smith",
                    "Second e.K.",
                    "Third OHG");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnBankAccount() {
            // given:
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = bankaccountRepo.findByOptionalHolderLike(givenBankAccount.getHolder());

            // then:
            exactlyTheseBankAccountsAreReturned(result, givenBankAccount.getHolder());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyBankAccount() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                bankaccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return bankaccountRepo.findByOptionalHolderLike(givenBankAccount.getHolder());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canDeleteABankAccountCreatedByItself() {
            // given
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                bankaccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return bankaccountRepo.findByOptionalHolderLike(givenBankAccount.getHolder());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void deletingABankAccountAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("selfregistered-user-drew@hostsharing.org", null);
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());
            final var givenBankAccount = givenSomeTemporaryBankAccount("selfregistered-user-drew@hostsharing.org");
            assertThat(rawRoleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleNames.size() + 4);
            assertThat(rawGrantRepo.findAll().size()).as("unexpected number of grants created")
                    .isEqualTo(initialGrantNames.size() + 7);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return bankaccountRepo.deleteByUuid(givenBankAccount.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames
            ));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames
            ));
        }
    }

    private HsOfficeBankAccountEntity givenSomeTemporaryBankAccount(
            final String createdByUser,
            Supplier<HsOfficeBankAccountEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return bankaccountRepo.save(entitySupplier.get());
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        final var result = bankaccountRepo.findByOptionalHolderLike("some temp acc");
        result.forEach(tempPerson -> {
            System.out.println("DELETING temporary bankaccount: " + tempPerson.getHolder());
            bankaccountRepo.deleteByUuid(tempPerson.getUuid());
        });
    }

    private HsOfficeBankAccountEntity givenSomeTemporaryBankAccount(final String createdByUser) {
        final var random = RandomString.make(3);
        return givenSomeTemporaryBankAccount(createdByUser, () ->
                hsOfficeBankAccount(
                        "some temp acc #" + random,
                        "DE41500105177739718697",
                        "INGDDEFFXXX"
                ));
    }

    void exactlyTheseBankAccountsAreReturned(
            final List<HsOfficeBankAccountEntity> actualResult,
            final String... bankaccountLabels) {
        assertThat(actualResult)
                .extracting(HsOfficeBankAccountEntity::getHolder)
                .containsExactlyInAnyOrder(bankaccountLabels);
    }

    void allTheseBankAccountsAreReturned(
            final List<HsOfficeBankAccountEntity> actualResult,
            final String... bankaccountLabels) {
        assertThat(actualResult)
                .extracting(HsOfficeBankAccountEntity::getHolder)
                .contains(bankaccountLabels);
    }
}
