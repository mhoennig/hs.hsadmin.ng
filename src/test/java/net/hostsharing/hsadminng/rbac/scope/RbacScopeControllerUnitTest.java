package net.hostsharing.hsadminng.rbac.scope;

import lombok.val;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.ApiKeyScopeInfoResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RbacScopeControllerUnitTest {

    private final RbacScopeController controller = new RbacScopeController();

    @Test
    void returnsAllAvailableScopesWithTheirAllowedEndpoints() {
        // when
        val response = controller.getListOfApiKeyScopes();

        // then
        assertThat(response.getBody())
                .extracting(resource -> resource.getScope().getValue(), ApiKeyScopeInfoResource::getAllows)
                .containsExactly(
                        tuple("rbac.subjects:sync",
                                List.of(
                                        "GET /api/rbac/subjects",
                                        "GET /api/rbac/subjects/{uuid}",
                                        "PUT /api/rbac/subjects/{uuid}",
                                        "POST /api/rbac/subjects/sync-report"
                                )),
                        tuple("hs.office:billing-mda",
                                List.of(
                                        "GET /api/hs/office/debitors",
                                        "GET /api/hs/office/debitors/D-{debitorNumber}",
                                        "GET /api/hs/office/sepamandates"
                                )),
                        tuple("*:read",
                                List.of("GET /api/**")));
    }
}
