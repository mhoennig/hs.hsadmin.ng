// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.google.common.base.VerifyException;

import org.junit.Test;

import java.lang.reflect.Field;

public class RoleUnitTest {

    @Test
    public void allUserRolesShouldCoverSameRequiredRole() {
        assertThat(Role.HOSTMASTER.covers(Role.HOSTMASTER)).isTrue();
        assertThat(Role.ADMIN.covers(Role.ADMIN)).isTrue();
        assertThat(Role.SUPPORTER.covers(Role.SUPPORTER)).isTrue();

        assertThat(Role.CONTRACTUAL_CONTACT.covers(Role.CONTRACTUAL_CONTACT)).isTrue();
        assertThat(Role.FINANCIAL_CONTACT.covers(Role.FINANCIAL_CONTACT)).isTrue();
        assertThat(Role.TECHNICAL_CONTACT.covers(Role.TECHNICAL_CONTACT)).isTrue();

        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.ACTUAL_CUSTOMER_USER))).isTrue();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.ANY_CUSTOMER_USER))).isTrue();
    }

    @Test
    public void lowerUserRolesShouldNotCoverHigherRequiredRoles() {
        assertThat(Role.HOSTMASTER.covers(Role.NOBODY)).isFalse();
        assertThat(Role.ADMIN.covers(Role.HOSTMASTER)).isFalse();
        assertThat(Role.SUPPORTER.covers(Role.ADMIN)).isFalse();

        assertThat(Role.ANY_CUSTOMER_CONTACT.covers(Role.SUPPORTER)).isFalse();
        assertThat(Role.ANY_CUSTOMER_CONTACT.covers(Role.CONTRACTUAL_CONTACT)).isFalse();
        assertThat(Role.FINANCIAL_CONTACT.covers(Role.CONTRACTUAL_CONTACT)).isFalse();
        assertThat(Role.FINANCIAL_CONTACT.covers(Role.TECHNICAL_CONTACT)).isFalse();
        assertThat(Role.TECHNICAL_CONTACT.covers(Role.CONTRACTUAL_CONTACT)).isFalse();
        assertThat(Role.TECHNICAL_CONTACT.covers(Role.FINANCIAL_CONTACT)).isFalse();

        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.ANY_CUSTOMER_CONTACT))).isFalse();
        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.CONTRACTUAL_CONTACT))).isFalse();
        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.TECHNICAL_CONTACT))).isFalse();
        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.FINANCIAL_CONTACT))).isFalse();

        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.ACTUAL_CUSTOMER_USER))).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.ANY_CUSTOMER_CONTACT))).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.CONTRACTUAL_CONTACT))).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.TECHNICAL_CONTACT))).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.FINANCIAL_CONTACT))).isFalse();

        assertThat(Role.ANYBODY.covers((Role.ANY_CUSTOMER_USER))).isFalse();
    }

    @Test
    public void higherUserRolesShouldCoverLowerRequiredRoles() {
        assertThat(Role.HOSTMASTER.covers(Role.SUPPORTER)).isTrue();
        assertThat(Role.ADMIN.covers(Role.SUPPORTER)).isTrue();

        assertThat(Role.SUPPORTER.covers(Role.ANY_CUSTOMER_CONTACT)).isTrue();

        assertThat(Role.CONTRACTUAL_CONTACT.covers(Role.ANY_CUSTOMER_CONTACT)).isTrue();
        assertThat(Role.CONTRACTUAL_CONTACT.covers(Role.FINANCIAL_CONTACT)).isTrue();
        assertThat(Role.CONTRACTUAL_CONTACT.covers(Role.TECHNICAL_CONTACT)).isTrue();
        assertThat(Role.TECHNICAL_CONTACT.covers(Role.ANY_CUSTOMER_USER)).isTrue();

        assertThat(Role.ACTUAL_CUSTOMER_USER.covers((Role.ANY_CUSTOMER_USER))).isTrue();
        assertThat(Role.ANY_CUSTOMER_USER.covers((Role.ANYBODY))).isTrue();
    }

    @Test
    public void financialContactShouldNotCoverAnyCustomersUsersRoleRequirement() {
        assertThat(Role.FINANCIAL_CONTACT.covers(Role.ACTUAL_CUSTOMER_USER)).isFalse();
    }

    @Test
    public void ignoredCoversNothingAndIsNotCovered() {
        assertThat(Role.IGNORED.covers(Role.HOSTMASTER)).isFalse();
        assertThat(Role.IGNORED.covers(Role.ANYBODY)).isFalse();
        assertThat(Role.IGNORED.covers(Role.IGNORED)).isFalse();
        assertThat(Role.HOSTMASTER.covers(Role.IGNORED)).isFalse();
        assertThat(Role.ANYBODY.covers(Role.IGNORED)).isFalse();
    }

    @Test
    public void coversAny() {
        assertThat(Role.HOSTMASTER.coversAny(Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT)).isTrue();
        assertThat(Role.CONTRACTUAL_CONTACT.coversAny(Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT)).isTrue();
        assertThat(Role.FINANCIAL_CONTACT.coversAny(Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT)).isTrue();

        assertThat(Role.ANY_CUSTOMER_USER.coversAny(Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT)).isFalse();

        assertThat(catchThrowable(() -> Role.HOSTMASTER.coversAny())).isInstanceOf(VerifyException.class);
        assertThat(catchThrowable(() -> Role.HOSTMASTER.coversAny((Role[]) null))).isInstanceOf(VerifyException.class);
    }

    @Test
    public void isNdependend() {
        assertThat(Role.NOBODY.isIndependent()).isFalse();

        assertThat(Role.HOSTMASTER.isIndependent()).isTrue();
        assertThat(Role.ADMIN.isIndependent()).isTrue();
        assertThat(Role.SUPPORTER.isIndependent()).isTrue();

        assertThat(Role.CONTRACTUAL_CONTACT.isIndependent()).isFalse();
        assertThat(Role.FINANCIAL_CONTACT.isIndependent()).isFalse();
        assertThat(Role.ACTUAL_CUSTOMER_USER.isIndependent()).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.isIndependent()).isFalse();

        assertThat(Role.ANYBODY.isIndependent()).isTrue();
    }

    @Test
    public void isIgnored() {
        for (Role role : Role.values()) {
            if (role == Role.IGNORED) {
                assertThat(role.isIgnored()).isTrue();
            } else {
                assertThat(role.isIgnored()).isFalse();
            }
        }
    }

    @Test
    public void toBeIgnoredForUpdates() {
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithoutAccessForAnnotation)).isTrue();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotationToBeIgnoredForUpdates)).isTrue();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotationToBeIgnoredForUpdatesAmongOthers)).isFalse();
        assertThat(Role.toBeIgnoredForUpdates(someFieldWithAccessForAnnotation)).isFalse();
    }

    @Test
    public void isIndependent() {
        assertThat(Role.HOSTMASTER.isIndependent()).isTrue();
        assertThat(Role.SUPPORTER.isIndependent()).isTrue();

        assertThat(Role.CONTRACTUAL_CONTACT.isIndependent()).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.isIndependent()).isFalse();
    }

    @Test
    public void asAuthority() {
        assertThat(Role.HOSTMASTER.asAuthority()).isEqualTo("ROLE_HOSTMASTER");
        assertThat(Role.ADMIN.asAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.SUPPORTER.asAuthority()).isEqualTo("ROLE_SUPPORTER");
        assertThat(Role.CONTRACTUAL_CONTACT.asAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    public void isBroadest() {
        assertThat(Role.broadest(Role.HOSTMASTER, Role.CONTRACTUAL_CONTACT)).isEqualTo(Role.HOSTMASTER);
        assertThat(Role.broadest(Role.CONTRACTUAL_CONTACT, Role.HOSTMASTER)).isEqualTo(Role.HOSTMASTER);
        assertThat(Role.broadest(Role.CONTRACTUAL_CONTACT, Role.ANY_CUSTOMER_USER)).isEqualTo(Role.CONTRACTUAL_CONTACT);
    }

    @Test
    public void isAllowedToInit() {
        assertThat(Role.HOSTMASTER.isAllowedToInit(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Role.SUPPORTER.isAllowedToInit(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Role.ADMIN.isAllowedToInit(someFieldWithAccessForAnnotation)).isTrue();
    }

    @Test
    public void isAllowedToUpdate() {
        assertThat(Role.HOSTMASTER.isAllowedToUpdate(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Role.ANY_CUSTOMER_CONTACT.isAllowedToUpdate(someFieldWithAccessForAnnotation)).isFalse();
        assertThat(Role.SUPPORTER.isAllowedToUpdate(someFieldWithAccessForAnnotation)).isTrue();
    }

    @Test
    public void isAllowedToRead() {
        assertThat(Role.HOSTMASTER.isAllowedToRead(someFieldWithoutAccessForAnnotation)).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.isAllowedToRead(someFieldWithAccessForAnnotation)).isFalse();
        assertThat(Role.ANY_CUSTOMER_CONTACT.isAllowedToRead(someFieldWithAccessForAnnotation)).isTrue();
    }

    // --- only test fixture below ---

    static class TestDto {

        @AccessFor(init = Role.ADMIN, update = Role.SUPPORTER, read = Role.ANY_CUSTOMER_CONTACT)
        private Integer someFieldWithAccessForAnnotation;

        @AccessFor(update = Role.IGNORED, read = Role.ANY_CUSTOMER_CONTACT)
        private Integer someFieldWithAccessForAnnotationToBeIgnoredForUpdates;

        @AccessFor(update = { Role.IGNORED, Role.SUPPORTER }, read = Role.ANY_CUSTOMER_CONTACT)
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
