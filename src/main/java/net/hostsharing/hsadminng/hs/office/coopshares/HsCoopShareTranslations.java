package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.RetroactiveTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// HOWTO translate messages which got created without i18n support, in this case in a PostgreSQL constraint trigger
@Service
public class HsCoopShareTranslations implements RetroactiveTranslator {

    public static final String ERROR_400_PREFIX = "ERROR: [400] ";

    @Autowired
    private MessageTranslator messageTranslator;

    @Override
    public boolean canTranslate(final String message) {
        return message.equals("ERROR: [400] coop shares transaction would result in a negative number of shares");
    }

    @Override
    public String translate(final String message) {
        return ERROR_400_PREFIX + messageTranslator.translate(message.substring(ERROR_400_PREFIX.length()));
    }
}
