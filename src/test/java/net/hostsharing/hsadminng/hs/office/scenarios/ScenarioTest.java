package net.hostsharing.hsadminng.hs.office.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.hs.office.scenarios.TemplateResolver.Resolver;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.shaded.org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.office.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.office.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class ScenarioTest extends ContextBasedTest {

    final static String RUN_AS_USER = "superuser-alex@hostsharing.net"; // TODO.test: use global:AGENT when implemented

    record Alias<T extends UseCase<T>>(Class<T> useCase, UUID uuid) {

        @Override
        public String toString() {
            return ObjectUtils.toString(uuid);
        }

    }
    private final static Map<String, Alias<?>> aliases = new HashMap<>();

    private final static Map<String, Object> properties = new HashMap<>();
    public final TestReport testReport = new TestReport(aliases);

    @LocalServerPort
    Integer port;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @SneakyThrows
    @BeforeEach
    void init(final TestInfo testInfo) {
        createHostsharingPerson();
        try {
            testInfo.getTestMethod().ifPresent(this::callRequiredProducers);
            testReport.createTestLogMarkdownFile(testInfo);
        } catch (Exception exc) {
            throw exc;
        }
    }

    @AfterEach
    void cleanup() { // final TestInfo testInfo
        properties.clear();
        testReport.close();
    }

    private void createHostsharingPerson() {
        jpaAttempt.transacted(() ->
                {
                    context.define("superuser-alex@hostsharing.net");
                    aliases.put(
                            "Person: Hostsharing eG",
                            new Alias<>(
                                    null,
                                    personRepo.findPersonByOptionalNameLike("Hostsharing eG")
                                            .stream()
                                            .map(HsOfficePersonEntity::getUuid)
                                            .reduce(Reducer::toSingleElement).orElseThrow())
                    );
                }
        );
    }

    @SneakyThrows
    private void callRequiredProducers(final Method currentTestMethod) {
        final var testMethodRequired = Optional.of(currentTestMethod)
                .map(m -> m.getAnnotation(Requires.class))
                .map(Requires::value)
                .orElse(null);
        if (testMethodRequired != null) {
            for (Method potentialProducerMethod : getClass().getDeclaredMethods()) {
                final var producesAnnot = potentialProducerMethod.getAnnotation(Produces.class);
                if (producesAnnot != null) {
                    final var testMethodProduces = allOf(
                            producesAnnot.value(),
                            producesAnnot.explicitly(),
                            producesAnnot.implicitly());
                    //  @formatter:off
                    if ( // that method can produce something required
                         testMethodProduces.contains(testMethodRequired) &&

                         // and it does not produce anything we already have (would cause errors)
                         SetUtils.intersection(testMethodProduces, knowVariables().keySet()).isEmpty()
                    ) {
                        assertThat(producesAnnot.permanent()).as("cannot depend on non-permanent producer: " + potentialProducerMethod);

                        // then we recursively produce the pre-requisites of the producer method
                        callRequiredProducers(potentialProducerMethod);

                        // and finally we call the producer method
                        potentialProducerMethod.invoke(this);
                    }
                    // @formatter:on
                }
            }
        }
    }

    private Set<String> allOf(final String value, final String explicitly, final String[] implicitly) {
        final var all = new HashSet<String>();
        if (!value.isEmpty()) {
            all.add(value);
        }
        if (!explicitly.isEmpty()) {
            all.add(explicitly);
        }
        all.addAll(asList(implicitly));
        return all;
    }

    static boolean containsAlias(final String alias) {
        return aliases.containsKey(alias);
    }

    static UUID uuid(final String nameWithPlaceholders) {
        final var resolvedName = resolve(nameWithPlaceholders, DROP_COMMENTS);
        final UUID alias = ofNullable(knowVariables().get(resolvedName)).filter(v -> v instanceof UUID).map(UUID.class::cast).orElse(null);
        assertThat(alias).as("alias '" + resolvedName + "' not found in aliases nor in properties [" +
                knowVariables().keySet().stream().map(v -> "'" + v + "'").collect(Collectors.joining(", ")) + "]"
        ).isNotNull();
        return alias;
    }

    static void putAlias(final String name, final Alias<?> value) {
        aliases.put(name, value);
    }

    static void putProperty(final String name, final Object value) {
        properties.put(name, (value instanceof String string) ? resolveTyped(string) : value);
    }

    static void removeProperty(final String propName) {
        properties.remove(propName);
    }

    static Map<String, Object> knowVariables() {
        final var map = new LinkedHashMap<String, Object>();
        ScenarioTest.aliases.forEach((key, value) -> map.put(key, value.uuid()));
        map.putAll(ScenarioTest.properties);
        return map;
    }

    public static String resolve(final String text, final Resolver resolver) {
        final var resolved = new TemplateResolver(text, ScenarioTest.knowVariables()).resolve(resolver);
        return resolved;
    }

    public static Object resolveTyped(final String resolvableText) {
        final var resolved = resolve(resolvableText, DROP_COMMENTS);
        try {
            return UUID.fromString(resolved);
        } catch (final IllegalArgumentException e) {
            // ignore and just use the String value
        }
        return resolved;
    }

    public static <T> T resolveTyped(final String resolvableText, final Class<T> valueType) {
        final var resolvedValue = resolve(resolvableText, DROP_COMMENTS);
        if (valueType == BigDecimal.class) {
            //noinspection unchecked
            return (T) new BigDecimal(resolvedValue);
        }
        //noinspection unchecked
        return (T) resolvedValue;
    }

}
