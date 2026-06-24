package net.hostsharing.hsadminng.hs.scenarios;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TestReportUnitTest {

    @Test
    void rendersImportantShortTitleWordsInLowercase() throws NoSuchMethodException {
        final var scenarioMethod = testMethod("grantingAProjectAdminRoleToAGroup");

        assertThat(TestReport.reportTitle(scenarioMethod))
                .isEqualTo("10001: Granting a Project Admin Role to a Group");
    }

    @Test
    void keepsFirstAndLastTitleWordsCapitalized() throws NoSuchMethodException {
        final var scenarioMethod = testMethod("aProjectAdminRoleFor");

        assertThat(TestReport.reportTitle(scenarioMethod))
                .isEqualTo("10002: A Project Admin Role For");
    }

    private static Method testMethod(final String name) throws NoSuchMethodException {
        return TestReportUnitTest.class.getDeclaredMethod(name);
    }

    @Test
    @Order(10001)
    void grantingAProjectAdminRoleToAGroup() {
        // just a test-dummy to have the method name converted to a title
    }

    @Test
    @Order(10002)
    void aProjectAdminRoleFor() {
        // just a test-dummy to have the method name converted to a title
    }
}
