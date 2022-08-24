package net.hostsharing.hsadminng.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextBasedTest {

    @Autowired
    Context context;

    TestInfo test;

    @BeforeEach
    void init(TestInfo testInfo) {
        this.test = testInfo;
    }

    protected void context(final String currentUser, final String assumedRoles) {
        context.setCurrentTask(test.getDisplayName());

        context.setCurrentUser(currentUser);
        assertThat(context.getCurrentUser()).as("precondition").isEqualTo(currentUser);

        if (assumedRoles != null) {
            context.assumeRoles(assumedRoles);
            assertThat(context.getAssumedRoles()).as("precondition").containsExactly(assumedRoles.split(";"));
//        } else {
//            context.assumeNoSpecialRole();
        }
    }

    protected void context(final String currentUser) {
        context(currentUser, null);
    }
}
