# PR#267: Vorhandene Relation-Marks pro Typ auflisten

## Relevante Links (Hostsharing-intern)

- Taiga-User-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/499
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/267

## Das Problem

Ein Nutzer abonniert per API/Frontend eine existierende Mailingliste.
Dazu muss der Nutzer wissen, welche Mailinglisten bereits existieren.
Der Nutzer soll keine Mailingliste abonnieren können, die nicht existiert.

Mailinglisten sind als `HsOfficeRelation` vom Typ `SUBSCRIBER` modelliert,
wobei das freie Textfeld `mark` den Listennamen trägt.
Es gibt bisher keinen Endpunkt, der die vorhandenen `mark`-Werte auflistet.
Jeder Nutzer mit Schreibrecht auf eine `SUBSCRIBER`-Relation kann aktuell einen beliebigen neuen `mark`-Wert anlegen.
Es fehlt eine Beschränkung, wer eine neue Mailingliste erstmalig anlegen darf.

## Nicht-Ziele

- Keine dedizierte Verwaltung von Mailinglisten als eigenes Objekt.
  `mark` bleibt freier Text auf der `SUBSCRIBER`-Relation.
- Keine Änderung an der externen `mlmmj`-Integration (`hs_integration.subscription`-View).

## Die Szenarien

### Feature: Vorhandene Marks pro Relation-Typ abfragen

#### Background

- Eine `HsOfficeRelation` vom Typ `SUBSCRIBER` bildet ein Mailinglisten-Abonnement ab.
  `mark` trägt darin den Listennamen.
- `mark` ist generisch für jeden `HsOfficeRelationType` nutzbar, nicht nur für `SUBSCRIBER`.

#### Scenario#267.01: Nutzer fragt vorhandene Mailinglisten ab

damit der Nutzer weiß, welche Mailinglisten er abonnieren kann.

- **Given** mehrere `SUBSCRIBER`-Relationen mit unterschiedlichen `mark`-Werten existieren
- **When** der Nutzer die vorhandenen Marks für den Typ `SUBSCRIBER` abfragt
- **Then** erhält er die eindeutigen (distinct) `mark`-Werte dieses Typs

##### Verified by

