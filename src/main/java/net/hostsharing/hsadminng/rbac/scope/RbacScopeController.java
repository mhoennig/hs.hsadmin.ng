package net.hostsharing.hsadminng.rbac.scope;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.config.ApiKeyScope;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacScopesApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.ApiKeyScopeInfoResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.ApiKeyScopeResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class RbacScopeController implements RbacScopesApi {

    @Override
    @Timed("app.rbac.scopes.api.getListOfApiKeyScopes")
    public ResponseEntity<List<ApiKeyScopeInfoResource>> getListOfApiKeyScopes() {
        // static metadata from the ApiKeyScope enum, no RBAC context needed
        return ResponseEntity.ok(
                Arrays.stream(ApiKeyScope.values())
                        .map(RbacScopeController::toResource)
                        .toList());
    }

    private static ApiKeyScopeInfoResource toResource(final ApiKeyScope scope) {
        final var resource = new ApiKeyScopeInfoResource();
        resource.setScope(ApiKeyScopeResource.fromValue(scope.wireName()));
        resource.setAllows(scope.allowedEndpoints());
        return resource;
    }
}
