package org.hostsharing.hsadminng.service.accessfilter;

import org.junit.Test;

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
    public void isAllowedToInit() {
    }

    @Test
    public void isAllowedToUpdate() {
    }

    @Test
    public void isAllowedToRead() {
    }
}
