package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

/**
 * What tools/generate-verified-by needs to fill a PR document, written by the scenario test run itself,
 * so that the report title comes from TestReport instead of being rebuilt in a second tool.
 */
public final class ScenarioIndex {

    public static final File FILE = new File(TestReport.SCENARIO_REPORTS_MARKDOWN, "index.json");

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Entry> entriesByReport = new TreeMap<>();
    private static boolean flushRegistered;

    private ScenarioIndex() {
    }

    public record Entry(String[] scenarios, String testClass, String testMethod, String useCase) {
    }

    static synchronized void record(final Method testMethod) {
        val scenarios = testMethod.getAnnotation(Scenario.class);
        entriesByReport.put(
                TestReport.reportTitle(testMethod),
                new Entry(
                        scenarios == null ? new String[0] : scenarios.value(),
                        testMethod.getDeclaringClass().getName(),
                        testMethod.getName(),
                        null));
        registerFlush();
    }

    /** The use-case is only known once it runs, thus it amends the entry of the current report. */
    static synchronized void recordUseCase(final String reportTitle, final Class<?> useCase) {
        val entry = entriesByReport.get(reportTitle);
        if (entry != null && entry.useCase() == null) {
            entriesByReport.put(
                    reportTitle,
                    new Entry(entry.scenarios(), entry.testClass(), entry.testMethod(), useCase.getName()));
        }
    }

    private static void registerFlush() {
        if (!flushRegistered) {
            flushRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(ScenarioIndex::flush));
        }
    }

    @SneakyThrows
    static synchronized void flush() {
        JSON.writerWithDefaultPrettyPrinter().writeValue(FILE, entriesByReport);
    }
}
