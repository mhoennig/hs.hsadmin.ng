package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ListSepaMandatesWithIssues extends UseCase<ListSepaMandatesWithIssues> {

    public ListSepaMandatesWithIssues(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                After an import of the Bundesbank bank-code file, existing bank accounts might not match anymore,
                e.g. because a BIC changed or a bank code is unknown;
                the administration corrects those which still get debited.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("List the Valid SEPA Mandates With Issues", () ->
                httpGet(asGlobalAdmin(), "/api/hs/office/sepamandates?issues=true")
                        .reportWithResponse().expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        final List<String> holders = response.getFromBody("$[*].bankAccount.holder");
        assertThat(holders)
                .contains(holder("wrong BIC"), holder("unknown bank code"))
                .doesNotContain(
                        holder("without SEPA-mandate, wrong BIC"), holder("expired, wrong BIC"), holder("never valid, wrong BIC"));
        assertThat(mandateField(response, "wrong BIC", "bankAccount.bic")).isEqualTo("FUSNDEB0XXX");
        assertThat(mandateField(response, "wrong BIC", "bankAccount.issues.bankDirectoryMismatch.reason")).isEqualTo("BIC_MISMATCH");
        assertThat(mandateField(response, "wrong BIC", "bankAccount.issues.bankDirectoryMismatch.bankCode")).isEqualTo("98060000");
        assertThat(mandateField(response, "wrong BIC", "bankAccount.issues.bankDirectoryMismatch.expectedBic")).isEqualTo("FUSNDEFFXXX");
        assertThat(mandateField(response, "unknown bank code", "bankAccount.issues.bankDirectoryMismatch.reason")).isEqualTo("UNKNOWN_BANK_CODE");
        assertThat(mandateField(response, "unknown bank code", "bankAccount.issues.bankDirectoryMismatch.bankCode")).isEqualTo("98070000");
        assertThat(mandateField(response, "unknown bank code", "bankAccount.issues.bankDirectoryMismatch.expectedBic")).isNull();
    }

    private static Object mandateField(final HttpResponse response, final String holderSuffix, final String path) {
        final List<Object> values = response.getFromBody(
                "$[?(@.bankAccount.holder == '" + holder(holderSuffix) + "')]." + path);
        assertThat(values).as("%s: %s", holder(holderSuffix), path).hasSizeLessThanOrEqualTo(1);
        return values.isEmpty() ? null : values.getFirst();
    }

    private static String holder(final String holderSuffix) {
        return ScenarioTest.resolve("%{holderPrefix} - " + holderSuffix, DROP_COMMENTS);
    }

    private static FakeLoginUser asGlobalAdmin() {
        return asSubject("hsh-alex_superuser").whichIs("a global admin");
    }
}
