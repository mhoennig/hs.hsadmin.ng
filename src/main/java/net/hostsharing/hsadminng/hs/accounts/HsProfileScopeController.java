package net.hostsharing.hsadminng.hs.accounts;

import java.util.List;

import io.micrometer.core.annotation.Timed;
import lombok.val;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.accounts.generated.api.v1.api.ScopesApi;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ScopeResource;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@NoSecurityRequirement
public class HsProfileScopeController implements ScopesApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsProfileScopeRbacRepository scopeRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.accounts.scopes.getListOfScopes")
    public ResponseEntity<List<ScopeResource>> getListOfScopes(final String assumedRoles) {
        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            context.assumeRoles(assumedRoles);
        }
        val isGlobalAdmin = context.isGlobalAdmin();
        final var scopes = scopeRepo.findAll().stream().filter(
                scope -> scope.isPublicAccess() || isGlobalAdmin
        ).toList();
        final var result = mapper.mapList(scopes, ScopeResource.class);
        return ResponseEntity.ok(result);
    }
}
