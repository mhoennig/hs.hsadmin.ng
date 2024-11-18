package net.hostsharing.hsadminng.repr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TaggedNumberUnitTest {

    @Test
    void cropsProperTag() {
        assertThat(TaggedNumber.cropTag("P-", "P-12345")).isEqualTo(12345);
    }

    @Test
    void throwsExceptionForImproperTag() {
        final var exception = catchThrowable(() -> TaggedNumber.cropTag("P-", "X-12345"));
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getMessage()).isEqualTo("Expected P-... but got: X-12345");
    }

}
