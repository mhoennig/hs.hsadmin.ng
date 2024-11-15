package net.hostsharing.hsadminng.hs.office.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.system.SystemProcess;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReport {

    public static final File BUILD_DOC_SCENARIOS = new File("build/doc/scenarios");
    private final static File markdownLogFile = new File(BUILD_DOC_SCENARIOS, ".last-debug-log.md");
    public static final SimpleDateFormat MM_DD_YYYY_HH_MM_SS = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");

    private final Map<String, ?> aliases;
    private final PrintWriter markdownLog; // records everything for debugging purposes
    private File markdownReportFile;
    private PrintWriter markdownReport; // records only the use-case under test, without its pre-requisites
    private int silent; // do not print anything to test-report if >0

    static {
        assertThat(BUILD_DOC_SCENARIOS.isDirectory() || BUILD_DOC_SCENARIOS.mkdirs())
                .as("mkdir " + BUILD_DOC_SCENARIOS).isTrue();
    }

    @SneakyThrows
    public TestReport(final Map<String, ?> aliases) {
        this.aliases = aliases;
        this.markdownLog = new PrintWriter(new FileWriter(markdownLogFile));
    }

    public void createTestLogMarkdownFile(final TestInfo testInfo) throws IOException {
        final var testMethodName = testInfo.getTestMethod().map(Method::getName).orElseThrow();
        final var testMethodOrder = testInfo.getTestMethod().map(m -> m.getAnnotation(Order.class).value()).orElseThrow();
        markdownReportFile = new File(BUILD_DOC_SCENARIOS, testMethodOrder + "-" + testMethodName + ".md");
        markdownReport = new PrintWriter(new FileWriter(markdownReportFile));
        print("## Scenario #" + determineScenarioTitle(testInfo));
    }

    @SneakyThrows
    public void print(final String output) {

        final var outputWithCommentsForUuids = appendUUIDKey(output);

        // for tests executed due to @Requires/@Produces there is no markdownFile yet
        if (markdownReport != null && silent == 0) {
            markdownReport.print(outputWithCommentsForUuids);
        }

        // but the debugLog should contain all output, even if silent
        markdownLog.print(outputWithCommentsForUuids);
    }

    public void printLine(final String output) {
        print(output + "\n");
    }

    public  void printPara(final String output) {
        printLine("\n" +output + "\n");
    }

    void silent(final Runnable code) {
        silent++;
        code.run();
        silent--;
    }

    public void close() {
        if (markdownReport != null) {
            printPara("---");
            printPara("generated on " + MM_DD_YYYY_HH_MM_SS.format(new Date()) + " for branch " + currentGitBranch());
            markdownReport.close();
            System.out.println("SCENARIO REPORT: " + asClickableLink(markdownReportFile));
        }
        markdownLog.close();
        System.out.println("DEBUG LOG: " + asClickableLink(markdownLogFile));
    }

    private static @NotNull String determineScenarioTitle(final TestInfo testInfo) {
        final var convertedTestMethodName =
                testInfo.getTestMethod().map(TestReport::orderNumber).orElseThrow() + ": " +
                testInfo.getTestMethod().map(Method::getName).map(t -> t.replaceAll("([a-z])([A-Z]+)", "$1 $2")).orElseThrow();
        return convertedTestMethodName.replaceAll(": should ", ": ");
    }

    private String asClickableLink(final File file) {
        return file.toURI().toString().replace("file:/", "file:///");
    }

    private static Object orderNumber(final Method method) {
        return method.getAnnotation(Order.class).value();
    }

    private String appendUUIDKey(String multilineText) {
        final var lines = multilineText.split("\\r?\\n");
        final var result = new StringBuilder();

        for (String line : lines) {
            for (Map.Entry<String, ?> entry : aliases.entrySet()) {
                final var uuidString = entry.getValue().toString();
                if (line.contains(uuidString)) {
                    line = line + " // " + entry.getKey();
                    break;  // only add comment for one UUID per row (in our case, there is only one per row)
                }
            }
            result.append(line).append("\n");
        }
        return result.toString();
    }

    @SneakyThrows
    private String currentGitBranch() {
        try {
            final var gitRevParse = new SystemProcess("git", "rev-parse", "--abbrev-ref", "HEAD");
            gitRevParse.execute();
            return gitRevParse.getStdOut().split("\\R", 2)[0];
        } catch (final IOException exc) {
            // TODO.test: the git call does not work in Jenkins, we have to find out why
            System.err.println(exc);
            return "unknown";
        }
    }
}
