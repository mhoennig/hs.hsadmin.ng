package net.hostsharing.hsadminng.rbac.subject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RbacSubjectEntityUnitTest {

    RbacSubjectEntity givenUser = new RbacSubjectEntity(UUID.randomUUID(), "test@example.org");

    @Test
    void generatedAccessCodeMatchesDefinedPattern() {
        final var givenAccessCode = givenUser.generateAccessCode();

        final var actual = givenAccessCode.matches("[0-9]{3}:[0-9]{3}");

        assertThat(actual).isTrue();
    }

    @Test
    void freshAccessCodeIsValid() {
        final var givenAccessCode = givenUser.generateAccessCode();

        final var actual = givenUser.isValidAccessCode(givenAccessCode, 4);

        assertThat(actual).isTrue();
    }

    @Test
    void recentEnoughAccessCodeIsValid() {
        final var givenAccessCode = givenUser.generateAccessCode(LocalDateTime.now().minus(4, ChronoUnit.HOURS));

        final var actual = givenUser.isValidAccessCode(givenAccessCode, 4);

        assertThat(actual).isTrue();
    }

    @Test
    void outdatedEnoughAccessCodeIsNotValid() {
        final var givenAccessCode = givenUser.generateAccessCode(LocalDateTime.now().minus(5, ChronoUnit.HOURS));

        final var actual = givenUser.isValidAccessCode(givenAccessCode, 4);

        assertThat(actual).isFalse();
    }

    @Test
    void noExceptionIsThrowIfMaxValidityIsNotExceeded() {
        final var givenAccessCode = givenUser.generateAccessCode();

        givenUser.isValidAccessCode(givenAccessCode, 24 * 21);
    }

    @Test
    void illegalArgumentExceptionIsThrowIfMaxValidityIsExceeded() {
        final var givenAccessCode = givenUser.generateAccessCode();

        assertThatThrownBy(() -> {
            givenUser.isValidAccessCode(givenAccessCode, 24 * 21 + 1);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max validity (21 days) exceeded.");
    }
}
