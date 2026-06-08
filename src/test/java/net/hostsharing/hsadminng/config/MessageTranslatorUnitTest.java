package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MessageTranslatorUnitTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final MessageSource messageSource = mock(MessageSource.class);
    private final MessageTranslator translator = new MessageTranslator();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(translator, "httpRequest", request);
        ReflectionTestUtils.setField(translator, "messageSource", messageSource);
    }

    @Test
    void translateUsesRequestLocale() {
        // given
        given(request.getLocale()).willReturn(Locale.GERMAN);
        given(messageSource.getMessage(eq("some.key"), any(Object[].class), eq(Locale.GERMAN)))
                .willReturn("translated");

        // when
        val result = translator.translate("some.key", "arg");

        // then
        assertThat(result).isEqualTo("translated");
    }

    @Test
    void translateToDecoratesFallbackIfTranslationIsMissing() {
        // given
        given(messageSource.getMessage("error.some-missing--message", new Object[] { "arg" }, Locale.GERMAN))
                .willThrow(new IllegalArgumentException("missing translation"));
        given(messageSource.getMessage(
                eq("error.some-missing--message"),
                any(Object[].class),
                eq("some missing - message"),
                eq(Locale.ENGLISH)))
                .willReturn("some missing - message");

        // when
        val result = translator.translateTo(Locale.GERMAN, "error.some-missing--message", "arg");

        // then
        assertThat(result).isEqualTo("【⍰some missing - message⍰】");
    }

    @Test
    void translateToUsesMessageKeyWithoutPrefixAsFallbackToEnglish() {
        // given
        given(messageSource.getMessage(eq("plain-message"), any(Object[].class), eq(Locale.GERMAN)))
                .willThrow(new IllegalArgumentException("missing translation"));
        given(messageSource.getMessage(
                eq("plain-message"),
                any(),
                eq("plain message"),
                eq(Locale.ENGLISH)))
                .willReturn("plain message");

        // when
        val result = translator.translateTo(Locale.GERMAN, "plain-message");

        // then
        assertThat(result).isEqualTo("【⍰plain message⍰】");
    }
}
