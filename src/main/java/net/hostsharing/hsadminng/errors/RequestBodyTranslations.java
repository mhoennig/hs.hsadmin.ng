package net.hostsharing.hsadminng.errors;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.RetroactiveTranslatorWithPlaceholderSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// HOWTO translate technical request-body parse errors (here from Jackson) into human-readable messages
@Service
public class RequestBodyTranslations implements RetroactiveTranslatorWithPlaceholderSupport {

    private static final String NO_PREFIX = "";

    private static final List<TranslatableMessage> TRANSLATABLE_MESSAGES = List.of(
            new TranslatableMessage(
                    NO_PREFIX,
                    "(?s)^JSON parse error: Cannot deserialize value of type `java\\.lang\\.String` from Object value.*$",
                    "general.a-plain-text-value-was-expected-but-a-json-object-was-provided"),
            new TranslatableMessage(
                    NO_PREFIX,
                    "(?s)^JSON parse error: Cannot deserialize value of type `[^`]*Map[^`]*` from Array value.*$",
                    "general.a-json-object-with-key-value-pairs-was-expected-but-a-json-array-was-provided")
    );

    @Autowired
    private MessageTranslator messageTranslator;

    @Override
    public MessageTranslator getMessageTranslator() {
        return messageTranslator;
    }

    @Override
    public List<TranslatableMessage> getTranslatableMessages() {
        return TRANSLATABLE_MESSAGES;
    }
}
