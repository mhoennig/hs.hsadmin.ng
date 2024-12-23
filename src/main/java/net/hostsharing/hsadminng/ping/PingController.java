package net.hostsharing.hsadminng.ping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.validation.constraints.NotNull;

@Controller
public class PingController {

    @ResponseBody
    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping(
            @RequestHeader(name = "current-subject") @NotNull String currentSubject,
            @RequestHeader(name = "assumed-roles", required = false) String assumedRoles
    ) {
        return "pong " + currentSubject + "\n";
    }
}
