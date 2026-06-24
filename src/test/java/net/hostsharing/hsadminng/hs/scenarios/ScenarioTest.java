package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.test.IgnoreOnFailureExtension;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.hs.scenarios.Produces.Aggregator.producedAliases;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("scenarioTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class,
                    RbacGrantsDiagramService.class },
        properties = {
                "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:17.7-trixie:///scenariosTC}",
                "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:ADMIN}",
                "spring.datasource.password=${HSADMINNG_POSTGRES_ADMIN_PASSWORD:password}",
                "hsadminng.superuser=${HSADMINNG_SUPERUSER:superuser-alex@hostsharing.net}"
        }
)
@ActiveProfiles({ "fake-jwt" })
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@ExtendWith(IgnoreOnFailureExtension.class)
public abstract class ScenarioTest extends ContextBasedTest {

    private final Stack<String> currentTestMethodProduces = new Stack<>();

    protected ScenarioTest scenarioTest = this;

    Optional<String> takeProducedAlias() {
        if (currentTestMethodProduces.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(currentTestMethodProduces.pop());
    }

    private final static Map<String, UUID> aliases = new HashMap<>();

    private final static Map<String, Object> properties = new HashMap<>();
    public final TestReport testReport = new TestReport(aliases);

    @LocalServerPort
    Integer port;

    @Autowired
    protected JpaAttempt jpaAttempt;

    @BeforeEach
    @SneakyThrows
    protected void beforeScenario(final TestInfo testInfo) {
        try {
            val currentTestMethod = testInfo.getTestMethod().orElseThrow();
            callRequiredProducers(currentTestMethod);
            keepProducesAlias(currentTestMethod);
            testReport.createTestLogMarkdownFile(testInfo);
            prepareRequiredProducersForReport(currentTestMethod);
        } catch (final Exception exc) {
            throw exc;
        }
    }

    @AfterEach
    void afterScenario(final TestInfo testInfo) {
        verifyProduceDeclaration(testInfo);

        properties.clear();
        testReport.close();
    }

    @SneakyThrows
    public static String bearerTemplate(final String subjectTemplate) {
        val subject = resolve(subjectTemplate, DROP_COMMENTS);
        return bearer(subject);
    }

    @SneakyThrows
    private void callRequiredProducers(final Method currentTestMethod) {
        final var testMethodRequires = Stream.of(currentTestMethod)
                .map(m -> m.getAnnotation(Requires.class))
                .filter(Objects::nonNull)
                .flatMap(annotation -> Stream.of(annotation.value()))
                .collect(Collectors.toSet());
        if (!testMethodRequires.isEmpty()) {
            for (Method potentialProducerMethod : getPotentialProducerMethods()) {
                final var producesAnnot = potentialProducerMethod.getAnnotation(Produces.class);
                final var testMethodProduces = producedAliases(producesAnnot);
                if ( thatMethodProducesSomethingRequired(testMethodProduces, testMethodRequires) &&
                     thatMethodDoesNotProduceAnythingWeAlreadyHave(testMethodProduces)
                ) {
                    assertThat(producesAnnot.permanent()).as("cannot depend on non-permanent producer: " + potentialProducerMethod);

                    // then we recursively produce the pre-requisites of the producer method
                    callRequiredProducers(potentialProducerMethod);
                    keepProducesAlias(currentTestMethod);

                    // and finally we call the producer method
                    invokeProducerMethod(this, potentialProducerMethod);
                }
            }

            assertThat(haveIntersection(knowVariables().keySet(), testMethodRequires))
                    .as("no @Producer for @Required(\"" + testMethodRequires + "\") found")
                    .isTrue();
        }
    }

    private static boolean haveIntersection(final Set<String> set1, final Set<String> set2) {
        return !SetUtils.intersection(set1, set2).isEmpty();
    }

    private static boolean areDisjunct(final Set<String> set1, final Set<String> set2) {
        return !haveIntersection(set1, set2);
    }

    private static boolean thatMethodProducesSomethingRequired(final Set<String> testMethodProduces, final Set<String> testMethodRequires) {
        return haveIntersection(testMethodProduces, testMethodRequires);
    }

    private static boolean thatMethodDoesNotProduceAnythingWeAlreadyHave(final Set<String> testMethodProduces) {
        return areDisjunct(testMethodProduces, knowVariables().keySet());
    }

    private void prepareRequiredProducersForReport(final Method currentTestMethod) {
        val requiresAnnot = currentTestMethod.getAnnotation(Requires.class);
        if (requiresAnnot == null) {
            return;
        }

        testReport.setRequiredProducerLinks(
                stream(requiresAnnot.value())
                        .map(requiredAlias -> "- `" + requiredAlias + "` " + producerLinkFor(requiredAlias))
                        .toList());
    }

    private String producerLinkFor(final String requiredAlias) {
        val producerMethod = stream(getPotentialProducerMethods())
                .filter(method -> producedAliases(method.getAnnotation(Produces.class)).contains(requiredAlias))
                .findFirst();
        return producerMethod
                .map(method -> "from [" + TestReport.reportTitle(method) + "](<" + TestReport.reportFileName(method) + ">)")
                .orElse("(producer report not found)");
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

    private static void verifyProduceDeclaration(final TestInfo testInfo) {
        testInfo.getTestMethod().ifPresent(currentTestMethod -> {
            final var producesAnnot = currentTestMethod.getAnnotation(Produces.class);
            if (producesAnnot != null && producesAnnot.permanent()) {
                final var testMethodProduces = producedAliases(producesAnnot);
                testMethodProduces.forEach(declaredAlias ->
                        assertThat(knowVariables().containsKey(declaredAlias))
                                .as("@Producer method " + currentTestMethod.getName() +
                                        " did declare but not produce \"" + declaredAlias + "\"")
                                .isTrue());
            }
        });
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

    protected static void putAlias(final String name, final UUID value) {
        aliases.put(name, value);
    }

    protected static void putProperty(final String name, final Object value) {
        properties.put(name, (value instanceof String string) ? resolveTyped(string) : value);
    }

    protected static void removeProperty(final String propName) {
        properties.remove(propName);
    }

    public static Map<String, Object> knowVariables() {
        final var map = new LinkedHashMap<String, Object>();
        map.putAll(ScenarioTest.aliases);
        map.putAll(ScenarioTest.properties);
        return map;
    }

    public static String resolve(final String text, final Resolver resolver) {
        final var resolved = new TemplateResolver(text, ScenarioTest.knowVariables()).resolve(resolver);
        return resolved;
    }

    @SneakyThrows
    public static List<Map<String, Object>> resolveJsonArray(final String text) {
        return new ObjectMapper().readValue(
                resolve(text, DROP_COMMENTS),
                new TypeReference<>() {}
        );
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
        if (valueType == Integer.class) {
            //noinspection unchecked
            return (T) Integer.valueOf(resolvedValue);
        }
        //noinspection unchecked
        return (T) resolvedValue;
    }
}
