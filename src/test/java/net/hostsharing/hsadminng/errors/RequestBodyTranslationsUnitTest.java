package net.hostsharing.hsadminng.errors;

import net.hostsharing.hsadminng.config.MessageTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestBodyTranslationsUnitTest {

    private static final String OBJECT_INSTEAD_OF_STRING_MESSAGE =
            "JSON parse error: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)";

    private static final String ARRAY_INSTEAD_OF_MAP_MESSAGE =
            "JSON parse error: Cannot deserialize value of type `java.util.LinkedHashMap<java.lang.String,java.lang.String>` from Array value (token `JsonToken.START_ARRAY`)";

    @Mock
    private MessageTranslator messageTranslator;

    @InjectMocks
    private RequestBodyTranslations translations;

    @Test
    void canTranslateObjectInsteadOfStringMessage() {
        assertThat(translations.canTranslate(OBJECT_INSTEAD_OF_STRING_MESSAGE)).isTrue();
    }

    @Test
    void canTranslateArrayInsteadOfMapMessage() {
        assertThat(translations.canTranslate(ARRAY_INSTEAD_OF_MAP_MESSAGE)).isTrue();
    }

    @Test
    void cannotTranslateOtherMessages() {
        assertThat(translations.canTranslate("JSON parse error: something else entirely")).isFalse();
    }

    @Test
    void translatesObjectInsteadOfStringMessage() {
        // given
        when(messageTranslator.translate("general.a-plain-text-value-was-expected-but-a-json-object-was-provided"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = translations.translate(OBJECT_INSTEAD_OF_STRING_MESSAGE);

        // then
        assertThat(translatedMessage).isEqualTo("translated message");
        verify(messageTranslator).translate("general.a-plain-text-value-was-expected-but-a-json-object-was-provided");
    }

    @Test
    void translatesArrayInsteadOfMapMessage() {
        // given
        when(messageTranslator.translate("general.a-json-object-with-key-value-pairs-was-expected-but-a-json-array-was-provided"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = translations.translate(ARRAY_INSTEAD_OF_MAP_MESSAGE);

        // then
        assertThat(translatedMessage).isEqualTo("translated message");
        verify(messageTranslator).translate("general.a-json-object-with-key-value-pairs-was-expected-but-a-json-array-was-provided");
    }
}
