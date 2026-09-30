package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "Bankleitzahl", "Merkmal", "Bezeichnung", "BIC", "Änderungskennzeichen");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final TaskScheduler scheduler;
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
            @Value("${hsadminng.bank-directory.import.url}") final URI sourceUrl,
            @Value("${hsadminng.bank-directory.import.retry-delay}") final Duration retryDelay,
            @Value("${hsadminng.bank-directory.import.max-attempts}") final int maxAttempts) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.scheduler = scheduler;
        this.sourceUrl = sourceUrl;
        this.retryDelay = retryDelay;
        this.maxAttempts = maxAttempts;
    }

    public record Bank(String bankCode, String bic, String name) {
    }

    public record BankCodeFile(List<Bank> banks, Set<String> deletedBankCodes) {
    }

    @Scheduled(
            initialDelayString = "${hsadminng.bank-directory.import.initial-delay}",
            fixedDelayString = "${hsadminng.bank-directory.import.interval}")
    public void importIfNewVersion() {
        importIfNewVersion(1);
    }

    private void importIfNewVersion(final int attempt) {
        try {
            val currentUrl = currentVersionUrl();
            if (alreadyImported(currentUrl)) {
                log.info("bank directory {} is already imported", currentUrl);
                return;
            }
            val file = parse(download(currentUrl));
            transaction.executeWithoutResult(status -> replaceBundesbankRows(currentUrl, file));
            log.info("bank directory {} imported with {} banks", currentUrl, file.banks().size());
        } catch (final IOException | RuntimeException exc) {
            retryOrGiveUp(attempt, exc);
        } catch (final InterruptedException exc) {
            Thread.currentThread().interrupt();
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

    /** The configured URL redirects to the current version, whose URL thus identifies it. */
    private URI currentVersionUrl() throws IOException, InterruptedException {
        val response = http.send(
                HttpRequest.newBuilder(sourceUrl).method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(TIMEOUT).build(),
                HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 200) {
            return sourceUrl;
        }
        val location = response.headers().firstValue("Location");
        if (response.statusCode() / 100 == 3 && location.isPresent()) {
            return sourceUrl.resolve(location.get());
        }
        throw new IOException("HTTP " + response.statusCode() + " without redirect for " + sourceUrl);
    }

    private boolean alreadyImported(final URI currentUrl) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from hs_office.bank_directory_import where outcome = 'IMPORTED' and sourceUrl = ?)",
                Boolean.class, currentUrl.toString()));
    }

    private String download(final URI currentUrl) throws IOException, InterruptedException {
        val response = http.send(
                HttpRequest.newBuilder(currentUrl).GET().timeout(TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + currentUrl);
        }
        return response.body();
    }

    /** Only the record with Merkmal 1 carries the BIC of a bank code, 'D' marks a bank code as deleted. */
    static BankCodeFile parse(final String csv) throws IOException {
        val lines = csv.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            throw new IOException("empty bank directory");
        }
        val columns = Arrays.asList(fields(lines.getFirst()));
        if (!columns.containsAll(REQUIRED_COLUMNS)) {
            throw new IOException("bank directory lacks some of the columns " + REQUIRED_COLUMNS + ": " + columns);
        }
        val banks = new ArrayList<Bank>();
        val deletedBankCodes = new TreeSet<String>();
        for (val line : lines.subList(1, lines.size())) {
            val fields = fields(line);
            if (fields.length != columns.size()) {
                throw new IOException("bank directory line with " + fields.length + " instead of " + columns.size() + " fields: " + line);
            }
            if (fields[columns.indexOf("Änderungskennzeichen")].equals("D")) {
                deletedBankCodes.add(fields[columns.indexOf("Bankleitzahl")]);
            } else {
                bankOf(columns, fields).ifPresent(banks::add);
            }
        }
        if (banks.isEmpty()) {
            throw new IOException("bank directory without any bank");
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
            if (c == '"' && quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
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
