package net.hostsharing.hsadminng.config;

/**
 * Translates messages from external sources that bypass our regular i18n flow.
 *
 * <p>Implement this as a Spring service for external messages, such as database or library errors,
 * which can be translated as a whole, thus don't contain values which need to replace placeholders.
 * The global error handler uses the first translator that accepts a message.
 *
 * <p>Use {@link RetroactiveTranslatorWithPlaceholderSupport} for messages with dynamic values.
 */
public interface RetroactiveTranslator {

    /**
     * Is called to check whehter this translator can translate a given message.
     *
     * @param message the original external message
     * @return {@code true} if this translator can translate the message
     */
    boolean canTranslate(final String message);

    /**
     * Is called to translate a message which was previously accepted by {@link #canTranslate(String)}.
     *
     * @param message the original external message
     * @return the translated message
     */
    String translate(final String message);
}
