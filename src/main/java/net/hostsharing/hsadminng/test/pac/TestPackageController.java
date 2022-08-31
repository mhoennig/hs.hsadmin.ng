package net.hostsharing.hsadminng.test.pac;

import net.hostsharing.hsadminng.OptionalFromJson;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.TestPackagesApi;
import net.hostsharing.hsadminng.generated.api.v1.model.TestPackageResource;
import net.hostsharing.hsadminng.generated.api.v1.model.TestPackageUpdateResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController
public class TestPackageController implements TestPackagesApi {

    @Autowired
    private Context context;

    @Autowired
    private TestPackageRepository testPackageRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<TestPackageResource>> listPackages(
            String currentUser,
            String assumedRoles,
            String name
    ) {
        context.define(currentUser, assumedRoles);

        final var result = testPackageRepository.findAllByOptionalNameLike(name);
        return ResponseEntity.ok(mapList(result, TestPackageResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<TestPackageResource> updatePackage(
            final String currentUser,
            final String assumedRoles,
            final UUID packageUuid,
            final TestPackageUpdateResource body) {

        context.define(currentUser, assumedRoles);

        final var current = testPackageRepository.findByUuid(packageUuid);
        OptionalFromJson.of(body.getDescription()).ifPresent(current::setDescription);
        final var saved = testPackageRepository.save(current);
        final var mapped = map(saved, TestPackageResource.class);
        return ResponseEntity.ok(mapped);
    }
}
