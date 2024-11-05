package net.hostsharing.hsadminng.hs.office.scenarios;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReport {

    private final Map<String, ?> aliases;
    private final StringBuilder markdownLog = new StringBuilder(); // records everything for debugging purposes

    private PrintWriter markdownReport;
    private int silent; // do not print anything to test-report if >0

    public TestReport(final Map<String, ?> aliases) {
        this.aliases = aliases;
    }

    public void createTestLogMarkdownFile(final TestInfo testInfo) throws IOException {
        final var testMethodName = testInfo.getTestMethod().map(Method::getName).orElseThrow();
        final var testMethodOrder = testInfo.getTestMethod().map(m -> m.getAnnotation(Order.class).value()).orElseThrow();
        assertThat(new File("doc/scenarios/").isDirectory() || new File("doc/scenarios/").mkdirs()).as("mkdir doc/scenarios/").isTrue();
        markdownReport = new PrintWriter(new FileWriter("doc/scenarios/" + testMethodOrder + "-" + testMethodName + ".md"));
        print("## Scenario #" + testInfo.getTestMethod().map(TestReport::orderNumber).orElseThrow() + ": " +
                testMethodName.replaceAll("([a-z])([A-Z]+)", "$1 $2"));
    }

    @SneakyThrows
    public void print(final String output) {

        final var outputWithCommentsForUuids = appendUUIDKey(output);

        // for tests executed due to @Requires/@Produces there is no markdownFile yet
        if (markdownReport != null && silent == 0) {
            markdownReport.print(outputWithCommentsForUuids);
        }

        // but the debugLog should contain all output, even if silent
        markdownLog.append(outputWithCommentsForUuids);
    }

    public void printLine(final String output) {
        print(output + "\n");
    }

    public  void printPara(final String output) {
        printLine("\n" +output + "\n");
    }

    public void close() {
        if (markdownReport != null) {
            markdownReport.close();
        }
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

    void silent(final Runnable code) {
        silent++;
        code.run();
        silent--;
    }

}
