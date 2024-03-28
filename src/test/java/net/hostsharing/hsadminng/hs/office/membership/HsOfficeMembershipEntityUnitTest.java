package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PrePersist;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;

import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeMembershipEntityUnitTest {

    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");

    final HsOfficeMembershipEntity givenMembership = HsOfficeMembershipEntity.builder()
            .memberNumberSuffix("01")
            .partner(TEST_PARTNER)
            .validity(Range.closedInfinite(GIVEN_VALID_FROM))
            .build();

    @Test
    void toStringContainsAllProps() {
        final var result = givenMembership.toString();
        assertThat(result).isEqualTo("Membership(M-1000101, P-10001, [2020-01-01,))");
    }

    @Test
    void toShortStringContainsMemberNumberSuffixOnly() {
        final var result = givenMembership.toShortString();
        assertThat(result).isEqualTo("M-1000101");
    }

    @Test
    void getMemberNumberWithPartnerAndSuffix() {
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(1000101);
    }

    @Test
    void getMemberNumberWithPartnerButWithoutSuffix() {
        givenMembership.setMemberNumberSuffix(null);
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getMemberNumberWithoutPartnerButWithSuffix() {
        givenMembership.setPartner(null);
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getMemberNumberWithoutPartnerNumberButWithSuffix() {
        givenMembership.setPartner(HsOfficePartnerEntity.builder().build());
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getValidtyIfNull() {
        givenMembership.setValidity(null);
        final var result = givenMembership.getValidity();
        assertThat(result).isEqualTo(Range.infinite(LocalDate.class));
    }

    @Test
    void initializesReasonForTerminationInPrePersistIfNull() throws Exception {
        final var givenUninitializedMembership = new HsOfficeMembershipEntity();
        assertThat(givenUninitializedMembership.getReasonForTermination()).as("precondition failed").isNull();

        invokePrePersist(givenUninitializedMembership);
        assertThat(givenUninitializedMembership.getReasonForTermination()).isEqualTo(HsOfficeReasonForTermination.NONE);
    }

    @Test
    void doesNotOverwriteReasonForTerminationInPrePersistIfNotNull() throws Exception {
        givenMembership.setReasonForTermination(HsOfficeReasonForTermination.CANCELLATION);

        invokePrePersist(givenMembership);
        assertThat(givenMembership.getReasonForTermination()).isEqualTo(HsOfficeReasonForTermination.CANCELLATION);
    }

    @Test
    void settingValidFromKeepsValidTo() {
        givenMembership.setValidFrom(LocalDate.parse("2020-01-01"));
        assertThat(givenMembership.getValidFrom()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(givenMembership.getValidTo()).isNull();

    }

    @Test
    void settingValidToKeepsValidFrom() {
        givenMembership.setValidTo(LocalDate.parse("2024-12-31"));
        assertThat(givenMembership.getValidFrom()).isEqualTo(GIVEN_VALID_FROM);
        assertThat(givenMembership.getValidTo()).isEqualTo(LocalDate.parse("2024-12-31"));

    }

    private static void invokePrePersist(final HsOfficeMembershipEntity membershipEntity)
            throws IllegalAccessException, InvocationTargetException {
        final var prePersistMethod = Arrays.stream(HsOfficeMembershipEntity.class.getDeclaredMethods())
                .filter(f -> f.getAnnotation(PrePersist.class) != null)
                .findFirst();
        assertThat(prePersistMethod).as("@PrePersist method not found").isPresent();

        prePersistMethod.get().invoke(membershipEntity);
    }
}
