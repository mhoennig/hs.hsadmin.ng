package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;

@RestController
public class PackageController {

    @Autowired
    private Context context;

    @Autowired
    private PackageRepository packageRepository;

    @RequestMapping(value = "/api/packages", method = RequestMethod.GET)
    @Transactional
    public List<PackageEntity> listPackages(
        @RequestHeader(value = "current-user") String userName,
        @RequestHeader(value = "assumed-roles", required = false) String assumedRoles,
        @RequestParam(required = false) String name
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return packageRepository.findAllByOptionalNameLike(name);
    }

}
