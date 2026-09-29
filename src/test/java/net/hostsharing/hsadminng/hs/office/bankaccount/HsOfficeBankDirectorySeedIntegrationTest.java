package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static net.hostsharing.hsadminng.hs.office.bankaccount.TestHsOfficeBankAccount.hsOfficeBankAccount;
import static org.assertj.core.api.Assertions.assertThat;

// Test databases get their bank accounts only after the seed changeset ran, thus this test runs the seed again.
@RbacDataJpaTest
@Tag("officeIntegrationTest")
class HsOfficeBankDirectorySeedIntegrationTest extends ContextBasedTest {

    private static final String CHANGELOG = "db/changelog/5-hs-office/505-bankaccount/5054-hs-office-bank-directory.sql";
    private static final String CHANGESET_ID = "hs-office-bank-directory-INITIAL-DATA";

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedsEachGermanBankCodeWithTheBicOfAClearMajority() {
        // given
        context("hsh-alex_superuser");
        givenBankAccounts(
                // variants with and without XXX, lower case and blanks count together
                "DE00 9801 0000 0000 0000 01", "bspndeb0xxx",
                "DE00980100000000000002", "BSPNDEB0",
                "DE00980100000000000003", "BSPNDEB1XXX",
                // a tie
                "DE00980200000000000001", "BSPVDEB0",
                "DE00980200000000000002", "BSPXDEB0",
                // only empty BICs
                "DE00980300000000000001", "",
                // a non-German and a structurally invalid BIC do not count
                "DE00980400000000000001", "BSPDATW0",
                "DE00980400000000000002", "BSPD-EB0",
                "DE00980400000000000003", "BSPDDEB0",
                // a foreign IBAN
                "AT00980500000000000001", "BSPOATW0");
        jdbc.update("delete from hs_office.bank");

        // when
        jdbc.execute(seedChangeset());

        // then
        assertThat(seededBanks()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "98010000", "BSPNDEB0 EXISTING",
                "98040000", "BSPDDEB0 EXISTING"));
    }

    private void givenBankAccounts(final String... ibanAndBicPairs) {
        for (int n = 0; n < ibanAndBicPairs.length; n += 2) {
            bankAccountRepo.save(hsOfficeBankAccount("seed test " + n, ibanAndBicPairs[n], ibanAndBicPairs[n + 1]));
        }
        em.flush();
    }

    private Map<String, String> seededBanks() {
        return jdbc.queryForList("select bankCode, bic, source from hs_office.bank where bankCode like '980%'").stream()
                .collect(Collectors.toMap(
                        row -> row.get("bankCode").toString(),
                        row -> row.get("bic") + " " + row.get("source")));
    }

    @SneakyThrows
    private static String seedChangeset() {
        val changelog = new ClassPathResource(CHANGELOG).getContentAsString(StandardCharsets.UTF_8);
        val changesetStart = changelog.indexOf('\n', changelog.indexOf(CHANGESET_ID));
        return changelog.substring(changesetStart, changelog.indexOf("--//", changesetStart));
    }
}
