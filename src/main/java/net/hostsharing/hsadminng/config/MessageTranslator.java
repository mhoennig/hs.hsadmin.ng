package net.hostsharing.hsadminng.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@Service
@RequestScope
@Slf4j
public class MessageTranslator {

    @Autowired
    private HttpServletRequest httpRequest;

    @Autowired
    private MessageSource messageSource;

    public String translateTo(final Locale locale, final String messageKey, final Object... args) {
        try {
            // we don't use the method which also takes a default message right away ...
            val translatedMessage = messageSource.getMessage(messageKey, args, locale);
            return translatedMessage;
        } catch (final Exception e) {
            // ... but log the missing translation ...
            log.error("Missing translation for message key '{}' in locale '{}'", messageKey, locale, e);

            // and decorate the default message to mark it as not really translated:
            val defaultMessage = messageKey.substring(messageKey.indexOf('.') + 1)
                    .replaceAll("--+", " - ")
                    .replaceAll("(?<! )-(?! )", " ")
                    .replace("'", "''");
            val fallbackMessage = messageSource.getMessage(messageKey, args, defaultMessage, Locale.ENGLISH);
            return decorateMissingTranslation(fallbackMessage);
        }
    }

    private static String decorateMissingTranslation(final String translatedMessage) {
        return "【⍰" + translatedMessage + "⍰】";
    }

    public String translate(final String messageKey, final Object... args) {
        return translateTo(httpRequest.getLocale(), messageKey, args);
    }
}
