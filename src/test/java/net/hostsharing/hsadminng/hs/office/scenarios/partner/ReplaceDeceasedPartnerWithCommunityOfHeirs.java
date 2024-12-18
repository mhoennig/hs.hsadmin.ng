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

        obtain("Person: Hostsharing eG", () ->
            httpGet("/api/hs/office/persons?name=Hostsharing+eG")
                    .expecting(OK).expecting(JSON),
                    response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "Even in production data we expect this query to return just a single result." // TODO.impl: add constraint?
        );

        obtain("Partner: %{partnerNumber}", () ->
                httpGet("/api/hs/office/partners/%{partnerNumber}")
                .reportWithResponse().expecting(OK).expecting(JSON),
                response -> response.getFromBody("uuid"),
                "Even in production data we expect this query to return just a single result." // TODO.impl: add constraint?
        )
                .extractValue("partnerRel.holder.familyName", "familyNameOfDeceasedPerson")
                .extractValue("partnerRel.holder.givenName", "givenNameOfDeceasedPerson")
                .extractUuidAlias("partnerRel.holder.uuid", "Person: %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}");

        obtain("Partner-Relation: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}", () ->
            httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "PARTNER",
                   "anchor.uuid": ${Person: Hostsharing eG},
                   "holder": {
                        "personType": "UNINCORPORATED_FIRM",
                        "tradeName": "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                    },
                   "contact": {
                        "caption": "Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}",
                        "postalAddress": {
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
                """))
                .reportWithResponse().expecting(CREATED).expecting(JSON)
        )
                .extractUuidAlias("contact.uuid", "Contact: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
                .extractUuidAlias("holder.uuid", "Person: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}");

        obtain("Representative-Relation: %{representativeGivenName} %{representativeFamilyName} for Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}", () ->
                httpPost("/api/hs/office/relations", usingJsonBody("""
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
        ).extractUuidAlias("holder.uuid", "Person: %{representativeGivenName} %{representativeFamilyName}");

        obtain("Partner: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}", () ->
            httpPatch("/api/hs/office/partners/%{Partner: %{partnerNumber}}", usingJsonBody("""
                        {
                            "partnerRel.uuid": ${Partner-Relation: Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}}
                        }
                        """))
                    .expecting(HttpStatus.OK)
        );

        // TODO.test: missing steps Debitor, Membership, Coop-Shares+Assets

        // Debitors

        // die Erbengemeinschaft wird als Anchor-Person (Partner) in die Debitor-Relations eingetragen
        // der neue Rechnungsempfänger (z.B. auch ggf. Rechtsanwalt) wird als Holder-Person (Debitor-Person) in die Debitor-Relations eingetragen -- oder neu?

        // Membership

        // intro: die Mitgliedschaft geht juristisch gesehen auf die Erbengemeinschaft über

        // die bisherige Mitgliedschaft als DECEASED mit Ende-Datum=Todesdatum markieren

        // eine neue Mitgliedschaft (-00) mit dem Start-Datum=Todesdatum+1 anlegen

        // die Geschäftsanteile per share-tx: TRANSFER→ADOPT an die Erbengemeinschaft übertragen
        // die Geschäftsguthaben per asset-tx: TRANSFER→ADOPT an die Erbengemeinschaft übertragen

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
                path("partnerRel.holder.tradeName").contains("Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}")
        );

        // TODO.test: Verify the EX_PARTNER-Relation, once we fixed the anchor problem, see HsOfficePartnerController
        // (net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerController.optionallyCreateExPartnerRelation)

        verify(
                "Verify the Representative-Relation",
                () -> httpGet("/api/hs/office/relations?relationType=REPRESENTATIVE&personUuid=%{Person: %{representativeGivenName} %{representativeFamilyName}}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].anchor.tradeName").contains("Erbengemeinschaft %{givenNameOfDeceasedPerson} %{familyNameOfDeceasedPerson}"),
                path("[0].holder.familyName").contains("%{representativeFamilyName}")
        );

        // TODO.test: Verify Debitor, Membership, Coop-Shares and Coop-Assets once implemented
    }
}
