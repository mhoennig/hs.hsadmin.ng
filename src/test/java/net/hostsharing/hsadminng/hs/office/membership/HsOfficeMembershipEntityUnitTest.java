package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import org.junit.jupiter.api.Test;

import javax.persistence.PrePersist;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.testDebitor;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.testPartner;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeMembershipEntityUnitTest {

    final HsOfficeMembershipEntity givenMembership = HsOfficeMembershipEntity.builder()
            .memberNumber(10001)
            .partner(testPartner)
            .mainDebitor(testDebitor)
            .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
            .build();

    @Test
    void toStringContainsAllProps() {
        final var result = givenMembership.toString();

        assertThat(result).isEqualTo("Membership(10001, Test Ltd., 10001, [2020-01-01,))");
    }

    @Test
    void toShortStringContainsMemberNumberOnly() {
        final var result = givenMembership.toShortString();

        assertThat(result).isEqualTo("10001");
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

    private static void invokePrePersist(final HsOfficeMembershipEntity membershipEntity)
            throws IllegalAccessException, InvocationTargetException {
        final var prePersistMethod = Arrays.stream(HsOfficeMembershipEntity.class.getDeclaredMethods())
                .filter(f -> f.getAnnotation(PrePersist.class) != null)
                .findFirst();
        assertThat(prePersistMethod).as("@PrePersist method not found").isPresent();

        prePersistMethod.get().invoke(membershipEntity);
    }
}
