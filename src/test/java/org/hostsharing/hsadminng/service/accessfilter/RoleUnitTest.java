// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.hostsharing.hsadminng.security.AuthoritiesConstants;
import org.hostsharing.hsadminng.service.accessfilter.Role.*;

import com.google.common.base.VerifyException;

import org.junit.Test;

import java.lang.reflect.Field;

public class RoleUnitTest {

    @Test
    public void allUserRolesShouldCoverSameRequiredRole() {
        assertThat(Hostmaster.ROLE.covers(Hostmaster.class)).isTrue();
        assertThat(Admin.ROLE.covers(Admin.class)).isTrue();
        assertThat(Supporter.ROLE.covers(Supporter.class)).isTrue();

        assertThat(Role.CustomerContractualContact.ROLE.covers(Role.CustomerContractualContact.class)).isTrue();
        assertThat(CustomerFinancialContact.ROLE.covers(CustomerFinancialContact.class)).isTrue();
        assertThat(CustomerTechnicalContact.ROLE.covers(CustomerTechnicalContact.class)).isTrue();

        assertThat(ActualCustomerUser.ROLE.covers((ActualCustomerUser.class))).isTrue();
        assertThat(AnyCustomerUser.ROLE.covers((Role.AnyCustomerUser.class))).isTrue();
    }

    @Test
    public void lowerUserRolesShouldNotCoverHigherRequiredRoles() {
        assertThat(Hostmaster.ROLE.covers(Nobody.class)).isFalse();
        assertThat(Admin.ROLE.covers(Hostmaster.class)).isFalse();
        assertThat(Supporter.ROLE.covers(Admin.class)).isFalse();

        assertThat(AnyCustomerContact.ROLE.covers(Supporter.class)).isFalse();
        assertThat(AnyCustomerContact.ROLE.covers(Role.CustomerContractualContact.class)).isFalse();
        assertThat(CustomerFinancialContact.ROLE.covers(Role.CustomerContractualContact.class)).isFalse();
        assertThat(CustomerFinancialContact.ROLE.covers(CustomerTechnicalContact.class)).isFalse();
        assertThat(CustomerTechnicalContact.ROLE.covers(Role.CustomerContractualContact.class)).isFalse();
        assertThat(CustomerTechnicalContact.ROLE.covers(CustomerFinancialContact.class)).isFalse();

        assertThat(ActualCustomerUser.ROLE.covers((AnyCustomerContact.class))).isFalse();
        assertThat(ActualCustomerUser.ROLE.covers((Role.CustomerContractualContact.class))).isFalse();
        assertThat(ActualCustomerUser.ROLE.covers((CustomerTechnicalContact.class))).isFalse();
        assertThat(ActualCustomerUser.ROLE.covers((CustomerFinancialContact.class))).isFalse();

        assertThat(AnyCustomerUser.ROLE.covers((ActualCustomerUser.class))).isFalse();
        assertThat(AnyCustomerUser.ROLE.covers((AnyCustomerContact.class))).isFalse();
        assertThat(AnyCustomerUser.ROLE.covers((Role.CustomerContractualContact.class))).isFalse();
        assertThat(AnyCustomerUser.ROLE.covers((CustomerTechnicalContact.class))).isFalse();
        assertThat(AnyCustomerUser.ROLE.covers((CustomerFinancialContact.class))).isFalse();

        assertThat(Anybody.ROLE.covers((Role.AnyCustomerUser.class))).isFalse();
    }

    @Test
    public void higherUserRolesShouldCoverLowerRequiredRoles() {
        assertThat(Hostmaster.ROLE.covers(Supporter.class)).isTrue();
        assertThat(Admin.ROLE.covers(Supporter.class)).isTrue();

        assertThat(Supporter.ROLE.covers(AnyCustomerContact.class)).isTrue();

        assertThat(Role.CustomerContractualContact.ROLE.covers(AnyCustomerContact.class)).isTrue();
        assertThat(Role.CustomerContractualContact.ROLE.covers(CustomerFinancialContact.class)).isTrue();
        assertThat(Role.CustomerContractualContact.ROLE.covers(CustomerTechnicalContact.class)).isTrue();
        assertThat(CustomerTechnicalContact.ROLE.covers(Role.AnyCustomerUser.class)).isTrue();

        assertThat(ActualCustomerUser.ROLE.covers((Role.AnyCustomerUser.class))).isTrue();
        assertThat(AnyCustomerUser.ROLE.covers((Anybody.class))).isTrue();
    }

    @Test
    public void financialContactShouldNotCoverAnyOtherRealRoleRequirement() {
        assertThat(CustomerFinancialContact.ROLE.covers(Role.AnyCustomerUser.class)).isFalse();
        assertThat(CustomerFinancialContact.ROLE.covers(ActualCustomerUser.class)).isFalse();
        assertThat(CustomerFinancialContact.ROLE.covers(Role.AnyCustomerUser.class)).isFalse();
    }

    @Test
    public void ignoredCoversNothingAndIsNotCovered() {
        assertThat(Ignored.ROLE.covers(Hostmaster.class)).isFalse();
        assertThat(Ignored.ROLE.covers(Anybody.class)).isFalse();
        assertThat(Ignored.ROLE.covers(Ignored.class)).isFalse();
        assertThat(Hostmaster.ROLE.covers(Ignored.class)).isFalse();
        assertThat(Anybody.ROLE.covers(Ignored.class)).isFalse();
    }

