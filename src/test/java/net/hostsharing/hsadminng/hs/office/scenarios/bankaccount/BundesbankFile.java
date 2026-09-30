package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

/** Builds a bank-code file in the format of the Bundesbank, with synthetic banks only. */
public final class BundesbankFile {

    public static final String CONTENT_TYPE = "text/csv; charset=ISO-8859-1";

    private static final String HEADER = "Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;"
            + "Prüfzifferberechnungsmethode;Datensatznummer;Änderungskennzeichen;Bankleitzahllöschung;Nachfolge-Bankleitzahl";

    public record Bank(String bankCode, String name, String bic) {
    }

    public static Bank bank(final String bankCode, final String name, final String bic) {
        return new Bank(bankCode, name, bic);
    }

    public static String of(final Bank... banks) {
        return HEADER + "\r\n" + Arrays.stream(banks)
                .map(bank -> "\"%s\";\"1\";\"%s\";\"12345\";\"Beispielstadt\";\"%s\";;\"%s\";\"09\";\"000001\";\"U\";\"0\";\"00000000\""
                        .formatted(bank.bankCode(), bank.name(), bank.name(), bank.bic()))
                .collect(joining("\r\n", "", "\r\n"));
    }

    private BundesbankFile() {
    }
}
