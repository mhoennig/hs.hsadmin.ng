package net.hostsharing.hsadminng.context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = Context.class)
@DirtiesContext
class ContextIntegrationTests {

    @Autowired
    private Context context;

    @Test
    @Transactional
    void setCurrentUser() {
        context.setCurrentUser("mike@hostsharing.net");

        final var currentUser = context.getCurrentUser();
        assertThat(currentUser).isEqualTo("mike@hostsharing.net");

        final var assumedRoles = context.getAssumedRoles();
        assertThat(assumedRoles).isEmpty();
    }

    @Test
    @Transactional
    void assumeRoles() {
        context.setCurrentUser("mike@hostsharing.net");
        context.assumeRoles("customer#aaa.owner;customer#aab.owner");

        final var currentUser = context.getCurrentUser();
        assertThat(currentUser).isEqualTo("mike@hostsharing.net");

        final var assumedRoles = context.getAssumedRoles();
        assertThat(assumedRoles).containsExactlyInAnyOrder("customer#aaa.owner", "customer#aab.owner");
    }
}
