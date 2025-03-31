package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.RetroactiveTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


// HOWTO translate messages which got created without i18n support, in this case in a PostgreSQL constraint trigger
@Service
public class HsHostingAssetTranslations implements RetroactiveTranslator {

    public static final String ERROR_400_PREFIX = "ERROR: [400] ";

    @Autowired
    private MessageTranslator messageTranslator;

    @Override
    public boolean canTranslate(final String message) {
        return message.equals("ERROR: [400] coop assets transaction would result in a negative balance of assets");
    }

    @Override
    public String translate(final String message) {
        // it's guaranteed to be the same message, for which canTranslate(...) returned true
        // and in this case it's just one
        return ERROR_400_PREFIX + messageTranslator.translate(message.substring(ERROR_400_PREFIX.length()));

        // HOWTO extract variable parts from a messages which got created without i18n support:
        //  final var regex = "(?<propertyName>[^ ]+) (?<propertyValue>.+) not found";
        //  final var pattern = Pattern.compile(regex);
        //  final var matcher = pattern.matcher(message);
        //
        //  if (matcher.matches()) {
        //      final var propertyName = matcher.group("propertyName");
        //      final var propertyValue = matcher.group("propertyValue");
        //      return messageTranslator.translate("", propertyName, propertyValue);
        //  }
    }
}
