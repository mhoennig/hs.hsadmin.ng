# PR#299: Scenario-Test für die Debitor-API des Billings

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/299
- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/587

## Das Problem

Das Billing braucht von `GET /api/hs/office/debitors` je Debitor:

- die komplette Debitor-Number,
- die Daten der Partner-Person,
- die Daten des Contact.

Taiga#US-587 verlangt dafür einen Ausbau der API, doch die API liefert diese Daten bereits.
Das belegt bisher nur ein Acceptance-Test des Controllers, ein Scenario-Test aus Sicht des Billings fehlt.
Der Szenario-Report dient dem Billing als Anleitung.

## Nicht-Ziele

- Keine Änderung am Produktionscode.
- Keine Filter für SEPA-Mandate, siehe Taiga#US-589.
- Keine Steuer-Verortung des Debitors, siehe Taiga#US-588.

## Die Szenarien

### Feature: Debitor-Daten für das Billing abrufen

#### Scenario#299.01: Das Billing ruft die Debitoren eines Partners mit vollständigen Stammdaten ab

&hellip; damit das Billing Rechnungen adressieren kann, ohne je Debitor weitere Abfragen zu stellen.

- **Given** ein Partner mit einem Debitor
- **When** die Debitoren des Partners mit `GET /api/hs/office/debitors?partnerNumber=...` abgerufen werden
- **Then** enthält der Eintrag des Debitors in `debitorNumber` die komplette Debitor-Number, zum Beispiel `D-1000111`
    - **and** in `partner.partnerRel.holder` die vollständigen Daten der Partner-Person
    - **and** in `debitorRel.contact` die vollständigen Daten des Contact

##### Verified by

- [HsOfficeScenarioTests.shouldListDebitorsWithCompleteMasterData](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ListDebitorsWithCompleteMasterData](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/debitor/ListDebitorsWithCompleteMasterData.java)
- CI-Report des Branches (Hostsharing-intern):
  [2005: List Debitors with Complete Master Data](https://vm4006.hostsharing.net/branches/mihoe_debitor-api-scenario-test/reports/scenarios/2005%3A%20List%20Debitors%20with%20Complete%20Master%20Data.html)
- CI-Report auf `master` (Hostsharing-intern):
  [2005: List Debitors with Complete Master Data](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/2005%3A%20List%20Debitors%20with%20Complete%20Master%20Data.html)

#### Scenario#299.02: Das Billing ruft alle Debitoren mit vollständigen Stammdaten ab

&hellip; damit das Billing alle Debitoren mit einem einzigen Request erhält.

- **Given** mehrere Partner mit Debitoren
- **When** alle Debitoren mit `GET /api/hs/office/debitors` ohne Query-Parameter abgerufen werden
- **Then** enthält die Liste mindestens die erwartete Anzahl von Debitoren
    - **and** ein als Stichprobe gewählter Debitor enthält dieselben Daten wie in Scenario#299.01

##### Verified by

- [HsOfficeScenarioTests.shouldListAllDebitorsWithCompleteMasterData](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ListAllDebitorsWithCompleteMasterData](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/debitor/ListAllDebitorsWithCompleteMasterData.java)
- CI-Report des Branches (Hostsharing-intern):
  [2006: List All Debitors with Complete Master Data](https://vm4006.hostsharing.net/branches/mihoe_debitor-api-scenario-test/reports/scenarios/2006%3A%20List%20All%20Debitors%20with%20Complete%20Master%20Data.html)
- CI-Report auf `master` (Hostsharing-intern):
  [2006: List All Debitors with Complete Master Data](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/2006%3A%20List%20All%20Debitors%20with%20Complete%20Master%20Data.html)

## Die Lösung

- Der Use-Case `ListDebitorsWithCompleteMasterData` ruft die Debitoren eines Partners ab und prüft die drei Angaben.
- Der Use-Case `ListAllDebitorsWithCompleteMasterData` ruft alle Debitoren ab.
  Er prüft die Mindestanzahl und als Stichprobe einen Debitor mit denselben Prüfungen.
- `HsOfficeScenarioTests.DebitorScenarios` führt beide für den Debitor `D-3101100` aus, den sie per `@Requires` voraussetzen.
- Beide Szenario-Reports nennen den Entwicklern des Billings die Felder, die das Billing braucht.
- Spätere Szenarien ändern E-Mail-Adresse und Familienname der Testdaten.
  Die Prüfungen verlangen darum nur eine vorhandene E-Mail-Adresse und den Anfang des Familiennamens.
