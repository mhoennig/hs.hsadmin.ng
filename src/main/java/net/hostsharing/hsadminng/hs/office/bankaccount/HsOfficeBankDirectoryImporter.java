package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps the German banks of hs_office.bank in line with the bank-code file of the Bundesbank,
 * which must not be shipped with the source code for license reasons.
 */
@Slf4j
@Component
@EnableScheduling
// @ConditionalOnProperty would take an empty URL as given
@ConditionalOnExpression("!'${hsadminng.bank-directory.import.url:}'.isEmpty()")
public class HsOfficeBankDirectoryImporter {

    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    // imported versions stay, they tell which version is current and which need no further import
    private static final Duration RUN_RETENTION = Duration.ofDays(366);
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "Bankleitzahl", "Merkmal", "Bezeichnung", "BIC", "Änderungskennzeichen");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final TaskScheduler scheduler;
    private final ApplicationEventPublisher events;
    private final URI sourceUrl;
    private final Duration retryDelay;
    private final int maxAttempts;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(TIMEOUT)
            .build();

    public HsOfficeBankDirectoryImporter(
            final JdbcTemplate jdbc,
            final PlatformTransactionManager transactionManager,
            final TaskScheduler scheduler,
            final ApplicationEventPublisher events,
            @Value("${hsadminng.bank-directory.import.url}") final URI sourceUrl,
            @Value("${hsadminng.bank-directory.import.retry-delay}") final Duration retryDelay,
            @Value("${hsadminng.bank-directory.import.max-attempts}") final int maxAttempts) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.scheduler = scheduler;
        this.events = events;
        this.sourceUrl = sourceUrl;
        this.retryDelay = retryDelay;
        this.maxAttempts = maxAttempts;
    }

    public record Bank(String bankCode, String bic, String name) {
    }

    public record BankCodeFile(List<Bank> banks, Set<String> deletedBankCodes) {
    }

    /** Lets those who use bank accounts check them against the new version without this package knowing them. */
    public record Imported(URI sourceUrl) {
    }

    /** What a monitoring may learn about a failed attempt; the exception message stays in the log. */
    static class ImportFailure extends IOException {

        enum Kind { HTTP_ERROR, NO_RESPONSE, INVALID_FILE, INTERNAL }

        final Kind kind;
        final String request;
        final Integer httpStatus;
        final Integer line;

        ImportFailure(final Kind kind, final String request, final Integer httpStatus, final Integer line,
                final String message, final Throwable cause) {
            super(message, cause);
            this.kind = kind;
            this.request = request;
            this.httpStatus = httpStatus;
            this.line = line;
        }

        static ImportFailure invalidFile(final Integer line, final String message) {
            return new ImportFailure(Kind.INVALID_FILE, null, null, line, message, null);
        }

        static ImportFailure of(final Exception exc) {
            return exc instanceof ImportFailure failure
                    ? failure
                    : new ImportFailure(Kind.INTERNAL, null, null, null, exc.getMessage(), exc);
        }
    }

    @Scheduled(
            initialDelayString = "${hsadminng.bank-directory.import.initial-delay}",
            fixedDelayString = "${hsadminng.bank-directory.import.interval}")
    public void importIfNewVersion() {
        importIfNewVersion(1);
    }

    private void importIfNewVersion(final int attempt) {
        val startedAt = Instant.now();
        try {
            val currentUrl = currentVersionUrl();
            if (alreadyImported(currentUrl)) {
                log.info("bank directory {} is already imported", currentUrl);
                recordRun(startedAt, "UNCHANGED", currentUrl, null, null);
                return;
            }
            val file = parse(currentUrl, download(currentUrl));
            transaction.executeWithoutResult(status -> replaceBundesbankRows(currentUrl, file));
            log.info("bank directory {} imported with {} banks", currentUrl, file.banks().size());
            checkAgainst(currentUrl);
        } catch (final IOException | RuntimeException exc) {
            recordRun(startedAt, "FAILED", null, null, ImportFailure.of(exc));
            retryOrGiveUp(attempt, exc);
        } catch (final InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    // an imported version gets recorded along with its banks; not recording another run must not trigger a retry
    private void recordRun(final Instant startedAt, final String outcome, final URI currentUrl, final Integer bankCount,
            final ImportFailure failure) {
        try {
            jdbc.update("""
                            insert into hs_office.bank_directory_import (startedAt, outcome, sourceUrl, bankCount,
                                    failureKind, failureRequest, failureHttpStatus, failureLine)
                                values (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Timestamp.from(startedAt), outcome, currentUrl == null ? null : currentUrl.toString(), bankCount,
                    failure == null ? null : failure.kind.name(), failure == null ? null : failure.request,
                    failure == null ? null : failure.httpStatus, failure == null ? null : failure.line);
            jdbc.update("delete from hs_office.bank_directory_import where outcome <> 'IMPORTED' and startedAt < ?",
                    Timestamp.from(startedAt.minus(RUN_RETENTION)));
        } catch (final RuntimeException exc) {
            log.warn("bank directory import run of {} could not be recorded", startedAt, exc);
        }
    }

    // a failure might be temporary, e.g. bundesbank.de being down, but giving up means the directory might get outdated
    private void retryOrGiveUp(final int attempt, final Exception exc) {
        if (attempt < maxAttempts) {
            val delay = retryDelay.multipliedBy(1L << (attempt - 1));
            log.warn("bank directory import from {}: attempt {} of {} failed, next attempt in {}",
                    sourceUrl, attempt, maxAttempts, delay, exc);
            scheduler.schedule(() -> importIfNewVersion(attempt + 1), Instant.now().plus(delay));
        } else {
            log.error("bank directory import from {} failed {} times, the possibly outdated previous version stays in use",
                    sourceUrl, maxAttempts, exc);
        }
    }

    // the import stays valid even if a check against it fails
    private void checkAgainst(final URI currentUrl) {
        try {
            events.publishEvent(new Imported(currentUrl));
        } catch (final RuntimeException exc) {
            log.warn("bank directory {} is imported, but a check against it failed", currentUrl, exc);
        }
    }

    /** The configured URL redirects to the current version, whose URL thus identifies it. */
    private URI currentVersionUrl() throws IOException, InterruptedException {
        val request = "HEAD " + sourceUrl;
        val response = send(request,
                HttpRequest.newBuilder(sourceUrl).method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(TIMEOUT).build(),
                HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 200) {
            return sourceUrl;
        }
        val location = response.headers().firstValue("Location");
        if (response.statusCode() / 100 == 3 && location.isPresent()) {
            return sourceUrl.resolve(location.get());
        }
        throw new ImportFailure(ImportFailure.Kind.HTTP_ERROR, request, response.statusCode(), null,
                "HTTP " + response.statusCode() + " without redirect for " + sourceUrl, null);
    }

    private <B> HttpResponse<B> send(final String request, final HttpRequest httpRequest,
            final HttpResponse.BodyHandler<B> bodyHandler) throws ImportFailure, InterruptedException {
        try {
            return http.send(httpRequest, bodyHandler);
        } catch (final IOException exc) {
            throw new ImportFailure(ImportFailure.Kind.NO_RESPONSE, request, null, null, exc.toString(), exc);
        }
    }

    private boolean alreadyImported(final URI currentUrl) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from hs_office.bank_directory_import where outcome = 'IMPORTED' and sourceUrl = ?)",
                Boolean.class, currentUrl.toString()));
    }

    private String download(final URI currentUrl) throws IOException, InterruptedException {
        val request = "GET " + currentUrl;
        val response = send(request,
                HttpRequest.newBuilder(currentUrl).GET().timeout(TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        if (response.statusCode() != 200) {
            throw new ImportFailure(ImportFailure.Kind.HTTP_ERROR, request, response.statusCode(), null,
                    "HTTP " + response.statusCode() + " for " + currentUrl, null);
        }
        return response.body();
    }

    private static BankCodeFile parse(final URI currentUrl, final String csv) throws ImportFailure {
        try {
            return parse(csv);
        } catch (final ImportFailure failure) {
            throw new ImportFailure(failure.kind, "GET " + currentUrl, null, failure.line, failure.getMessage(), failure);
        }
    }

    /** Only the record with Merkmal 1 carries the BIC of a bank code, 'D' marks a bank code as deleted. */
    static BankCodeFile parse(final String csv) throws ImportFailure {
        val lines = csv.lines().toList();
        if (lines.stream().allMatch(String::isBlank)) {
            throw ImportFailure.invalidFile(null, "empty bank directory");
        }
        val columns = Arrays.asList(fields(lines.getFirst()));
        if (!columns.containsAll(REQUIRED_COLUMNS)) {
            throw ImportFailure.invalidFile(1, "bank directory lacks some of the columns " + REQUIRED_COLUMNS + ": " + columns);
        }
        val banks = new ArrayList<Bank>();
        val deletedBankCodes = new TreeSet<String>();
        for (int lineNumber = 2; lineNumber <= lines.size(); ++lineNumber) {
            val line = lines.get(lineNumber - 1);
            if (line.isBlank()) {
                continue;
            }
            val fields = fields(line);
            if (fields.length != columns.size()) {
                throw ImportFailure.invalidFile(lineNumber,
                        "bank directory line with " + fields.length + " instead of " + columns.size() + " fields: " + line);
            }
            if (fields[columns.indexOf("Änderungskennzeichen")].equals("D")) {
                deletedBankCodes.add(fields[columns.indexOf("Bankleitzahl")]);
            } else {
                bankOf(columns, fields).ifPresent(banks::add);
            }
        }
        if (banks.isEmpty()) {
            throw ImportFailure.invalidFile(null, "bank directory without any bank");
        }
        return new BankCodeFile(banks, deletedBankCodes);
    }

    private static Optional<Bank> bankOf(final List<String> columns, final String[] fields) {
        if (!fields[columns.indexOf("Merkmal")].equals("1")) {
            return Optional.empty();
        }
        val bic = fields[columns.indexOf("BIC")].strip();
        return Optional.of(new Bank(
                fields[columns.indexOf("Bankleitzahl")],
                bic.isEmpty() ? null : bic,
                fields[columns.indexOf("Bezeichnung")].strip()));
    }

    /** Splits a line of semicolon separated, optionally double-quoted fields. */
    private static String[] fields(final String line) {
        val fields = new ArrayList<String>();
        val field = new StringBuilder();
        var quoted = false;
        for (int i = 0; i < line.length(); ++i) {
            val c = line.charAt(i);
            if (isEscapedQuote(line, i, quoted)) {
                field.append('"');
                ++i;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == ';' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(String[]::new);
    }

    /** Inside a quoted field, two double quotes stand for one. */
    private static boolean isEscapedQuote(final String line, final int i, final boolean quoted) {
        return quoted && line.charAt(i) == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"';
    }

    /** The Bundesbank supersedes the German banks seeded from the existing bank accounts and those entered by hand. */
    private void replaceBundesbankRows(final URI currentUrl, final BankCodeFile file) {
        val banks = file.banks();
        jdbc.update("delete from hs_office.bank where source = 'BUNDESBANK'");
        jdbc.batchUpdate("""
                        insert into hs_office.bank (countryCode, bankCode, bic, name, source)
                            values ('DE', ?, ?, ?, 'BUNDESBANK')
                            on conflict (countryCode, bankCode) do update
                                set bic = excluded.bic, name = excluded.name, source = excluded.source
                                where hs_office.bank.source in ('EXISTING', 'MANUAL')
                        """,
                banks, 500, (statement, bank) -> {
                    statement.setString(1, bank.bankCode());
                    statement.setString(2, bank.bic());
                    statement.setString(3, bank.name());
                });
        // the Bundesbank forbids using a deleted bank code, thus it must not survive as a seeded or manual entry either
        jdbc.batchUpdate("delete from hs_office.bank where countryCode = 'DE' and bankCode = ? and source in ('EXISTING', 'MANUAL')",
                List.copyOf(file.deletedBankCodes()), 500, (statement, bankCode) -> statement.setString(1, bankCode));
        jdbc.update("insert into hs_office.bank_directory_import (outcome, sourceUrl, bankCount) values ('IMPORTED', ?, ?)",
                currentUrl.toString(), banks.size());
    }
}
