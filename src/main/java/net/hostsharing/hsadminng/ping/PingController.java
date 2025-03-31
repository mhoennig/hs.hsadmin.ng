package net.hostsharing.hsadminng.ping;

import net.hostsharing.hsadminng.config.MessageTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class PingController {

    @Autowired
    private MessageTranslator messageTranslator;

    @ResponseBody
    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping() {
        final var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("pong {0} - in English", userName);
        return translatedMessage + "\n";
    }
}
