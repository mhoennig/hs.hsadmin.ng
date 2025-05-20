package net.hostsharing.hsadminng.credentials;

import java.util.List;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.credentials.generated.api.v1.api.LoginContextsApi;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginContextResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@NoSecurityRequirement
public class HsCredentialsContextsController implements LoginContextsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsCredentialsContextRbacRepository contextRepo;

    @Override
    @Timed("app.credentials.contexts.getListOfLoginContexts")
    public ResponseEntity<List<LoginContextResource>> getListOfLoginContexts(final String assumedRoles) {
        context.assumeRoles(assumedRoles);

        final var loginContexts = contextRepo.findAll();
        final var result = mapper.mapList(loginContexts, LoginContextResource.class);
        return ResponseEntity.ok(result);
    }
}
