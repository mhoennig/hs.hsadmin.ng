package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.PackagesApi;
import net.hostsharing.hsadminng.generated.api.v1.model.PackageResource;
import net.hostsharing.hsadminng.generated.api.v1.model.PackageUpdateResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class PackageController implements PackagesApi {

    @Autowired
    private Context context;

    @Autowired
    private PackageRepository packageRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<PackageResource>> listPackages(
        String currentUser,
        String assumedRoles,
        String name
    ) {
        context.register(currentUser, assumedRoles);

        final var result = packageRepository.findAllByOptionalNameLike(name);
        return ResponseEntity.ok(mapList(result, PackageResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<PackageResource> updatePackage(
        final String currentUser,
        final String assumedRoles,
        final UUID packageUuid,
        final PackageUpdateResource body) {

        context.register(currentUser, assumedRoles);

        final var current = packageRepository.findByUuid(packageUuid);
        OptionalFromJson.of(body.getDescription()).ifPresent(current::setDescription);
        final var saved = packageRepository.save(current);
        final var mapped = map(saved, PackageResource.class);
        return ResponseEntity.ok(mapped);
    }
}
