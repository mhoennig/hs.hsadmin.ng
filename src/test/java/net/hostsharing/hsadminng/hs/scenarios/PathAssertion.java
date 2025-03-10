package net.hostsharing.hsadminng.hs.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.UseCase.HttpResponse;

import java.util.function.Consumer;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PathAssertion {

    private final String path;

    public PathAssertion(final String path) {
        this.path = path;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Consumer<UseCase.HttpResponse> contains(final String resolvableValue) {
        return response -> {
            try {
                response.path(path).isEqualTo(ScenarioTest.resolve(resolvableValue, DROP_COMMENTS));
            } catch (final AssertionError e) {
                // without this, the error message is often lacking important context
                fail(e.getMessage() + " in `path(\"" + path +  "\").contains(\"" + resolvableValue + "\")`" );
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Consumer<UseCase.HttpResponse> lenientlyContainsJson(final String resolvableValue) {
        return response -> {
            try {
                lenientlyEquals(ScenarioTest.resolve(resolvableValue, DROP_COMMENTS)).matches(response.getFromBody(path)) ;
            } catch (final AssertionError e) {
                // without this, the error message is often lacking important context
                fail(e.getMessage() + " in `path(\"" + path +  "\").contains(\"" + resolvableValue + "\")`" );
            }
        };
    }

    public Consumer<HttpResponse> doesNotExist() {
        return response -> {
            try {
                response.path(path).isNull(); // here, null Optional means key not found in JSON
            } catch (final AssertionError e) {
                // without this, the error message is often lacking important context
                fail(e.getMessage() + " in `path(\"" + path +  "\").doesNotExist()`" );
            }
        };
    }
}
