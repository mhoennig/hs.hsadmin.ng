package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;

public class CreateBankAccount extends UseCase<CreateBankAccount> {

    private final FakeLoginUser loginUser;

    public CreateBankAccount(final ScenarioTest testSuite) {
        this(testSuite, asGlobalAgent());
    }

    public CreateBankAccount(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        val response = withTitle("Create bank account %{holder}", () ->
                httpPost(loginUser, "/api/hs/office/bankaccounts", usingJsonBody("""
                        {
                            "holder": ${holder},
                            "iban": ${iban},
                            "bic": ${bic???},
                            "overrideBankDirectory": ${overrideBankDirectory???}
                        }
                        """),
                        request -> ScenarioTest.hasProperty("assumedRoles")
                                ? request.header("Hostsharing-Assumed-Roles", ScenarioTest.resolve("%{assumedRoles}", DROP_COMMENTS))
                                : request)
                        .reportWithResponse().expecting(expectedStatus));
        if (expectedStatus.isError()) {
            // UseCase calls verify() only for successful responses
            response.path("message").contains(ScenarioTest.resolve("%{errorMessage}", DROP_COMMENTS));
            return response;
        }
        return response.expecting(JSON);
    }

    @Override
    protected void verify(final HttpResponse response) {
        path("bic").contains("%{expectedBic}").accept(response);
    }
}
