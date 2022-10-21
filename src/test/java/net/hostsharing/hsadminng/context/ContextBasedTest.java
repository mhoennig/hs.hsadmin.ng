package net.hostsharing.hsadminng.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ContextBasedTest {

    @Autowired
    Context context;

    TestInfo test;

    @BeforeEach
    void init(TestInfo testInfo) {
        this.test = testInfo;
    }

    protected void context(final String currentUser, final String assumedRoles) {
        context.define(test.getDisplayName(), null, currentUser, assumedRoles);
    }

    protected void context(final String currentUser) {
        context(currentUser, null);
    }
}
