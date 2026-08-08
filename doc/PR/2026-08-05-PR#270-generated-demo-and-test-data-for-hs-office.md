# PR#270: Konzept für generierte Demo- und Testdaten für das hs_office-Modul

Der PR beginnt mit dem Konzept; die Implementierung folgt in diesem Branch, sobald das Konzept abgestimmt ist.

## Das Problem

Für Vorführungen vor Kunden und für realitätsnahe Entwicklungsarbeit fehlt ein Datenbestand in Produktionsgröße.

Die vorhandenen `*-test-data.sql`-Changesets unter `src/main/resources/db/changelog` (Liquibase-Context `!without-test-data`)
sind bewusst klein und auf Verwendung in Integrationstests ausgelegt.
Ihre Personen und Firmen heißen `Susan Firby`, `First GmbH`, `Third OHG`, `Fourth eG`.
Für eine Vorführung vor Genossenschaftsmitgliedern ist das nicht verwendbar.

Daraus ergeben sich vier Anforderungen:

- Eine Vorführung vor Mitgliedern braucht Daten, die echt aussehen, aber keine realen Personen betreffen.
- Entwicklung und Fehlersuche brauchen realistische Mengen und Verflechtungen, nicht lediglich ein Dutzend Beispielsätze.
- Spätere Performance-Messungen an den Restricted Views brauchen einen Grant-Graphen in realistischer Größe.
- Der Datenbestand muss reproduzierbar sein, damit Vorführungen und Messungen wiederholbar bleiben.

In der Produktion existieren derzeit nur Office-Daten; für Booking und Hosting gibt es dort noch keinen Datenbestand.

## Nicht-Ziele

- Keine Booking- und Hosting-Daten.
  Sie folgen später mit etwa 5 Booking Items und 50 Hosting Assets je Debitor
  und brauchen wegen der abgeleiteten Identifier und der Asset-Hierarchie einen eigenen Entwurf.
- Keine Anonymisierung einer Produktionskopie.
  Sie bleibt für Support-Nachstellung und Migrationstests im Haus sinnvoll, ist aber ein separates Vorhaben.

`pr-quick-check:max-scope-score=72`: Ein Generator für Massendaten besteht aus Wortlisten, Archetypen,
Wertegeneratoren, Use-Cases und Invarianten; diese Teile ergeben erst zusammen einen lauffähigen Datensatz
und lassen sich nicht sinnvoll auf mehrere PRs verteilen.
Der feste Personensatz kommt hinzu, weil ohne ihn niemand sich am Ergebnis anmelden kann.
Sieben der Punkte entstehen erst durch die Auslagerung nach RFC#0005:
`is_substantive` nimmt allein das PR-Dokument von der Zeilenzählung aus, ein RFC dagegen nicht,
sodass unveränderter Text durch den Umzug überhaupt erst gezählt wird.

## Die Szenarien

Die Szenarien beschreiben den Generator selbst, nicht ein Anwendungsverhalten;
das Ergebnis ist ein Datenbestand, kein fachlicher Anwendungsfall.

### Feature: Erzeugung eines Demo-Datenbestands

#### Scenario#270.01: Minimalbestand im CI-Lauf

- **Gegeben** keine gesetzte Kundenzahl und keine CSV-Datei mit festem Personensatz,
- **wenn** der Generator läuft,
- **dann** entstehen fünf Partner mit vollständigem Office-Geflecht,
- **und** alle Invarianten aus der Lösung sind eingehalten,
- **und** der Scenario-Report wird geschrieben.

#### Scenario#270.02: Vollbestand für die Vorführung

- **Gegeben** die Kundenzahl 500,
- **wenn** der Generator läuft,
- **dann** entstehen 500 Partner mit der beschriebenen Archetypen-Verteilung,
- **und** der Scenario-Report wird nicht geschrieben.

#### Scenario#270.03: Fester Personensatz

- **Gegeben** eine CSV-Datei mit den Anmeldenamen aus dem Keycloak-Realm,
- **wenn** der Generator läuft,
- **dann** entsteht je Anmeldenamen-Präfix ein Kunde,
  dreibuchstabige Präfixe als Privatperson und längere als GmbH,
- **und** jede darin genannte Person ist `REPRESENTATIVE` ihres Kunden und einer erzeugten `eG`,
- **und** ihr Vor- und Nachname kommt im Datenbestand genau einmal vor,
- **und** `tools/natural-partner-person-by-name` findet sie eindeutig.

#### Scenario#270.04: Wiederholbarkeit

- **Gegeben** derselbe Seed und dieselbe Kundenzahl,
- **wenn** der Generator zweimal läuft,
- **dann** sind beide Datenbestände fachlich gleich.

