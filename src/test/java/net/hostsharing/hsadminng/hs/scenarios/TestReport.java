package net.hostsharing.hsadminng.hs.scenarios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;

public class TestReport {

    public static final File BUILD_DOC_SCENARIOS = new File("build/doc/scenarios");
    public static final SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private static final File markdownLogFile = new File(BUILD_DOC_SCENARIOS, ".last-debug-log.md");
    private static final ObjectMapper objectMapper = JsonObjectMapperConfiguration.build();
    private static final Set<String> LOWERCASE_TITLE_WORDS = Set.of(
            "a", "an", "the",
            "and", "or", "for",
            "to", "of", "in", "on", "with", "by");

    private final Map<String, UUID> aliases;
    private final PrintWriter markdownLog; // records everything for debugging purposes
    private File markdownReportFile;
    private PrintWriter markdownReport; // records only the use-case under test, without its pre-requisites
    private int silent; // do not print anything to test-report if >0
    private List<String> requiredProducerLinks = List.of();
    private boolean previousMarkdownLineWasBlank = true;
    private boolean withinCodeBlock = false;

    static {
        assertThat(BUILD_DOC_SCENARIOS.isDirectory() || BUILD_DOC_SCENARIOS.mkdirs())
                .as("mkdir " + BUILD_DOC_SCENARIOS).isTrue();
    }

    @SneakyThrows
    public TestReport(final Map<String, UUID> aliases) {
        this.aliases = aliases;
        this.markdownLog = new PrintWriter(new FileWriter(markdownLogFile));
    }

    public void createTestLogMarkdownFile(final TestInfo testInfo) throws IOException {
        markdownReportFile = new File(BUILD_DOC_SCENARIOS, reportFileName(testInfo.getTestMethod().orElseThrow()));
        markdownReport = new PrintWriter(new FileWriter(markdownReportFile));
        previousMarkdownLineWasBlank = true;
        withinCodeBlock = false;
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

    void setRequiredProducerLinks(final List<String> requiredProducerLinks) {
        this.requiredProducerLinks = requiredProducerLinks;
    }

    void printRequiredProducerLinks() {
        if (requiredProducerLinks.isEmpty()) {
            return;
        }

        printPara("#### Required");
        requiredProducerLinks.forEach(this::printLine);
    }

    @SneakyThrows
    public void printJson(final String json) {
        printLine(prettyJson(json));
    }

    void silent(final Runnable code) {
        silent++;
        code.run();
        silent--;
    }

    public void close() {
        if (markdownReport != null) {
            printPara("---");
            printPara("generated on " + YYYY_MM_DD_HH_MM_SS.format(new Date()) + " for branch " + currentGitBranch());
            markdownReport.close();
            System.out.println("SCENARIO REPORT: " + asClickableLink(markdownReportFile));
        }
        requiredProducerLinks = List.of();
        markdownLog.close();
        System.out.println("DEBUG LOG: " + asClickableLink(markdownLogFile));
    }

    private static @NotNull String determineScenarioTitle(final TestInfo testInfo) {
        return testInfo.getTestMethod().map(TestReport::reportTitle).orElseThrow();
    }

    static String reportTitle(final Method method) {
        return orderNumber(method) + ": " + asScenarioTitle(method.getName());
    }

    static String reportFileName(final Method method) {
        return reportTitle(method) + ".md";
    }

    private static String prettyJson(final String json) throws JsonProcessingException {
        if (json == null) {
            return "";
        }
        final var jsonNode = objectMapper.readTree(json);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
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
            if (line.trim().startsWith("```")) {
                withinCodeBlock = !withinCodeBlock;
            }
            // a `#` within a code block is a shell comment, not a markdown heading which needs a blank line before it
            if (line.startsWith("#") && !previousMarkdownLineWasBlank && !withinCodeBlock) {
                result.append("\n");
            }
            for (Map.Entry<String, UUID> entry : aliases.entrySet()) {
                if ( entry.getValue() != null  && line.contains(entry.getValue().toString())) {
                    line = appendAliasComment(line, entry.getKey());
                    break;  // only add comment for one UUID per row (in our case, there is only one per row)
                }
            }
            result.append(line).append("\n");
            previousMarkdownLineWasBlank = line.isBlank();
        }
        return result.toString();
    }

    private String appendAliasComment(final String line, final String alias) {
        if (line.endsWith(" \\")) {
            return line.substring(0, line.length() - 2) + " // " + alias + " \\";
        }
        return line + " // " + alias;
    }

    private static String chopShouldPrefix(final String text) {
        return text.replaceAll("^should", "");
    }

    private static String asScenarioTitle(final String methodName) {
        final var withoutShouldPrefix = chopShouldPrefix(methodName);
        final var separatedWords = splitMixedCaseIntoSeparateWords(withoutShouldPrefix);
        return capitalizeFirstLetter(lowercaseTitleWords(separatedWords));
    }

    private static String splitMixedCaseIntoSeparateWords(final String text) {
        final var WORD_FINDER = Pattern.compile("(([A-Z]?[a-z]+)|([A-Z]))");
        final var matcher = WORD_FINDER.matcher(text);
        final var words = new ArrayList<String>();
        while (matcher.find()) {
            words.add(matcher.group(0));
        }
        return join(" ", words);
    }

    private static String capitalizeFirstLetter(final String words) {
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String lowercaseTitleWords(final String separatedWords) {
        final var words = separatedWords.split(" ");
        for (int i = 1; i < words.length - 1; ++i) {
            if (LOWERCASE_TITLE_WORDS.contains(words[i].toLowerCase(Locale.ROOT))) {
                words[i] = words[i].toLowerCase(Locale.ROOT);
            }
        }
        return join(" ", words);
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

    public boolean isSilent() {
        return silent > 0;
    }
}
