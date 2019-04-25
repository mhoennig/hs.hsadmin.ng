package org.hostsharing.hsadminng.service.accessfilter;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void isIndependent() {
        assertThat(Role.HOSTMASTER.isIndependent()).isTrue();
        assertThat(Role.SUPPORTER.isIndependent()).isTrue();

        assertThat(Role.CONTRACTUAL_CONTACT.isIndependent()).isFalse();
        assertThat(Role.ANY_CUSTOMER_USER.isIndependent()).isFalse();
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

        private Integer someFieldWithoutAccessForAnnotation;
    }

    private static Field someFieldWithoutAccessForAnnotation;
    private static Field someFieldWithAccessForAnnotation;
    static {
        try {
            someFieldWithoutAccessForAnnotation = TestDto.class.getDeclaredField("someFieldWithoutAccessForAnnotation");
            someFieldWithAccessForAnnotation = TestDto.class.getDeclaredField("someFieldWithAccessForAnnotation");
        } catch (NoSuchFieldException e) {
            throw new AssertionError("precondition failed", e);
        }
    }
}
