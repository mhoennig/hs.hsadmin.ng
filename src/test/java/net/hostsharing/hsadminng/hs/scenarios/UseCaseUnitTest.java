package net.hostsharing.hsadminng.hs.scenarios;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UseCaseUnitTest {

    @Test
    void reportsOnlyTheFakeAuthorizationHeaderAsAuthorization() {
        assertThat(UseCase.reportableRequestHeaderName("X-Fake-Authorization")).isEqualTo("Authorization");
        assertThat(UseCase.reportableRequestHeaderName("x-fake-authorization")).isEqualTo("Authorization");
        assertThat(UseCase.reportableRequestHeaderName("Content-Type")).isEqualTo("Content-Type");
        assertThat(UseCase.reportableRequestHeaderName("Hostsharing-Assumed-Roles")).isEqualTo("Hostsharing-Assumed-Roles");
    }

    @Test
    void reportsRequestHeadersInScenarioReportOrder() {
        final var requestHeaders = new LinkedHashMap<String, List<String>>();
        requestHeaders.put("Content-Type", List.of("application/json"));
        requestHeaders.put("X-Other", List.of("other"));
        requestHeaders.put("X-Fake-Authorization", List.of("Bearer [some-user]"));
        requestHeaders.put("Hostsharing-Assumed-Roles", List.of("rbactest.customer#yyy:OWNER"));
        requestHeaders.put("Authorization", List.of("Bearer real.jwt.token"));

        assertThat(UseCase.reportableRequestHeaders(requestHeaders, true).stream()
                .map(Map.Entry::getKey)
                .toList())
                .containsExactly("Authorization", "Hostsharing-Assumed-Roles", "X-Other", "Content-Type");
    }

    @Test
    void reportsRequestBodyAsPrettyJsonDataArgument() {
        assertThat(UseCase.requestBodyArgument("""
                {
                  "personType": "LEGAL_PERSON",
                  "tradeName": "Test AG"
                }
                """)).isEqualTo("""
                -d '{
                  "personType" : "LEGAL_PERSON",
                  "tradeName" : "Test AG"
                }'""");
    }

    @Test
    void reportsBearerJwtHeaderValueAsPrettyJsonClaims() {
        assertThat(UseCase.reportableRequestHeaderValue(
                "Authorization",
                "Bearer JWT {\"sub\":\"uuid<some-user@example.org>\",\"groups\":[\"/xyz-GroupOne\"]}"))
                .isEqualTo("""
                        Bearer JWT {
                          "sub" : "uuid<some-user@example.org>",
                          "groups" : [
                            "/xyz-GroupOne"
                          ]
                        }""");
    }

    @Test
    void limitsReportedJsonArrayElementsWithEllipsisIndicatorOnItsOwnLine() {
        assertThat(UseCase.HttpResponse.limitJsonArrayElements(
                """
                [ { "name": "one" }, { "name": "two" }, { "name": "three" } ]
                """, 2))
                .contains("""
                        [
                          {
                            "name" : "one"
                          },
                          {
                            "name" : "two"
                          },
                          "..."
                        ]""");
    }

    @Test
    void keepsJsonArraysWithinTheLimitUnlimited() {
        assertThat(UseCase.HttpResponse.limitJsonArrayElements("[ 1, 2 ]", 2)).isEmpty();
    }

    @Test
    void keepsNonArrayJsonUnlimited() {
        assertThat(UseCase.HttpResponse.limitJsonArrayElements("{ \"name\": \"one\" }", 1)).isEmpty();
    }

    @Test
    void reportsOnlyActualResponseBodies() {
        assertThat(UseCase.HttpResponse.hasReportableResponseBody(null)).isFalse();
        assertThat(UseCase.HttpResponse.hasReportableResponseBody("")).isFalse();
        assertThat(UseCase.HttpResponse.hasReportableResponseBody(" \n")).isFalse();
        assertThat(UseCase.HttpResponse.hasReportableResponseBody("null")).isFalse();
        assertThat(UseCase.HttpResponse.hasReportableResponseBody(" null \n")).isFalse();
        assertThat(UseCase.HttpResponse.hasReportableResponseBody("{}")).isTrue();
    }
}
