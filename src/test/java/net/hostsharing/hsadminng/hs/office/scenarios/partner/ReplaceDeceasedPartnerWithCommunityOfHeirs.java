package net.hostsharing.hsadminng.hs.office.scenarios.partner;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class ReplaceDeceasedPartnerWithCommunityOfHeirs extends UseCase<ReplaceDeceasedPartnerWithCommunityOfHeirs> {

    public ReplaceDeceasedPartnerWithCommunityOfHeirs(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Partner: %{partnerNumber}",
                () -> httpGet("/api/hs/office/partners/%{partnerNumber}")
                                .reportWithResponse().expecting(OK).expecting(JSON),
                response -> response.getFromBody("uuid"),
                "Even in production data we expect this query to return just a single result."
                // TODO.impl: add constraint?
        )
                .extractValue("partnerRel.holder.familyName", "familyNameOfDeceasedPerson")
                .extractValue("partnerRel.holder.givenName", "givenNameOfDeceasedPerson")
                .extractUuidAlias(
                        "partnerRel.holder.uuid",
                        "Person: %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}");

        withTitle("New Partner-Person+Contact: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                () -> httpPatch("/api/hs/office/partners/%{Partner: %{partnerNumber}}",
                        usingJsonBody("""
                                {
                                    "wrong1": false,
                                    "partnerRel": {
                                        "wrong2": false,
                                        "holder": {
                                            "personType": "UNINCORPORATED_FIRM",
                                            "tradeName": "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                                        },
                                        "contact": {
                                            "wrong3": false,
                                            "caption": "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                                            "postalAddress": {
                                                "wrong4": false,
                                                "name": "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                                                "co": "%{representativeGivenName} %{representativeFamilyName}",
                                                %{communityOfHeirsPostalAddress}
                                            },
                                            "phoneNumbers": {
                                                "office": ${communityOfHeirsOfficePhoneNumber}
                                            },
                                            "emailAddresses": {
                                                "main": ${communityOfHeirsEmailAddress}
                                            }
                                        }
                                    }
                                }
                                """))
                        .reportWithResponse().expecting(HttpStatus.OK).expecting(JSON)
                        .extractUuidAlias(
                                "partnerRel.holder.uuid",
                                "Person: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
                        .extractUuidAlias(
                                "partnerRel.contact.uuid",
                                "Contact: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
        );

        obtain(
                "Representative-Relation: %{representativeGivenName} %{representativeFamilyName} for Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                () -> httpPost("/api/hs/office/relations",
                        usingJsonBody("""
                                {
                                   "type": "REPRESENTATIVE",
                                   "anchor.uuid": ${Person: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}},
                                   "holder": {
                                        "personType": "NATURAL_PERSON",
                                        "givenName": ${representativeGivenName},
                                        "familyName": ${representativeFamilyName}
                                    },
                                   "contact.uuid": ${Contact: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}}
                                }
                                """))
                        .reportWithResponse().expecting(CREATED).expecting(JSON)
        )
                .extractUuidAlias("holder.uuid", "Person: %{representativeGivenName} %{representativeFamilyName}");

        // outro: die Erbengemeinschaft hat eine Frist von 6 Monaten, um die Mitgliedschaft einer Person zu übertragen
        // →nächster "Drecksfall"

        return null;
    }

    @Override
    protected void verify(final UseCase<ReplaceDeceasedPartnerWithCommunityOfHeirs>.HttpResponse response) {
        verify(
                "Verify the Updated Partner",
                () -> httpGet("/api/hs/office/partners/%{partnerNumber}")
                        .expecting(OK).expecting(JSON).expectObject(),
                path("partnerRel.holder.tradeName").contains(
                        "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}"),
                path("partnerRel.contact.postalAddress").lenientlyContainsJson("§{communityOfHeirsPostalAddress}"),
                path("partnerRel.contact.phoneNumbers.office").contains("%{communityOfHeirsOfficePhoneNumber}"),
                path("partnerRel.contact.emailAddresses.main").contains("%{communityOfHeirsEmailAddress}")
        );

        verify(
                "Verify the Ex-Partner-Relation",
                () -> httpGet(
                        "/api/hs/office/relations?relationType=EX_PARTNER&personUuid=%{Person: %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].anchor.tradeName").contains(
                        "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
        );

        verify(
                "Verify the Representative-Relation",
                () -> httpGet(
                        "/api/hs/office/relations?relationType=REPRESENTATIVE&personUuid=%{Person: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].anchor.tradeName").contains(
                        "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}"),
                path("[0].holder.familyName").contains("%{representativeFamilyName}"),
                path("[0].contact.postalAddress").lenientlyContainsJson("§{communityOfHeirsPostalAddress}"),
                path("[0].contact.phoneNumbers.office").contains("%{communityOfHeirsOfficePhoneNumber}"),
                path("[0].contact.emailAddresses.main").contains("%{communityOfHeirsEmailAddress}")
        );

        verify(
                "Verify the Debitor-Relation",
                () -> httpGet(
                        "/api/hs/office/debitors?partnerNumber=%{partnerNumber}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].debitorRel.anchor.tradeName").contains(
                        "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}"),
                path("[0].debitorRel.holder.tradeName").contains(
                        "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
        );
    }
}
