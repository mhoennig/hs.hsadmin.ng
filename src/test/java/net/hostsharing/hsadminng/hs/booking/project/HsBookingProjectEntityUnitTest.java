package net.hostsharing.hsadminng.hs.booking.project;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.PROJECT_TEST_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

class HsBookingProjectEntityUnitTest {

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {
        final var result = PROJECT_TEST_ENTITY.toString();

        assertThat(result).isEqualTo("HsBookingProject(D-1234500, test project)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {
        final var result = PROJECT_TEST_ENTITY.toShortString();

        assertThat(result).isEqualTo("D-1234500:test project");
    }
}