#### Scenario#270.05: Personen-UUIDs für die Account-Erzeugung

- **Gegeben** eine CSV-Datei, in der ein Mensch einen Mitglieds- und einen `hostsharing-`-Anmeldenamen hat,
- **wenn** der Generator läuft,
- **dann** entsteht die Datei mit gefüllter Spalte `personUuid` daneben,
- **und** beide Anmeldenamen dieses Menschen tragen dort dieselbe `personUuid`,
- **und** diese verweist auf genau eine Person im Datenbestand.

## Die Lösung

Ein Generator erzeugt deterministisch rund 500 Partner mit dem vollständigen Office-Geflecht.
Die Anzahl ist parametrisierbar; ohne Angabe entstehen fünf Kunden.

Er ist als Scenario-Test im `UseCase`-Stil gebaut und trägt den vorhandenen Tag `scenarioTest`.
Damit läuft er bei jedem CI-Build mit dem Minimalbestand von fünf Kunden mit;
größere Zahlen werden nur lokal über die Property gesetzt.
Die Daten entstehen über die REST-API und werden anschließend als Dump-Artefakt konserviert;
Aufbau und Rücksetzen eines Demo-Systems erfolgen über den Dump, nicht über einen erneuten Generatorlauf.

Ein Anomalie-Schalter trennt die beiden Ausprägungen desselben Generators:
Der Demo-Datensatz ist durchgängig vollständig und sauber,
der Entwickler-Datensatz enthält zusätzlich bewusste Datenqualitätsmängel.

Optional beginnt der Lauf mit einem Satz fest definierter natürlicher Personen aus dem Keycloak-Realm.
Je Anmeldenamen-Präfix entsteht ein Kunde, und der Generator schreibt die Personen-UUIDs zurück,
sodass sich nach einem Sync Accounts erzeugen lassen und in der Vorführung angemeldete Benutzer auftreten.

Mindestens eine `eG` ist kein Zufallsprodukt, sondern trägt eine vollständige Mitgliedschaftshistorie,
weil die Vorführung nach der Verwaltungssicht auch die Sicht eines Mitglieds zeigt.
Sie entsteht immer, auch im Minimalbestand, und ist zugleich der Anker für den festen Personensatz.