- [HsOfficeScenarioTests.shouldListAvailableMailinglists](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [QueryRelationMarks](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/contact/QueryRelationMarks.java)

#### Scenario#267.02: Abfrage ohne passende Relationen liefert eine leere Liste

damit die Abfrage auch für einen Typ ohne jede Relation ein wohldefiniertes Ergebnis liefert.

- **Given** keine Relation vom Typ `OPERATIONS_ALERT` existiert
- **When** der Nutzer die vorhandenen Marks für den Typ `OPERATIONS_ALERT` abfragt
- **Then** erhält er eine leere Liste

##### Verified by

- [HsOfficeScenarioTests.shouldQueryRelationMarksOfRelationTypeWithoutAnyMarks](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [QueryRelationMarks](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/contact/QueryRelationMarks.java)

### Feature: Zugriffskontrolle für neue Marks

#### Background

- Ein neuer `mark`-Wert für einen Relation-Typ etabliert de facto ein neues Objekt, z.B. eine neue Mailingliste.
- Weitere Relationen mit einem bereits existierenden `mark`-Wert reihen sich dagegen nur in ein bestehendes Objekt ein,
  z.B. ein weiteres Abonnement.
- Als "normaler Nutzer" agiert in den Szenarien ein Partner-Repräsentant,
  der per ReBAC ohnehin berechtigt ist, Relationen mit Anker auf der Partner-Person anzulegen;
  nur so prüft das Szenario die neue Mark-Beschränkung und nicht bloß eine fehlende ReBAC-Berechtigung.

#### Scenario#267.03: Ein normaler Nutzer darf keine neue Mailingliste erstanlegen

damit nicht jeder Nutzer beliebige neue Mailinglisten erfinden kann.

- **Given** noch keine Relation mit dem gewünschten `mark`-Wert für den Typ existiert
- **When** ein normaler Nutzer eine Relation mit diesem neuen `mark`-Wert anlegen will
- **Then** wird die Anfrage mit `403 Forbidden` abgelehnt

##### Verified by

- [HsOfficeScenarioTests.shouldRejectNewMailinglistCreatedByNormalUser](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [SubscribeExistingPersonAndContactToMailinglist](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/subscription/SubscribeExistingPersonAndContactToMailinglist.java)

#### Scenario#267.04: Ein Global-Admin darf eine neue Mailingliste erstanlegen

damit es weiterhin möglich bleibt, neue Mailinglisten einzuführen.

- **Given** noch keine Relation mit dem gewünschten `mark`-Wert für den Typ existiert
- **When** ein Global-Admin eine Relation mit diesem neuen `mark`-Wert anlegt
- **Then** wird sie angelegt

##### Verified by

- [HsOfficeScenarioTests.shouldCreateNewMailinglistAsGlobalAdmin](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [SubscribeExistingPersonAndContactToMailinglist](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/subscription/SubscribeExistingPersonAndContactToMailinglist.java)

#### Scenario#267.05: Weiterer Abonnent für eine bestehende Mailingliste

damit sich normale Nutzer für bereits bestehende Mailinglisten eintragen können.

- **Given** bereits eine Relation mit dem `mark`-Wert für den Typ existiert
- **When** ein normaler Nutzer eine weitere Relation mit demselben `mark`-Wert anlegt
- **Then** wird sie angelegt

##### Verified by

- [HsOfficeScenarioTests.shouldSubscribeAnotherPersonToExistingMailinglistAsNormalUser](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [SubscribeNewPersonAndContactToMailinglist](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/subscription/SubscribeNewPersonAndContactToMailinglist.java)

## Die Lösung

- Der neue Endpunkt `GET /api/hs/office/relations/marks` liefert die eindeutigen `mark`-Werte
  als Paare aus `relationType` und `mark`, sortiert nach Typ und darin alphabetisch.
  Der Query-Parameter `relationType` ist optional und schränkt das Ergebnis auf einen Typ ein;
  ohne ihn kommen die Marks aller Relation-Typen zurück.
  Er funktioniert für jeden `relationType`, wobei derzeit nur bei `SUBSCRIBER` etwas gefunden wird.
- Die Repository-Query läuft bewusst auf der realen Tabelle statt der `_rv`-View:
  die vorhandenen Marks sollen für alle authentifizierten Nutzer sichtbar sein,
  unabhängig von der ReBAC-Zeilensichtbarkeit.
- Ein `BEFORE INSERT OR UPDATE`-Trigger auf `hs_office.relation`
  (Changeset `hs-office-relation-new-mark-CONSTRAINT-BY-TRIGGER`)
  lehnt einen für seinen `relationType` noch nicht existierenden `mark`-Wert mit `[403]` ab,
  sofern der effektive ReBAC-Kontext nicht die Global-Admin-Rolle hat (`rbac.hasGlobalAdminRole()`).
  Die Prüfung erfolgt pro `relationType`.
  Weitere Relationen mit bereits existierendem `mark` bleiben für ReBAC-berechtigte Nutzer offen.
- Die `[403]`-Fehlerkonvention wird vom bestehenden `RestResponseEntityExceptionHandler`
  bereits generisch in HTTP `403 Forbidden` übersetzt, dafür war kein neuer Code nötig;
  der Acceptance-Test `normalUser_canNotAddRelationWithNewMark` sichert Status und Fehlermeldung ab.

---

## Release Note

released: 2026-08-08

### Existing relation marks can be listed, new ones are reserved for global admins

Since 2026-08-04, `GET /api/hs/office/relations/marks` returns the distinct `mark` values together with their `relationType`, optionally narrowed to one type by the `relationType` query parameter.
Every authenticated caller sees all marks, independent of which relations are visible to it.
A relation whose `mark` does not yet exist for its type now requires the global-admin role and is rejected with `403 Forbidden` otherwise, whereas a relation reusing an existing mark stays open to anyone allowed to create it.
