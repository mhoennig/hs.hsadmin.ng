# PR#264: Tooling-, Dependency- und Request-Body-Härtung aus der zweiten Runde des Backend-Security-Audits

## Relevante Links (Hostsharing-intern)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/545
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/264

## Aufteilung der zweiten Audit-Runde

Der ursprüngliche Branch der zweiten Audit-Runde wurde zu groß und ist in drei PRs aufgeteilt:

- [PR#260](2026-07-30-PR%23260-security-hardening-second-audit-round.md):
  Fixes in den Modulen Office und Accounts, dazu ein RBAC-Regressionstest (Java und SQL).
- PR#264 (dieser PR): Fixes im Tooling, den Dependencies und dem Request-Body-Filter.
- [PR#265](2026-08-03-PR%23265-security-hardening-booking-hosting.md):
  Fixes im Booking/Hosting-Bereich (Java und SQL).

## Das Problem

Das Backend-Security-Audit (Hostsharing-intern) meldete neben den Autorisierungslücken
einen Ressourcen-Befund im Request-Pfad, dazu CVE-Meldungen im OWASP-Report.
Beim Beheben zeigten sich außerdem Defekte in den Betriebs- und Monitoring-Tools.
Die Befund-Nummern folgen dem Audit-Bericht vom 2026-07-27.

- A-10: der Request-Body-Cache las unbegrenzt, und das vor der Autorisierung.
- OWASP-Report: gemeldete CVEs in Tomcat, swagger-ui und weiteren Dependencies;
  dazu False-Positives und ein OSS-Index-Analyzer, der ohne Credentials die ganze Analyse scheitern ließ.
- `remote backend sql` führte nur das erste Wort seines SQL-Statements aus.
- Ein laufendes Ausfall-Backoff konnte einen Sicherheitsalarm bis zu 24 Stunden verzögern.
- `create-api-key-for-keycloak-sync` starb bei einem fehlschlagenden Rollen-Lookup wortlos.

## Nicht-Ziele

- Die fachlichen Autorisierungsänderungen in Office und Accounts gehören zu PR#260.
- Die fachlichen Autorisierungsänderungen in Booking und Hosting gehören zu PR#265.

## Die Lösung

### A-10 — Request-Body-Cache las unbegrenzt

Der globale Request-Body-Caching-Filter las den Body mit `readAllBytes()`,
und das vor jeder Autorisierungsprüfung — schon ohne Authentifizierung ließ sich so Speicher binden.
Der Filter lehnt Bodies über 256 KiB jetzt mit `413` und dem `CustomErrorResponse`-JSON ab
(dessen Konstruktor dafür `public` wurde)
und allokiert dafür höchstens 256 KiB + 1 Byte, auch ohne deklarierte Content-Length.
Die Sonderbehandlung von Form- und Multipart-Bodies ist in
[HttpServletRequestBodyCachingFilter](../../src/main/java/net/hostsharing/hsadminng/context/HttpServletRequestBodyCachingFilter.java)
kommentiert.
Die Commits dieses Fixes nennen den Befund noch REPO-01.

### Dependency-Härtung nach OWASP-Report

- Tomcat-Bump über die Spring-Boot-Version und swagger-ui-Pin gegen die gemeldeten CVEs;
  der dreifache Pin (`build.gradle.kts` plus beide `application.yml`) wird
  vom neuen Gradle-Task `verifySwaggerUiVersionPin` (Teil von `check`) erzwungen.
- Patch-Level oberhalb der Spring-Boot-Verwaltung für Jackson, Logback, Log4j2 und den PostgreSQL-Treiber;
  jeder Eintrag entfällt, sobald die verwaltete Version aufgeholt hat.
- Suppressions für False-Positives in `etc/owasp-dependency-check-suppression.xml`.
- Der OSS-Index-Analyzer läuft nur noch mit konfigurierten Credentials,
  weil er anonyme Anfragen mit `HTTP 401` beantwortet und damit die ganze Analyse scheitern ließ;
  die Registrierung beschreibt das README.

### Tooling-Fixes

- `tools/remote`: `backend sql` führte nur das erste Wort des Statements aus
  und `login-monitor status` verlor das Argument seines `bash -c`
  (ssh fügt die Argumente zu einem String zusammen, den die Login-Shell erneut parst);
  beide Pfade quoten jetzt sauber über den gemeinsamen `unindented`-Filter.
- `tools/login-monitoring-check`: eigene Backoff-Statusdatei je Alarm,
  damit ein laufendes Ausfall-Backoff den `@room`-Sicherheitsalarm nicht mehr verzögert;
  ein erreichter Critical-Check löst einen offenen Failure-Alarm selbst auf;
  die Define-Only-Sperre wirkt jetzt auch bei direkter Ausführung.
  Die Begründungen stehen als Kommentare im Skript.
- `remote login-monitor alerts` validiert `HSADMINNG_LOGIN_MONITOR_ALARM_REPEAT` genauso wie der Checker selbst.
- `tools/create-api-key-for-keycloak-sync` nennt bei einem fehlschlagenden Rollen-Lookup
  jetzt das bereits angelegte API-Key-Subject,
  damit der Rerun nicht in der „existiert bereits"-Sackgasse endet.
- `tools/grant-global-admin-to-subject` hält die `--help`-Zusage des idempotenten Reruns wieder ein:
  `409 Conflict` gilt als Erfolg und wird auch im aktuellen `CustomErrorResponse`-Feld `statusCode` erkannt.
- Die beiden Sync-Check-Variablen des Login-Monitorings sind in `doc/environment-variables.md` dokumentiert;
  dort ist auch die `ALLOWED_ORIGINS`-Zeile korrigiert.

### Test-Nachweis

Der Test zum Befund A-10 wurde *vor* seinem Fix committet;
die Branch-Historie zeigt ihn rot ohne den Fix und grün mit ihm.

- A-10: `HttpServletRequestBodyCachingFilterUnitTest` — zu große Bodies mit und ohne Content-Length
  werden samt `CustomErrorResponse`-Schema abgelehnt, ein Body mit exakt 256 KiB wird angenommen.
- Der Smoke-Test deckt den SQL-Pfad von `remote backend sql` gegen ein gestubbtes `ssh` ab,
  prüft den Sofort-Alarm eines Sicherheitsvorfalls trotz laufendem Ausfall-Backoff
  und den Rerun von `create-api-key-for-keycloak-sync` nach einem fehlgeschlagenen Rollen-Lookup.

## Offene Fragen

- [ ] TODO.tool: das Zusammenspiel der Tools mit dem Environment aufräumen,
  abgestimmt mit dem Frontend-Tooling;
  Details im gleichnamigen Kommentar in `.aliases`.

## Zusätzliche Änderungen

- Kommentar-Präzisierungen ohne Verhaltensänderung:
  `.envrc` (der PATH-Eintrag gilt allen Tools, nicht nur `remote`)
  und `tools/http` (warum Form-Posts auf JSON gezwungen werden).

## Voraussetzungs-PRs

- Keine: dieser PR ist unabhängig von PR#260 und PR#265; die Merge-Reihenfolge ist frei.
