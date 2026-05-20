package net.hostsharing.hsadminng.config;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;

/**
 * Translates external messages that contain dynamic values,
 * which need to replace placeholders in the translation templates.
 *
 * <p>Implement this as a Spring service when a message includes values such as names, roles,
 * identifiers or amounts. Register one {@link TranslatableMessage} per supported message shape.
 *
 * <p>Capture groups in the pattern become {@code {0}}, {@code {1}}, ... arguments of the translated message:
 * <pre>{@code
 * new TranslatableMessage(
 *         "ERROR: [403] ",
 *         "^ERROR: \\[403] subject (.+) has no permission to assume role (.+)$",
 *         "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}")
 * }</pre>
 */
public interface RetroactiveTranslatorWithPlaceholderSupport extends RetroactiveTranslator {

    /**
     * In your Spring service implementation,
     * overwrite this method to provide the translator used to resolve message keys.
     *
     * @return the message translator to use; implementations must override this method
     */
    default MessageTranslator getMessageTranslator() {
        return null;
    }

    /**
     * In your Spring service implementation,
     * overwrite this method to provide the message shapes supported by this translator.
     * <p>
     * More specific patterns should come before more general patterns.
     *
     * @return the supported messages; implementations must override this method
     */
    default List<TranslatableMessage> getTranslatableMessages() {
        return emptyList();
    }

    /**
     * Checks whether any registered message shape matches the message.
     * Usually does not need to be overridden by implementation service anymore.
     *
     * @param message the original external message
     * @return {@code true} if this translator can translate the message
     */
    @Override
    default boolean canTranslate(final String message) {
        return getTranslatableMessages().stream()
                .anyMatch(translatableMessage -> translatableMessage.pattern().matcher(message).matches());
    }

    /**
     * Translates the message using the first matching message shape.
     * Usually does not need to be overridden by implementation service anymore.
     *
     * @param message the original external message
     * @return the translated message, or the original message if no pattern matches
     */
    @Override
    default String translate(final String message) {
        for (final var translatableMessage : getTranslatableMessages()) {
            final var matcher = translatableMessage.pattern().matcher(message);

            if (matcher.matches()) {
                final var args = IntStream.rangeClosed(1, matcher.groupCount())
                        .mapToObj(matcher::group)
                        .toArray();
                return translatableMessage.messagePrefix() +
                        getMessageTranslator().translate(translatableMessage.messageKey(), args);
            }
        }

        return message;
    }

    /**
     * Defines one supported external message shape.
     */
    class TranslatableMessage {

        private final String messagePrefix;
        private final Pattern pattern;
        private final String messageKey;

        /**
         * Creates a descriptor with a precompiled pattern.
         *
         * @param messagePrefix the prefix to keep in the translated message, e.g. {@code ERROR: [403] }
         * @param pattern the pattern for the full original message
         * @param messageKey the key to resolve via {@link MessageTranslator}
         */
        public TranslatableMessage(
                final String messagePrefix,
                final Pattern pattern,
                final String messageKey) {
            this.messagePrefix = messagePrefix;
            this.pattern = pattern;
            this.messageKey = messageKey;
        }

        /**
         * Creates a descriptor for messages that equal {@code messagePrefix + messageKey}.
         *
         * @param messagePrefix the prefix to keep in the translated message, e.g. {@code ERROR: [400] }
         * @param messageKey the key to resolve via {@link MessageTranslator}
         */
        public TranslatableMessage(final String messagePrefix, final String messageKey) {
            this(
                    messagePrefix,
                    Pattern.compile("^" + Pattern.quote(messagePrefix + messageKey) + "$"),
                    messageKey);
        }

        /**
         * Creates a descriptor from a regular expression.
         *
         * @param messagePrefix the prefix to keep in the translated message, e.g. {@code ERROR: [403] }
         * @param regex the regular expression for the full original message
         * @param messageKey the key to resolve via {@link MessageTranslator}
         */
        public TranslatableMessage(final String messagePrefix, final String regex, final String messageKey) {
            this(messagePrefix, Pattern.compile(regex), messageKey);
        }

        /**
         * Returns the prefix kept in the translated message.
         *
         * @return the message prefix, e.g. {@code ERROR: [403] }
         */
        public String messagePrefix() {
            return messagePrefix;
        }

        /**
         * Returns the pattern for the original message.
         *
         * @return the message pattern
         */
        public Pattern pattern() {
            return pattern;
        }

        /**
         * Returns the message key.
         *
         * @return the translation key
         */
        public String messageKey() {
            return messageKey;
        }
    }
}
