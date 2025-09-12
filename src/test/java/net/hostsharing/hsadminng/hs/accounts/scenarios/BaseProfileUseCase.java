package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ScopeResource;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public abstract class BaseProfileUseCase<T extends UseCase<?>> extends UseCase<T> {

    public BaseProfileUseCase(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @SneakyThrows
    protected ScopeResource[] fetchScopeResourcesByDescriptorPairs(final String descriptPairsVarName) {
        final var requestedScopes = ScenarioTest.getTypedVariable("scopes", Pair[].class);
        final var existingScopesJson = withTitle("Fetch Available Account Scopes", () ->
                httpGet("/api/hs/accounts/scopes").expecting(OK).expecting(JSON)
        ).getResponse().body();
        final var existingScopes = objectMapper.readValue(existingScopesJson, ScopeResource[].class);
        return Arrays.stream(requestedScopes)
                .map(pair -> Arrays.stream(existingScopes)
                        .filter(scope -> scope.getType().equals(pair.getLeft())
                                && scope.getQualifier().equals(pair.getRight()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No matching scope found for type=" + pair.getLeft()
                                        + " and qualifier=" + pair.getRight()))
                )
                .toArray(ScopeResource[]::new);
    }
}