Der vollständige Entwurf steht in
[RFC#0005](../rfc/2026-08-06-RFC%230005-generated-demo-and-test-data-for-hs-office.md):
Mengengerüst, Archetypen-Verteilung, Invarianten, IBAN-Berechnung, Kontakt-Captions, der feste Personensatz,
die Trennung des Datenbestands sowie die verworfenen Alternativen und die Performance-Abschätzungen.
Er ist dort festgehalten, weil er die einzelnen PRs überdauert,
während dieses Dokument nach dem Merge nicht weiter gepflegt wird.

### Umsetzung

Der Generator liegt im Test-Sourceset bei den übrigen Scenario-Tests
und wird über einen eigenen Gradle-Task aufgerufen, nach dem Vorbild von `importHostingAssets`
in `buildSrc/src/main/kotlin/HsadminTestTasksPlugin.kt`.

Dass der Generator nicht im Anwendungs-Jar liegt, ist kein Hindernis, sondern der Zweck:
`ScenarioTest` ist mit `@SpringBootTest(webEnvironment = RANDOM_PORT)` annotiert und startet die vollständige Anwendung,
gegen die `UseCase` echte HTTP-Requests schickt.
Der Generator braucht also keine Anwendung, die ihn enthält — er bringt sie mit.
Auf dem Demo-Server läuft niemals Generatorcode, dorthin gelangen ausschließlich Daten.

Der Dump ist kein Bestandteil des Generators.
`PostgresTestcontainer.dump(...)` schied dafür aus zwei Gründen aus:
Die Architekturregel `hsOfficeMigrationPackageRule` macht `hs.migration` bewusst zu einem Blatt, auf das niemand zugreift,
und der Helfer schreibt einen auf den Liquibase-Referenzdump zugeschnittenen Kopf, der in einem Demo-Dump falsch wäre.

Für den vollen Lauf zeigt `HSADMINNG_POSTGRES_JDBC_URL` deshalb auf eine dauerhafte Datenbank statt auf den Testcontainer,
und der Dump entsteht danach mit `pg_dump` als Betriebsschritt;
der Ablauf steht in `doc/demo-datasets.md`, weil er den Datenbanknamen des jeweiligen Datensatzes braucht.

## Offene Punkte

Der Datenbestand für die Vorführung ist mit 500 Kunden erreicht und gemessen.
Für Skalierungs- und Performance-Tests mit etwa 5.000 Kunden reicht der Generator nicht;
die folgenden Punkte sind dafür zu lösen.

- [ ] TODO.test: Einen Skalierungsmodus erwägen, der Realismus gegen Menge tauscht,
      falls einmal mit sehr großen Datenmengen getestet werden soll:
      laufende Nummern statt eindeutig gezogener Namen und keine Prüfabfragen je Entität.
      Die Anforderungen der Vorführung blieben davon unberührt.
      Am Schreibaufkommen ändert das allerdings wenig, siehe die Laufzeitabschätzungen im RFC.
- [ ] TODO.impl: Ein zu langer Feldwert führt zu `409 CONFLICT` statt zu einem Validierungsfehler.
      `hs_office.bankaccount.holder` ist `varchar(64)`; PostgreSQL meldet `string_data_right_truncation`,
      Spring verpackt das als `DataIntegrityViolationException`,
      und `RestResponseEntityExceptionHandler` bildet die auf `409 CONFLICT` ab.
      Die Meldung zeigt damit in die falsche Richtung, und das trifft jeden API-Aufruf mit zu langem Wert.
- [ ] TODO.perf: Der Generator legt alles als Global Admin an.
      Für Neukunden entspricht das dem Betrieb, denn die werden immer von Global Admins aufgesetzt.
      Weitere Personen, Kontaktdaten, Mailinglisten und Abos legen dagegen Administratoren der Kunden an,
      also normale Benutzer ohne Global-Admin-Rolle.
      Deren Lesezugriffe nehmen nicht den Kurzschluss in `rbac.select_*_rv` (`1058-rbac-generators.sql:203`),
      sondern laufen über `rbac.queryAccessibleObjectUuidsOfSubjectIds` durch den Grant-Graphen.
      Ein Teil der Daten sollte deshalb als normaler Benutzer erzeugt werden.
      Das bringt zweierlei: belastbare Zahlen für den Pfad ohne die Abkürzung,
      und realistischere Rechte-Strukturen im Demo-Datensatz.
      Folge-PR, nicht Bestandteil dieses.
- [x] TODO.ops-dump: Den Dump dem Betrieb bereitstellen, zusammen mit
      `.private/keycloak-users-500-with-person-uuids.csv`, deren UUIDs nur zu diesem einen Datensatz passen.
      Erzeugt und geprüft ist er als `demo-dataset-500-2026-08-07.dump`, 13 MB im Format `custom`,
      ohne `rbac.api_key`, `rbac.api_key_scope` und `base.tx_journal`.
      Bereitgestellt für den backend auf dev-Stage unter `db-dumps` mit Datumsstempel.
      Dass die Anwendung mit `without-test-data` laufen muss, wurde Ops mitgeteilt.
- [x] Den Branch vor dem Merge als Ganzes durchsehen und aufräumen.
- [x] Der Dump enthält die festen Personen, aber weder Subjects noch Accounts;
      die entstehen nach dem Restore über die Werkzeugkette, damit das Artefakt realm-unabhängig bleibt.
      Über die Spalte `subjectUuid` könnte der Generator beides gleich miterzeugen, siehe RFC#0005,
      wir wollen das aber über den Subject-Sync-Prozess machen, um die Integration zu testen.
      Für den rein lokalen Blick auf die Daten genügt das User-Subject `hsh-demo_admin`.
- [x] Telefonnummern stammen aus den für Medienproduktionen reservierten Blöcken (Mitteilung 148/2021):
      Festnetz nur in den fünf reservierten Ortsnetzen, alle anderen Orte bekommen Mobilfunk, siehe RFC#0005.
- [ ] TODO.tool: Die hier aufgefallenen Punkte an `tools/pr-quick-check` stehen im Dokument zu PR#275.

## Voraussetzungs-PRs

- PR#271 (Index auf `base.tx_context.seqTxId`) — gemergt als `eb6282cf` und in diesem Branch enthalten.
  Er löst den quadratischen Laufzeitanteil auf, der bei der Erzeugung großer Bestände dominiert;
  technisch unabhängig, aber Voraussetzung dafür, dass ein Lauf mit 5.000 Kunden in vertretbarer Zeit endet.
  Die Laufzeitangaben im RFC setzen ihn voraus.
- PR#274 (Auth-Konfigurations-Endpunkt ) - gemerged als `b954312f` und in diesem Branch enthalten.
  Stellt die Backend-Auth-Config auch dem Frontend zur Verfügung, vermeided fehlerträchtige Redundanzen.

## Folge-PRs

- Booking- und Hosting-Daten mit eigenem Entwurf für die Asset-Hierarchie.
