package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.RetroactiveTranslatorWithPlaceholderSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// HOWTO translate messages with placeholders which got created without i18n support, in this case in a PostgreSQL constraint trigger
@Service
public class RbacTranslations implements RetroactiveTranslatorWithPlaceholderSupport {

    public static final String ERROR_403_PREFIX = "ERROR: [403] ";

    private static final List<TranslatableMessage> TRANSLATABLE_MESSAGES = List.of(
            new TranslatableMessage(
                    ERROR_403_PREFIX,
                    "^ERROR: \\[403] subject (.+) has no permission to assume role (.+)$",
                    "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}")
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
