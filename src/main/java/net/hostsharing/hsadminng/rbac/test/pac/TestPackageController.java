package net.hostsharing.hsadminng.rbac.test.pac;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.test.generated.api.v1.api.TestPackagesApi;
import net.hostsharing.hsadminng.test.generated.api.v1.model.TestPackageResource;
import net.hostsharing.hsadminng.test.generated.api.v1.model.TestPackageUpdateResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "casTicket")
public class TestPackageController implements TestPackagesApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private TestPackageRepository testPackageRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<TestPackageResource>> listPackages(
            String currentSubject,
            String assumedRoles,
            String name
    ) {
        context.define(currentSubject, assumedRoles);

        final var result = testPackageRepository.findAllByOptionalNameLike(name);
        return ResponseEntity.ok(mapper.mapList(result, TestPackageResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<TestPackageResource> updatePackage(
            final String currentSubject,
            final String assumedRoles,
            final UUID packageUuid,
            final TestPackageUpdateResource body) {

        context.define(currentSubject, assumedRoles);

        final var current = testPackageRepository.findByUuid(packageUuid);
        OptionalFromJson.of(body.getDescription()).ifPresent(current::setDescription);
        final var saved = testPackageRepository.save(current);
        final var mapped = mapper.map(saved, TestPackageResource.class);
        return ResponseEntity.ok(mapped);
    }
}
