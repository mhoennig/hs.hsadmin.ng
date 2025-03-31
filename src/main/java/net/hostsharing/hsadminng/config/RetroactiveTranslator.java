package net.hostsharing.hsadminng.config;

/**
 * Makes it possible to translate messages which got created by external sources (libraries, database, etc.)
 * without i18n support.
 */
public interface RetroactiveTranslator {

    boolean canTranslate(final String message);
    String translate(final String message);
}