    @Test
    public void coversAny() {
        assertThat(Hostmaster.ROLE.coversAny(Role.CustomerContractualContact.class, CustomerFinancialContact.class)).isTrue();
        assertThat(
                Role.CustomerContractualContact.ROLE.coversAny(
                        Role.CustomerContractualContact.class,
                        CustomerFinancialContact.class))
                                .isTrue();
        assertThat(
                CustomerFinancialContact.ROLE.coversAny(
                        Role.CustomerContractualContact.class,
                        CustomerFinancialContact.class))
                                .isTrue();

        assertThat(Role.AnyCustomerUser.ROLE.coversAny(Role.CustomerContractualContact.class, CustomerFinancialContact.class))
                .isFalse();

        assertThat(catchThrowable(Hostmaster.ROLE::coversAny)).isInstanceOf(VerifyException.class);
        assertThat(
                catchThrowable(
                        () -> Hostmaster.ROLE.coversAny(
                                (Class<Role>[]) null))).isInstanceOf(VerifyException.class);
    }

    @Test
    public void toBeIgnoredForUpdates() {
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithoutAccessForAnnotation)).isTrue();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotationToBeIgnoredForUpdates)).isTrue();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers)).isFalse();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotation)).isFalse();
    }

    @Test
    public void getAuthority() {
        assertThat(Nobody.ROLE.authority()).isEqualTo(AuthoritiesConstants.USER);
        assertThat(Hostmaster.ROLE.authority()).isEqualTo(AuthoritiesConstants.HOSTMASTER);
        assertThat(Admin.ROLE.authority()).isEqualTo(AuthoritiesConstants.ADMIN);
        assertThat(Supporter.ROLE.authority()).isEqualTo(AuthoritiesConstants.SUPPORTER);
        assertThat(Role.CustomerContractualContact.ROLE.authority()).isEqualTo(AuthoritiesConstants.USER);
        assertThat(Anybody.ROLE.authority()).isEqualTo(AuthoritiesConstants.ANONYMOUS);
    }

    @Test
    public void isBroadest() {
        assertThat(Role.broadest(Hostmaster.ROLE, Role.CustomerContractualContact.ROLE)).isEqualTo(Hostmaster.ROLE);
        assertThat(Role.broadest(Role.CustomerContractualContact.ROLE, Hostmaster.ROLE)).isEqualTo(Hostmaster.ROLE);
        assertThat(Role.broadest(Role.CustomerContractualContact.ROLE, Role.AnyCustomerUser.ROLE))
                .isEqualTo(Role.CustomerContractualContact.ROLE);
    }

    @Test
    public void isAllowedToInit() {
        assertThat(Hostmaster.ROLE.isAllowedToInit(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Supporter.ROLE.isAllowedToInit(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Admin.ROLE.isAllowedToInit(someFieldWithAccessForAnnotation)).isTrue();
    }

    @Test
    public void isAllowedToUpdate() {
        assertThat(Hostmaster.ROLE.isAllowedToUpdate(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(AnyCustomerContact.ROLE.isAllowedToUpdate(someFieldWithAccessForAnnotation)).isFalse();
        assertThat(Supporter.ROLE.isAllowedToUpdate(someFieldWithAccessForAnnotation)).isTrue();
    }

    @Test
    public void isAllowedToRead() {
        assertThat(Hostmaster.ROLE.isAllowedToRead(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Role.AnyCustomerUser.ROLE.isAllowedToRead(someFieldWithAccessForAnnotation)).isFalse();
        assertThat(AnyCustomerContact.ROLE.isAllowedToRead(someFieldWithAccessForAnnotation)).isTrue();
    }

    // --- only test fixture below ---

    private static class TestDto {

        @AccessFor(init = Admin.class, update = Supporter.class, read = AnyCustomerContact.class)
        private Integer someFieldWithAccessForAnnotation;

        @AccessFor(update = Ignored.class, read = AnyCustomerContact.class)
        private Integer someFieldWithAccessForAnnotationToBeIgnoredForUpdates;

        @AccessFor(update = { Ignored.class, Supporter.class }, read = AnyCustomerContact.class)
        private Integer someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers;

        private Integer someFieldWithoutAccessForAnnotation;
    }

    private static Field someFieldWithoutAccessForAnnotation;
    private static Field someFieldWithAccessForAnnotationToBeIgnoredForUpdates;
    private static Field someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers;
    private static Field someFieldWithAccessForAnnotation;

    static {
        try {
            someFieldWithoutAccessForAnnotation = TestDto.class.getDeclaredField("someFieldWithoutAccessForAnnotation");
            someFieldWithAccessForAnnotationToBeIgnoredForUpdates = TestDto.class
                    .getDeclaredField("someFieldWithAccessForAnnotationToBeIgnoredForUpdates");
            someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers = TestDto.class
                    .getDeclaredField("someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers");
            someFieldWithAccessForAnnotation = TestDto.class.getDeclaredField("someFieldWithAccessForAnnotation");
        } catch (NoSuchFieldException e) {
            throw new AssertionError("precondition failed", e);
        }
    }
}
