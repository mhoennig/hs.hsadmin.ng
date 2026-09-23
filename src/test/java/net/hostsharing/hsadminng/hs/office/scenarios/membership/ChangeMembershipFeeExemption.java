package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.hasProperty;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class ChangeMembershipFeeExemption extends UseCase<ChangeMembershipFeeExemption> {

    public ChangeMembershipFeeExemption(final ScenarioTest testSuite) {
        super(testSuite);
        introduction("""
                A membership is either liable to the membership fee or exempt from it.
                An exempt membership needs a comment which gives the reason for the exemption,
                a membership which is liable to the fee must not have such a comment.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        obtain("Membership: %{memberNumber}", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/memberships/%{memberNumber}"),
                response -> response.getFromBody("uuid")
        );

        val response = withTitle("Patch the Fee Exemption Into the Membership", () ->
                httpPatch(asGlobalAgent(), "/api/hs/office/memberships/%{Membership: %{memberNumber}}", usingJsonBody("""
                {
                   "membershipFeeBillable": ${membershipFeeBillable},
                   "membershipFeeComment": ${membershipFeeComment???}
                }
                """))
                .expecting(expectedStatus).expecting(JSON)
        );
        if (hasProperty("errorMessage")) {
            response.path("message").contains(ScenarioTest.resolve("%{errorMessage}", DROP_COMMENTS));
        }
        return response;
    }

    @Override
    protected void verify(final UseCase<ChangeMembershipFeeExemption>.HttpResponse response) {
        verify(
                "Verify That the Membership Carries the Fee Exemption",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/memberships/%{Membership: %{memberNumber}}")
                        .expecting(OK).expecting(JSON),
                path("membershipFeeBillable").contains("%{membershipFeeBillable}"),
                hasProperty("membershipFeeComment")
                        ? path("membershipFeeComment").contains("%{membershipFeeComment}")
                        : path("membershipFeeComment").doesNotExist()
        );
    }
}
