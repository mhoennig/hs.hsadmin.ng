package net.hostsharing.hsadminng.credentials;

import java.util.List;
import java.util.UUID;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.credentials.generated.api.v1.api.CredentialsApi;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsInsertResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "casTicket")
public class HsCredentialsController implements CredentialsApi {

    @Autowired
    private Context context;

    @Autowired
    private EntityManagerWrapper em;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsOfficePersonRbacRepository personRepo;

    @Autowired
    private HsCredentialsRepository credentialsRepo;

    @Override
    @Timed("app.credentials.credentials.getSingleCredentialsByUuid")
    public ResponseEntity<CredentialsResource> getSingleCredentialsByUuid(
            final String assumedRoles,
            final UUID credentialsUuid) {
        context.assumeRoles(assumedRoles);

        final var credentials = credentialsRepo.findByUuid(credentialsUuid);
        final var result = mapper.map(credentials, CredentialsResource.class);
        return ResponseEntity.ok(result);
    }

    @Override
    @Timed("app.credentials.credentials.getListOfCredentialsByPersonUuid")
    public ResponseEntity<List<CredentialsResource>> getListOfCredentialsByPersonUuid(
            final String assumedRoles,
            final UUID personUuid
    ) {
        context.assumeRoles(assumedRoles);

        final var person = personRepo.findByUuid(personUuid).orElseThrow(); // FIXME: use proper exception
        final var credentials = credentialsRepo.findByPerson(person);
        final var result = mapper.mapList(credentials, CredentialsResource.class);
        return ResponseEntity.ok(result);
    }

    @Override
    @Timed("app.credentials.credentials.postNewCredentials")
    public ResponseEntity<CredentialsResource> postNewCredentials(
            final String assumedRoles,
            final CredentialsInsertResource body
    ) {
        context.assumeRoles(assumedRoles);

        final var newCredentialsEntity = mapper.map(body, HsCredentialsEntity.class);
        final var savedCredentialsEntity = credentialsRepo.save(newCredentialsEntity);
        final var newCredentialsResource = mapper.map(savedCredentialsEntity, CredentialsResource.class);
        return ResponseEntity.ok(newCredentialsResource);
    }

    @Override
    @Timed("app.credentials.credentials.deleteCredentialsByUuid")
    public ResponseEntity<Void> deleteCredentialsByUuid(final String assumedRoles, final UUID credentialsUuid) {
        context.assumeRoles(assumedRoles);
        final var credentialsEntity = em.getReference(HsCredentialsEntity.class, credentialsUuid);
        em.remove(credentialsEntity);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Timed("app.credentials.credentials.patchCredentials")
    public ResponseEntity<CredentialsResource> patchCredentials(
            final String assumedRoles,
            final UUID credentialsUuid,
            final CredentialsPatchResource body
    ) {
        context.assumeRoles(assumedRoles);

        final var current = credentialsRepo.findByUuid(credentialsUuid).orElseThrow();

        new HsCredentialsEntityPatcher(em, current).apply(body);

        final var saved = credentialsRepo.save(current);
        final var mapped = mapper.map(saved, CredentialsResource.class);
        return ResponseEntity.ok(mapped);
    }
}
