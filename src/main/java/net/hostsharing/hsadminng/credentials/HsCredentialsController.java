package net.hostsharing.hsadminng.credentials;

import java.util.List;
import java.util.UUID;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.credentials.generated.api.v1.api.LoginCredentialsApi;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginCredentialsInsertResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginCredentialsPatchResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginCredentialsResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsCredentialsController implements LoginCredentialsApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManagerWrapper em;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficePersonRbacRepository personRepo;

    @Autowired
    private HsCredentialsRepository loginCredentialsRepo;

    @Override
    @Timed("app.credentials.credentials.getSingleLoginCredentialsByUuid")
    public ResponseEntity<LoginCredentialsResource> getSingleLoginCredentialsByUuid(
            final String assumedRoles,
            final UUID loginCredentialsUuid) {
        context.assumeRoles(assumedRoles);

        final var credentials = loginCredentialsRepo.findByUuid(loginCredentialsUuid);
        final var result = mapper.map(credentials, LoginCredentialsResource.class);
        return ResponseEntity.ok(result);
    }

    @Override
    @Timed("app.credentials.credentials.getListOfLoginCredentialsByPersonUuid")
    public ResponseEntity<List<LoginCredentialsResource>> getListOfLoginCredentialsByPersonUuid(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        final var person = personRepo.findByUuid(personUuid).orElseThrow(); // FIXME: use proper exception
        final var credentials = loginCredentialsRepo.findByPerson(person);
        final var result = mapper.mapList(credentials, LoginCredentialsResource.class);
        return ResponseEntity.ok(result);
    }

    @Override
    @Timed("app.credentials.credentials.postNewLoginCredentials")
    public ResponseEntity<LoginCredentialsResource> postNewLoginCredentials(
            final String assumedRoles,
            final LoginCredentialsInsertResource body
    ) {
        context.assumeRoles(assumedRoles);

        final var newLoginCredentialsEntity = mapper.map(body, HsCredentialsEntity.class);
        final var savedLoginCredentialsEntity = loginCredentialsRepo.save(newLoginCredentialsEntity);
        final var newLoginCredentialsResource = mapper.map(savedLoginCredentialsEntity, LoginCredentialsResource.class);
        return ResponseEntity.ok(newLoginCredentialsResource);
    }

    @Override
    @Timed("app.credentials.credentials.deleteLoginCredentialsByUuid")
    public ResponseEntity<Void> deleteLoginCredentialsByUuid(final String assumedRoles, final UUID loginCredentialsUuid) {
        context.assumeRoles(assumedRoles);
        final var loginCredentialsEntity = em.getReference(HsCredentialsEntity.class, loginCredentialsUuid);
        em.remove(loginCredentialsEntity);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Timed("app.credentials.credentials.patchLoginCredentials")
    public ResponseEntity<LoginCredentialsResource> patchLoginCredentials(
            final String assumedRoles,
            final UUID loginCredentialsUuid,
            final LoginCredentialsPatchResource body
    ) {
        context.assumeRoles(assumedRoles);

        final var current = loginCredentialsRepo.findByUuid(loginCredentialsUuid).orElseThrow();

        new HsCredentialsEntityPatcher(em, current).apply(body);

        final var saved = loginCredentialsRepo.save(current);
        final var mapped = mapper.map(saved, LoginCredentialsResource.class);
        return ResponseEntity.ok(mapped);
    }
}
