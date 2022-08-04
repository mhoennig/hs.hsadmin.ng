package net.hostsharing.hsadminng;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {

    @ResponseBody
    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping() {
        return "pong\n";
    }
}
