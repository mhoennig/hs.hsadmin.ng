package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.PackagesApi;
import net.hostsharing.hsadminng.generated.api.v1.model.PackageResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;

import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class PackageController implements PackagesApi {

    @Autowired
    private Context context;

    @Autowired
    private PackageRepository packageRepository;

    @Override
    @Transactional
    public ResponseEntity<List<PackageResource>> listPackages(
        String userName,
        String assumedRoles,
        String name
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        final var result = packageRepository.findAllByOptionalNameLike(name);
        return ResponseEntity.ok(mapList(result, PackageResource.class));
    }

}
