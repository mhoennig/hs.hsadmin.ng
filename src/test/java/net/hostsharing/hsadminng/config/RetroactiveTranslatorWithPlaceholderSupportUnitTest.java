package net.hostsharing.hsadminng.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RetroactiveTranslatorWithPlaceholderSupportUnitTest {

    private final RecordingMessageTranslator messageTranslator = new RecordingMessageTranslator();

    @Test
    void defaultImplementationDoesNotTranslateAnything() {
        final var translator = new RetroactiveTranslatorWithPlaceholderSupport() {
        };

        assertThat(translator.getMessageTranslator()).isNull();
        assertThat(translator.getTranslatableMessages()).isEmpty();
        assertThat(translator.canTranslate("ERROR: [400] anything")).isFalse();
        assertThat(translator.translate("ERROR: [400] anything")).isEqualTo("ERROR: [400] anything");
    }

    @Test
    void translatableMessageCreatedFromPatternExposesItsValues() {
        final var pattern = Pattern.compile("^ERROR: \\[403] subject (.+) not allowed to access (.+)$");
        final var translatableMessage = new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                "ERROR: [403] ",
                pattern,
                "rbac.subject-{0}-not-allowed-to-access-{1}");

        assertThat(translatableMessage.messagePrefix()).isEqualTo("ERROR: [403] ");
        assertThat(translatableMessage.pattern()).isSameAs(pattern);
        assertThat(translatableMessage.messageKey()).isEqualTo("rbac.subject-{0}-not-allowed-to-access-{1}");
    }

    @Test
    void canTranslateMessageMatchingAnyRegisteredPattern() {
        final var translator = givenTranslator(
                new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                        "ERROR: [403] ",
                        "^ERROR: \\[403] subject (.+) not allowed to access (.+)$",
                        "rbac.subject-{0}-not-allowed-to-access-{1}"));

        assertThat(translator.canTranslate("ERROR: [403] subject cur-subject not allowed to access xyz")).isTrue();
        assertThat(translator.canTranslate("ERROR: [404] subject cur-subject not allowed to access xyz")).isFalse();
    }

    @Test
    void translatesUsingFirstMatchingMessageAndExtractsPlaceholderArguments() {
        final var translator = givenTranslator(
                new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                        "ERROR: [403] ",
                        "^ERROR: \\[403] unrelated (.+)$",
                        "rbac.unrelated-{0}"),
                new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                        "ERROR: [403] ",
                        "^ERROR: \\[403] subject (.+) not allowed to access (.+)$",
                        "rbac.subject-{0}-not-allowed-to-access-{1}"));

        final var translatedMessage = translator.translate(
                "ERROR: [403] subject cur-subject not allowed to access xyz");

        assertThat(translatedMessage).isEqualTo(
                "ERROR: [403] translated:rbac.subject-{0}-not-allowed-to-access-{1}"
                        + "[cur-subject, xyz]");
        assertThat(messageTranslator.messageKey).isEqualTo("rbac.subject-{0}-not-allowed-to-access-{1}");
        assertThat(messageTranslator.args)
                .containsExactly("cur-subject", "xyz");
    }

    @Test
    void translatesExactMessageEvenWithRegexCharsInKey() {
        final var translator = givenTranslator(new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                "ERROR: [400] ",
                "simple.key-with-[regex]-chars?"));

        assertThat(translator.canTranslate("ERROR: [400] simple.key-with-[regex]-chars?")).isTrue();
        assertThat(translator.canTranslate("ERROR: [400] simpleXkey-with-[regex]-chars?")).isFalse();
        assertThat(translator.translate("ERROR: [400] simple.key-with-[regex]-chars?"))
                .isEqualTo("ERROR: [400] translated:simple.key-with-[regex]-chars?[]");
        assertThat(messageTranslator.messageKey).isEqualTo("simple.key-with-[regex]-chars?");
        assertThat(messageTranslator.args).isEmpty();
    }

    @Test
    void returnsOriginalMessageIfNoRegisteredPatternMatches() {
        final var translator = givenTranslator(
                new RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage(
                        "ERROR: [403] ",
                        "^ERROR: \\[403] subject (.+) not allowed to access (.+)$",
                        "rbac.subject-{0}-not-allowed-to-access-{1}"));

        assertThat(translator.translate("ERROR: [404] subject cur-subject"))
                .isEqualTo("ERROR: [404] subject cur-subject");
        assertThat(messageTranslator.messageKey).isNull();
        assertThat(messageTranslator.args).isNull();
    }

    private RetroactiveTranslatorWithPlaceholderSupport givenTranslator(
            final RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage... translatableMessages) {
        return new TestTranslator(messageTranslator, List.of(translatableMessages));
    }

    private record TestTranslator(
            MessageTranslator messageTranslator,
            List<RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage> translatableMessages)
            implements RetroactiveTranslatorWithPlaceholderSupport {

        @Override
        public MessageTranslator getMessageTranslator() {
            return messageTranslator;
        }

        @Override
        public List<RetroactiveTranslatorWithPlaceholderSupport.TranslatableMessage> getTranslatableMessages() {
            return translatableMessages;
        }
    }

    private static class RecordingMessageTranslator extends MessageTranslator {

        private String messageKey;
        private Object[] args;

        @Override
        public String translate(final String messageKey, final Object... args) {
            this.messageKey = messageKey;
            this.args = args;
            return "translated:" + messageKey + Arrays.toString(args);
        }
    }
}
