package net.hostsharing.hsadminng.hs.office.scenarios;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase.HttpResponse;

import java.util.function.Consumer;

public class PathAssertion {

    private final String path;

    public PathAssertion(final String path) {
        this.path = path;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Consumer<UseCase.HttpResponse> contains(final String resolvableValue) {
        return response -> response.path(path).contains(ScenarioTest.resolve(resolvableValue));
    }

    public Consumer<HttpResponse> doesNotExist() {
        return response -> response.path(path).isNull(); // here, null Optional means key not found in JSON
    }
}
