package net.hostsharing.hsadminng.config;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;

import net.hostsharing.hsadminng.context.Context;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.val;

@SpringBootTest(classes = {
        MessagesResourceConfig.class,
        MessageTranslator.class
})
@ActiveProfiles("test")
@Tag("generalIntegrationTest")
class MessageTranslatorIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private Context contextMock; // avoiding dependency issues

    @AllArgsConstructor
    enum TestCases {
        ENGLISH_KNOWN(Locale.ENGLISH, "test.ponged-{0}--in-your-language",
                "ponged testUser - in English"),
        ENGLISH_UNKNOWN(Locale.ENGLISH, "test.ponged-{0}--unknown-key",
                "【⍰ponged testUser - unknown key⍰】"),
        ENGLISH_US(Locale.of("en", "US"), "test.ponged-{0}--in-your-language",
                "ponged testUser - in English"),
        ENGLISH_UK(Locale.of("en", "UK"), "test.ponged-{0}--in-your-language",
                "ponged testUser - in English"),
        GERMAN_KNOWN(Locale.GERMAN, "test.ponged-{0}--in-your-language",
                "ponged testUser - auf Deutsch"),
        FRENCH_UNKNOWN_BUT_ENGLISH_KNOWN(Locale.FRENCH, "test.ponged-{0}--in-your-language",
                "【⍰ponged testUser - in English⍰】"),
        FRENCH_UNKNOWN_AND_ENGLISH_UNKNOWN(Locale.FRENCH, "test.ponged-{0}--unknown-key",
                "【⍰ponged testUser - unknown key⍰】"),
        UNKNOWN_LOCALE_AND_ENGLISH_KNOWN(Locale.TRADITIONAL_CHINESE,
                "test.ponged-{0}--in-your-language", "【⍰ponged testUser - in English⍰】"),
        UNKNOWN_LOCALE_AND_ENGLISH_UNKNOWN(Locale.TRADITIONAL_CHINESE, "test.ponged-{0}--unknown-key",
                "【⍰ponged testUser - unknown key⍰】");
        final Locale locale;
        final String messageKey;
        final String expectedTranslation;
    }

    @ParameterizedTest
    @EnumSource(TestCases.class)
    void shouldHandleDifferentLocalesAppropriately(final TestCases testCase) {
        // given
        val messageTranslator = webApplicationContext.getBean(MessageTranslator.class);

        // when
        val result = messageTranslator.translateTo(testCase.locale, testCase.messageKey, "testUser");

        // then
        assertThat(result).isEqualTo(testCase.expectedTranslation);
    }
}
