package net.hostsharing.hsadminng.credentials;

import java.util.List;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.credentials.generated.api.v1.api.ContextsApi;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@NoSecurityRequirement
public class HsCredentialsContextsController implements ContextsApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsCredentialsContextRbacRepository contextRepo;

    @Override
    @Timed("app.credentials.contexts.getListOfLoginContexts")
    public ResponseEntity<List<ContextResource>> getListOfContexts(final String assumedRoles) {
        context.assumeRoles(assumedRoles);

        final var loginContexts = contextRepo.findAll();
        final var result = mapper.mapList(loginContexts, ContextResource.class);
        return ResponseEntity.ok(result);
    }
}
