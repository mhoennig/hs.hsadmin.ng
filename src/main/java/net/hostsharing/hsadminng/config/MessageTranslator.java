package net.hostsharing.hsadminng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@Service
@RequestScope
public class MessageTranslator {

    @Autowired
    private HttpServletRequest httpRequest;

    @Autowired
    private MessageSource messageSource;

    public String translateTo(final Locale locale, final String messageKey, final Object... args) {
        try {
            // we don't use the method which also takes a default message right away ...
            final var translatedMessage = messageSource.getMessage(messageKey, args, locale);
            return translatedMessage;
        } catch (final Exception e) {
            final var defaultMessage = messageKey.replace("'", "''");
            final var translatedMessage = messageSource.getMessage(messageKey, args, defaultMessage, locale);
            if (locale != Locale.ENGLISH) {
                // ... because we want to add a hint that the translation is missing, even if placeholders got replaced
                return translatedMessage + " [" + locale + " translation missing]";
            }
            return translatedMessage;
        }
    }

    public String translate(final String messageKey, final Object... args) {
        return translateTo(httpRequest.getLocale(), messageKey, args);
    }
}
