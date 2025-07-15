package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public abstract class BaseCredentialsUseCase<T extends UseCase<?>> extends UseCase<T> {

    public BaseCredentialsUseCase(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @SneakyThrows
    protected ContextResource[] fetchContextResourcesByDescriptorPairs(final String descriptPairsVarName) {
        final var requestedContexts = ScenarioTest.getTypedVariable("contexts", Pair[].class);
        final var existingContextsJson = withTitle("Fetch Available Account Contexts", () ->
                httpGet("/api/hs/accounts/contexts").expecting(OK).expecting(JSON)
        ).getResponse().body();
        final var existingContexts = objectMapper.readValue(existingContextsJson, ContextResource[].class);
        return Arrays.stream(requestedContexts)
                .map(pair -> Arrays.stream(existingContexts)
                        .filter(context -> context.getType().equals(pair.getLeft())
                                && context.getQualifier().equals(pair.getRight()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No matching context found for type=" + pair.getLeft()
                                        + " and qualifier=" + pair.getRight()))
                )
                .toArray(ContextResource[]::new);
    }
}
