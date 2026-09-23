# PR#300: SEPA-Mandate-API für das Billing

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/300
- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/589

## Das Problem

Das Billing braucht die an einem Stichtag gültigen SEPA-Mandate, sowohl für alle Debitoren als auch für einen einzelnen.
`GET /api/hs/office/sepamandates` filtert bisher nur nach `iban`.
Das Billing müsste alle SEPA-Mandate abrufen und selbst filtern.

## Nicht-Ziele

- Keine SEPA-Mandate in der Response von `GET /api/hs/office/debitors`, denn jeder Abruf der Debitoren zahlte dann die Joins.
- Keine Änderung der Response von `GET /api/hs/office/sepamandates`.

## Die Szenarien

### Feature: SEPA-Mandate für das Billing abrufen

#### Scenario#300.01: Das Billing ruft das am Stichtag gültige SEPA-Mandat eines Debitors ab

&hellip; damit das Billing die Lastschrift vom richtigen Konto einzieht.

- **Given** ein Debitor mit einem abgelaufenen und einem gültigen SEPA-Mandat
- **When** die SEPA-Mandate mit `GET /api/hs/office/sepamandates?debitorNumber=...&validAt=...` abgerufen werden
- **Then** enthält die Liste nur das am Stichtag gültige SEPA-Mandat des Debitors

##### Verified by

- [HsOfficeScenarioTests.shouldListValidSepaMandatesOfDebitor](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ListValidSepaMandatesOfDebitor](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/debitor/ListValidSepaMandatesOfDebitor.java)
- CI-Report des Branches (Hostsharing-intern):
  [3102: List Valid Sepa Mandates of Debitor](https://vm4006.hostsharing.net/branches/mihoe_sepamandates-api-for-billing-ng/reports/scenarios/3102%3A%20List%20Valid%20Sepa%20Mandates%20of%20Debitor.html)
- CI-Report auf `master` (Hostsharing-intern), verfügbar nach dem Merge und dem nächsten `release`-Build:
  [3102: List Valid Sepa Mandates of Debitor](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/3102%3A%20List%20Valid%20Sepa%20Mandates%20of%20Debitor.html)

#### Scenario#300.02: Das Billing ruft alle am Stichtag gültigen SEPA-Mandate ab

&hellip; damit das Billing alle Lastschriften eines Rechnungslaufs mit einem einzigen Request vorbereitet.

- **Given** mehrere Debitoren mit SEPA-Mandaten
- **When** die SEPA-Mandate mit `GET /api/hs/office/sepamandates?validAt=...` abgerufen werden
- **Then** enthält die Liste die am Stichtag gültigen SEPA-Mandate aller Debitoren
    - **and** kein abgelaufenes SEPA-Mandat

##### Verified by

- [HsOfficeScenarioTests.shouldListAllValidSepaMandates](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ListAllValidSepaMandates](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/debitor/ListAllValidSepaMandates.java)
- CI-Report des Branches (Hostsharing-intern):
  [3103: List All Valid Sepa Mandates](https://vm4006.hostsharing.net/branches/mihoe_sepamandates-api-for-billing-ng/reports/scenarios/3103%3A%20List%20All%20Valid%20Sepa%20Mandates.html)
- CI-Report auf `master` (Hostsharing-intern), verfügbar nach dem Merge und dem nächsten `release`-Build:
  [3103: List All Valid Sepa Mandates](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/3103%3A%20List%20All%20Valid%20Sepa%20Mandates.html)

## Die Lösung

- `GET /api/hs/office/sepamandates` erhält die optionalen Query-Parameter `debitorUuid`, `debitorNumber` und `validAt`.
- `debitorUuid` und `debitorNumber` schließen sich aus, beide zusammen ergeben `400 Bad Request`.
- `validAt` wählt die SEPA-Mandate, deren `validity` den Stichtag enthält.
  Die Repository-Query prüft das mit dem PostgreSQL-Operator `@>` über die HQL-Funktion `sql()`.
  Sie erhält den Stichtag als typisierten Text, denn einen `null`-Parameter ohne Typ bindet Hibernate als `bytea`.
- Der Controller löst `debitorNumber` über `HsOfficeDebitorRepository` in die UUID auf.
  Eine unbekannte `debitorNumber` ergibt eine leere Liste.
- Alle Query-Parameter wirken zusammen mit `iban` als UND-Verknüpfung.

## Zusätzliche Änderungen

- `CreateSepaMandateForDebitor` akzeptiert ein optionales `mandateValidTo`.
  Damit legt `shouldCreateFormerSepaMandateForDebitor` das abgelaufene SEPA-Mandat für beide Szenarien an.
- Die Szenarien des Billings rufen mit einem read-only API-Key ab, wie später das Billing, statt als globaler Admin.
  Das gilt auch für die beiden Debitor-Szenarien aus PR#299.
  Die neue Basisklasse `BillingUseCase` legt den API-Key über `CreateReadOnlyApiKeySubject` an, ohne Ausgabe im Report.
- Das PR-doc von PR#299 erhält die Links auf die CI-Reports auf `master`.
  `pr-quick-check:extra-pr-docs=1`: die Links auf `master` gibt es erst nach dem Merge von PR#299
- `tools/pr-quick-check` lehnt `final var` in hinzugefügten Java-Zeilen ab und verlangt Lombok `val`.
  Der Agenten-Hook `tools/hooks/reject-final-var.sh` sieht nur `Write` und `Edit`, aber keinen Java-Code aus der Shell.

---

## Release Note

released: 2026-09-21

### SEPA mandates can be filtered by debitor and key date

Since 2026-09-21, `GET /api/hs/office/sepamandates` accepts the query parameters `debitorUuid`, `debitorNumber` and `validAt`.
`validAt` returns only the SEPA mandates which are valid at the given date.
