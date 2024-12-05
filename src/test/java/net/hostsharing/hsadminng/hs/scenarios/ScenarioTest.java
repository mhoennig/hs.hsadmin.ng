package net.hostsharing.hsadminng.hs.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacRepository;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.scenarios.Produces.Aggregator.producedAliases;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class ScenarioTest extends ContextBasedTest {

    final static String RUN_AS_USER = "superuser-alex@hostsharing.net"; // TODO.test: use global:AGENT when implemented

    private final Stack<String> currentTestMethodProduces = new Stack<>();

    protected ScenarioTest scenarioTest = this;

    Optional<String> takeProducedAlias() {
        if (currentTestMethodProduces.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(currentTestMethodProduces.pop());
    }

    record Alias<T extends UseCase<T>>(Class<T> useCase, UUID uuid) {

        @Override
        public String toString() {
            return Objects.toString(uuid);
        }

    }

    private final static Map<String, Alias<?>> aliases = new HashMap<>();

    private final static Map<String, Object> properties = new HashMap<>();
    public final TestReport testReport = new TestReport(aliases);

    @LocalServerPort
    Integer port;

    @Autowired
    HsOfficePersonRbacRepository personRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @SneakyThrows
    @BeforeEach
    void beforeScenario(final TestInfo testInfo) {
        createHostsharingPerson();
        try {
            testInfo.getTestMethod().ifPresent(currentTestMethod -> {
                callRequiredProducers(currentTestMethod);
                keepProducesAlias(currentTestMethod);
            });
            testReport.createTestLogMarkdownFile(testInfo);
        } catch (Exception exc) {
            throw exc;
        }
    }

    @AfterEach
    void afterScenario(final TestInfo testInfo) { // final TestInfo testInfo
        testInfo.getTestMethod() .ifPresent(currentTestMethod -> {
            // FIXME: extract to method
            final var producesAnnot = currentTestMethod.getAnnotation(Produces.class);
            if (producesAnnot != null && producesAnnot.permanent()) {
                final var testMethodProduces = producedAliases(producesAnnot);
                testMethodProduces.forEach(declaredAlias ->
                        assertThat(knowVariables().containsKey(declaredAlias))
                                .as("@Producer method " + currentTestMethod.getName() +
                                        " did declare but not produce \"" + declaredAlias + "\"")
                                .isTrue() );
            }
        });

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
                                            .map(HsOfficePersonRbacEntity::getUuid)
                                            .reduce(Reducer::toSingleElement).orElseThrow())
                    );
                }
        );
    }

    @SneakyThrows
    private void callRequiredProducers(final Method currentTestMethod) {
        final var testMethodRequires = Optional.of(currentTestMethod)
                .map(m -> m.getAnnotation(Requires.class))
                .map(Requires::value)
                .orElse(null);
        if (testMethodRequires != null) {
            for (Method potentialProducerMethod : getPotentialProducerMethods()) {
                final var producesAnnot = potentialProducerMethod.getAnnotation(Produces.class);
                final var testMethodProduces = producedAliases(producesAnnot);
                //  @formatter:off
                if ( // that method can produce something required
                     testMethodProduces.contains(testMethodRequires) &&

                     // and it does not produce anything we already have (would cause errors)
                     SetUtils.intersection(testMethodProduces, knowVariables().keySet()).isEmpty()
                ) {
                    assertThat(producesAnnot.permanent()).as("cannot depend on non-permanent producer: " + potentialProducerMethod);

                    // then we recursively produce the pre-requisites of the producer method
                    callRequiredProducers(potentialProducerMethod);
                    keepProducesAlias(currentTestMethod);

                    // and finally we call the producer method
                    invokeProducerMethod(this, potentialProducerMethod);
                }
                // @formatter:on
            }

            assertThat(knowVariables().containsKey(testMethodRequires))
                    .as("no @Producer for @Required(\"" + testMethodRequires + "\") found")
                    .isTrue();
        }
    }

    private void keepProducesAlias(final Method currentTestMethod) {
        final var producesAnnot = currentTestMethod.getAnnotation(Produces.class);
        if (producesAnnot != null) {
            final var producesAlias = isNotBlank(producesAnnot.value()) ? producesAnnot.value() : producesAnnot.explicitly();
            assertThat(producesAlias)
                    .as(currentTestMethod.getName() + " must define either value or explicit for @Produces")
                    .isNotNull();
            this.currentTestMethodProduces.push(producesAlias);
        }
    }

    private Method @NotNull [] getPotentialProducerMethods() {
        final var methodsDeclaredInOuterTestClass = stream(getClass().getDeclaredMethods())
                .filter(m -> m.getAnnotation(Produces.class) != null)
                .toArray(Method[]::new);
        final var methodsDeclaredInInnerTestClasses = stream(getClass().getDeclaredClasses())
                .map(Class::getDeclaredMethods).flatMap(Stream::of)
                .filter(m -> m.getAnnotation(Produces.class) != null)
                .toArray(Method[]::new);
        return ArrayUtils.addAll(methodsDeclaredInOuterTestClass, methodsDeclaredInInnerTestClasses);
    }

    @SneakyThrows
    private void invokeProducerMethod(final ScenarioTest scenarioTest, final Method producerMethod) {
        producerMethod.setAccessible(true);
        if (producerMethod.getDeclaringClass() == scenarioTest.getClass()) {
            producerMethod.invoke(this);
        } else {
            final var innerClassConstructor = producerMethod.getDeclaringClass()
                    .getDeclaredConstructor(scenarioTest.getClass());
            innerClassConstructor.setAccessible(true);
            final var inner = innerClassConstructor.newInstance(this);
            producerMethod.invoke(inner);
        }
    }

    static boolean containsAlias(final String alias) {
        return aliases.containsKey(alias);
    }

    static UUID uuid(final String nameWithPlaceholders) {
        final var resolvedName = resolve(nameWithPlaceholders, DROP_COMMENTS);
        final UUID alias = ofNullable(knowVariables().get(resolvedName)).filter(v -> v instanceof UUID)
                .map(UUID.class::cast)
                .orElse(null);
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
