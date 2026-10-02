package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.core.env.PropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorSanitizerUnitTest {

    private ActuatorSanitizer actuatorSanitizer;

    @BeforeEach
    void setUp() {
        // Initialize with additional keys for testing
        final var additionalKeys = List.of("customSecret", "^custom[._]regex.*$");
        actuatorSanitizer = new ActuatorSanitizer(additionalKeys);
    }

    @Test
    void testSanitizesDefaultKeys() {
        final var data = createSanitizableData("password", "my-secret-password");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo(SanitizableData.SANITIZED_VALUE);
    }

    @Test
    void testSanitizesCustomKey() {
        final var data = createSanitizableData("customSecret", "my-custom-secret");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo(SanitizableData.SANITIZED_VALUE);
    }

    @Test
    void testSanitizesCustomRegexKey() {
        final var data = createSanitizableData("custom.regex.key", "my-custom-regex-value");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo(SanitizableData.SANITIZED_VALUE);
    }

    @Test
    void aPlainKeyMatchesAsSuffix() {
        final var data = createSanitizableData("myCustomSecret", "my-custom-secret");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo(SanitizableData.SANITIZED_VALUE);
    }

    @Test
    void aRegexKeyMatchesAsAWholeNotAsSuffix() {
        val sanitizer = new ActuatorSanitizer(List.of("secret+key"));

        assertThat(sanitizer.apply(createSanitizableData("secretkey", "value")).getValue())
                .isEqualTo(SanitizableData.SANITIZED_VALUE);
        assertThat(sanitizer.apply(createSanitizableData("mysecretkey", "value")).getValue())
                .isEqualTo("value");
    }

    @Test
    void testSanitizesUriWithUserInfo() {
        final var data = createSanitizableData("uri", "http://user:password@host.com");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo("http://user:******@host.com");
    }

    @Test
    void testDoesNotSanitizeIrrelevantKey() {
        final var data = createSanitizableData("irrelevantKey", "non-sensitive-value");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo("non-sensitive-value");
    }

    @Test
    void testHandlesNullValue() {
        final var data = createSanitizableData("password", null);
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isNull();
    }

    @Test
    void testHandlesMultipleUris() {
        final var data = createSanitizableData(
                "uris",
                "http://user1:password1@host1.com,http://user2:geheim@host2.com,http://user2@host2.com");
        final var sanitizedData = actuatorSanitizer.apply(data);

        assertThat(sanitizedData.getValue()).isEqualTo(
                "http://user1:******@host1.com,http://user2:******@host2.com,http://user2@host2.com");
    }

    /**
     * Utility method to create a SanitizableData instance for testing.
     */
    private SanitizableData createSanitizableData(final String key, final String value) {
        final var dummyPropertySource = new PropertySource<>("testSource") {

            @Override
            public Object getProperty(String name) {
                return null; // No real property resolution needed for this test
            }
        };
        return new SanitizableData(dummyPropertySource, key, value);
    }
}
