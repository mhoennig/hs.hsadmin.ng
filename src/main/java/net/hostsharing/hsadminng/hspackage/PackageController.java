package net.hostsharing.hsadminng.hspackage;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.transaction.Transactional;
import java.util.List;

@Controller
public class PackageController {

    @Autowired
    private Context context;

    @Autowired
    private PackageRepository packageRepository;

    @ResponseBody
    @RequestMapping(value = "/api/packages", method = RequestMethod.GET)
    @Transactional
    public List<PackageEntity> listPackages(
        @RequestHeader(value = "current-user") String userName,
        @RequestHeader(value = "assumed-roles", required = false) String assumedRoles
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return packageRepository.findAll();
    }

}
