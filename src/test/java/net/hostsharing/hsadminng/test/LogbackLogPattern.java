package net.hostsharing.hsadminng.test;

import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import org.springframework.boot.logging.LogLevel;

public class LogbackLogPattern {

    private static final int LOGBACK_MAX_CLASSNAME_LENGTH = 36;

    /**
     * @return a regular expression for a log message
     */
    public static CharSequence of(
            final LogLevel logLevel,
            final Class<?> loggingClass,
            final String message) {
        final var abbreviator = new TargetLengthBasedClassNameAbbreviator(LOGBACK_MAX_CLASSNAME_LENGTH);
        final var shortenedClassName = abbreviator.abbreviate(loggingClass.getName());
        return logLevel + " [0-9]+ .* " + shortenedClassName +  " *: " + message;
    }
}
