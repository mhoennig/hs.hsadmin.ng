# PR#282: CI-Build unter 10 Minuten durch selektiven Build-Cache

Der CI-Build nutzt den Gradle-Build-Cache für mechanische Tasks,
während alle Test-Tasks weiterhin bei jedem Build vollständig ausgeführt werden.

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/282

## Das Problem

Der CI-Build (GitTally auf vm2176) dauerte zuletzt 10:15 bis 10:45 Minuten.
Die Messung des Laufs vom 2026-08-10 11:28 ergab:
etwa 3:12 für Start, Konfiguration, Lizenzbericht, OpenAPI-Generierung, Kompilierung und `unitTest`,
etwa 4:52 für die Integrationstests, etwa 1:21 für Dokumentations-PDFs plus `scenarioTest`,
sowie 0:32 für `smokeTest` und `prQuickCheck`.
Wegen `--no-build-cache` und `rm -rf build` liefen auch unveränderte mechanische Tasks in jedem Build erneut.
Ein Cache brachte bisher ohnehin nichts:
der Zeitstempel in der `@Generated`-Annotation der OpenAPI-Quellen änderte bei jedem Lauf den Cache-Schlüssel
von `compileJava` und aller nachgelagerten Tasks.

## Nicht-Ziele

- Kein Test, keine Prüfung und kein Berichts-Artefakt entfällt oder wird seltener ausgeführt.
- Keine Wiederverwendung von Testergebnissen: Test-Tasks laufen in jedem Build vollständig.
- Keine Änderung an Fork-Anzahl oder Test-Verteilung; vm2176 ist mit 4 Kernen bereits ausgelastet.

## Die Lösung

Der `openApiGenerate`-Filter normalisiert den `@Generated`-Zeitstempel,
damit unveränderte Quellen stabile Cache-Schlüssel ergeben.
Die `processSpring*`- und die PDF-Konvertierungs-Tasks werden per `outputs.cacheIf { true }` cachefähig.
Alle Test-Tasks sind per `outputs.cacheIf { false }` dauerhaft vom Cache ausgenommen;
das schützt auch lokale Builds, bei denen der Cache schon immer aktiv war.
Der CI-Build-Befehl in `.gittally.yml` verliert `--no-build-cache` und erhält `--profile`,
sodass künftige Läufe einen Task-Zeitbericht unter `build/reports/profile` archivieren;
GitTally verlinkt ihn unter der stabilen URL `/branches/<branch>/reports/profile/`.
Nachweis rot/grün: vor der Änderung ergab ein zweiter sauberer Lauf nur einen Cache-Treffer (Lizenzbericht);
danach treffen `processSpring*`, `compileJava`, `compileTestJava` und die PDFs,
während `unitTest` trotz identischer Eingaben ausgeführt wird.

Gemessen auf vm2176 am 2026-08-10:
Gradle-Zeit 8:33 vor der Änderung, 8:46 im Cache-befüllenden ersten Lauf, 7:28 im Folgelauf (21 Cache-Treffer);
Gesamtdauer 9:26 statt zuvor 10:15 bis 10:45, mit unverändert ausgeführten 1292 Unit-, 659 Integrations- und 104 Szenario-Tests.

## Umzug auf den neuen Buildserver vm4006

Während der Arbeit an diesem PR wurde der CI-Build von vm2176 (Legacy-GitTally) auf vm4006 mit der neuen GitTally-Implementierung umgestellt.
Die Migration der Konfiguration nach `.gittally.yml` hatte den `buildCommand` versehentlich aus dem Legacy-Stand übernommen,
also mit `--no-build-cache` und ohne `--profile`; das ist inzwischen korrigiert.
Die neue Implementierung nutzt ein wiederverwendbares Worktree pro Branch und ein gemeinsames `GRADLE_USER_HOME`-Volume pro Repository.
Der Build-Cache wirkt dadurch branch-übergreifend, zum Beispiel liefern Master-Builds Cache-Treffer für Feature-Branches.
Da `cleanCommand: rm -rf build` weiterhin vor jedem Build läuft, bleibt der Build-Cache der einzige Mechanismus, der mechanische Tasks einspart.
Die früheren Probleme mit verzögertem Build-Start und dem `rm -rf`-Zeitpunkt des Legacy-GitTally entfallen mit der neuen Implementierung.

Messung auf vm4006 am 2026-08-10 mit Commit `d7b85c3f`:
Gesamtdauer 7:04 bei 7:03 Gradle-Zeit, der GitTally-Overhead von zuvor etwa 2:00 pro Build ist damit verschwunden.
Von 53 Tasks kamen 21 aus dem Cache und 10 waren aktuell, 22 liefen.
Alle Testkategorien wurden ausgeführt:
`unitTest` mit 1292 Tests in 0:37,
`test` mit 659 Integrationstests in 4:49 einschließlich `ImportHostingAssets` und `LiquibaseCompatibilityIntegrationTest`,
`scenarioTest` mit 104 Tests in 0:50,
dazu `smokeTest` in 0:31 und `prQuickCheck`.

## Offene Fragen

- [ ] TODO.tool: Falls der Buildserver mehr Kerne erhält, `HSADMINNG_TEST_MAX_PARALLEL_FORKS` auf 4-5 erhöhen.

## Zusätzliche Änderungen

- Nächtlicher Master-Build um 01:00 UTC per `autoBuild`, auch ohne neue Commits;
  er erkennt schleichende Umgebungs- und Abhängigkeitsprobleme.
- Neue Anleitung `doc/ci-build-server-setup.md` für die Einrichtung des CI-Builds
  auf einem neuen Hostsharing-Container-Server, verlinkt aus der `README.md`.
- Nur der Master-Build erzeugt zusätzlich `bootJarWithDocumentation` und archiviert `build/libs`;
  das Jar mit eingebetteter Dokumentation wiegt 83 MB und kostet einen zweiten `scenarioTest`-Lauf.
- Die `README.md` nannte noch die abgelöste Datei `.gitTally` statt `.gittally.yml`.
