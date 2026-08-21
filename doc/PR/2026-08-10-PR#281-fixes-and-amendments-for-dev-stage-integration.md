# PR#281: Korrekturen für die Dev-Stage-Integration

Beim Demo-Daten-Import auf der Dev-Stage zeigten sich einige Ungereimtheiten, welcher dieser PR behebt.

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/281

## Das Problem

### Umgebungsabhängige Liquibase-Prüfsummen

Liquibase ersetzte `${HSADMINNG_POSTGRES_RESTRICTED_USERNAME}` vor der Berechnung der Prüfsumme.
Changesets mit dieser Variable hatten daher je Umgebung eine andere Prüfsumme.

Der Demo-Dump bringt die `databasechangelog`-Tabelle mit den lokal berechneten Prüfsummen mit.
Beim Start auf der Dev-Stage meldete Liquibase deshalb drei Abweichungen:
`rbac-api-key-TABLE` und `rbac-api-key-scope-TABLE` in `1086-rbac-api-key.sql`,
sowie `rbac-subject-sync-report-TABLE` in `1087-rbac-subject-sync-report.sql`.
Nur diese drei Tabellen-Changesets betten die `grant`-Zeile direkt ein.
Alle anderen Verwendungen stehen in `runOnChange:true`-Changesets; diese laufen bei abweichender Prüfsumme einfach erneut.

Daraus folgt ein zweites, verstecktes Problem:
Der Dump enthielt die Tabellenrechte nur für den restricted-Benutzer der Quell-Umgebung.
Die Tabellen-Changesets liefen nicht erneut; dem restricted-Benutzer der Ziel-Umgebung fehlten daher die Rechte darauf.

### Fehlende Subject-Delete-Trigger auf älteren Datenbeständen

Auf der int-Stage scheiterte das Löschen von API_KEY-Subjects, die noch Grants hielten.
Die Datenbank meldete Fremdschlüssel-Verletzungen der `rbac.grant`-Tabelle.
Es fehlte der Trigger `delete_grants_of_subject_tg` aus PR#238, der die Grants eines gelöschten Subjects mitlöscht.
Nach einem `changelog-sync` oder einer Checksummen-Zurücksetzung galt sein Changeset als angewendet, ohne je gelaufen zu sein.

### Ein einzelner Deploy-Login reicht nicht mehr

Bisher enthielt `HSADMINNG_DEPLOY_HOST` einen vollständigen SSH-Login, zum Beispiel `hsh03-hsngdev@hsh03`.
Auf der Dev-Stage übernimmt künftig ein Ansible-Skript von Ops das Einspielen von Jar und Dump sowie den Neustart.
Jar und Dump müssen dafür beim Benutzer `hsh03-ansible` liegen.
Die systemd-, SQL- und Log-Kommandos brauchen weiterhin den Backend-Benutzer.

## Nicht-Ziele

- Der `pg_restore`-Abbruch wegen `--clean` ohne `--if-exists` wird im Ansible-Skript von Ops behoben.
- Kein genereller Umbau der Rechtevergabe, etwa auf einen umgebungsunabhängigen Rollennamen.
- Das Ansible-Skript für Einspielen und Neustart liefert Ops nach; es ist nicht Teil dieses Repos.

## Die Lösung

### Changesets umgebungsunabhängig machen

Die drei Tabellen-Changesets bleiben inhaltlich unverändert und erhalten nur `--validCheckSum: ANY`.
Damit akzeptiert die Validierung jede gespeicherte Prüfsumme, ohne etwas erneut auszuführen.

Die Rechtevergabe steht zusätzlich in neuen `runOnChange:true`-Changesets:
`rbac-api-key-GRANTS` und `rbac-subject-sync-report-GRANTS`, jeweils in derselben Changelog-Datei.
Sie ziehen die `grant`-Anweisungen je Umgebung idempotent nach, auch nach dem Einspielen eines fremden Dumps.

Regel für künftige Changesets: `${HSADMINNG_POSTGRES_RESTRICTED_USERNAME}` gehört nie in nicht-wiederholbare Changesets.
Rechtevergaben stehen in eigenen `runOnChange:true`-Changesets, wie in `1055-rbac-views.sql` und `1058-rbac-generators.sql`.

Zur Auslieferung genügt ein neues Jar, denn die Changelogs sind Teil des Jars.
Der Demo-Datensatz muss nicht neu erzeugt werden: die neuen GRANTS-Changesets laufen dort beim ersten Start einfach nach.

### Subject-Delete-Trigger bei jedem Start neu einspielen

Das Changeset `rbac-base-BEFORE-DELETE-SUBJECT-TRIGGER` ist idempotent (`create or replace`, `drop trigger if exists`).
Trotzdem genügt `runOnChange:true` nicht:
bei gespeicherter Prüfsumme `NULL` führt Liquibase 4.31.1 solche Changesets nicht erneut aus,
sondern trägt nur die neue Prüfsumme ein (`ShouldRunChangeSetFilter.checksumChanged`).
Deshalb wird das Changeset auf `runAlways:true` umgestellt: jeder Start stellt die Trigger wieder her.

Der `LiquibaseCompatibilityIntegrationTest` prüft beides:
Nach der Migration des Referenz-Dumps ohne Trigger existieren sie.
Nach einem manuellen `drop trigger` stellt ein erneuter Liquibase-Lauf sie wieder her.

Das Löschen eines API_KEY-Subjects samt Grants ist bereits abgedeckt durch den Szenariotest 9720 (`RevokeApiKeySubject`)
und `RbacSubjectRepositoryIntegrationTest.deleteByUuidPhysicallyRemovesSubjectReferenceAndGrants`.

### SSH-Logins aus dem Host-Namen ableiten

`HSADMINNG_DEPLOY_HOST` enthält nur noch den Host, also `hsh03` oder `hsh11`.
Einen `user@host`-Wert lehnt `tools/remote` mit einem Umstellungshinweis ab.
Daraus leitet das Werkzeug die Logins `<host>-backend@<host>` und `<host>-ansible@<host>` ab.
Die Variablen-Tabelle in `doc/environment-variables.md` beschreibt die neue Bedeutung.

- `remote upload` zielt auf den Ansible-Benutzer; Demo-Dumps aus `tools/demo-data` landen in dessen `~/db-dumps/`.
- `backend deploy --backend` (Vorgabe) lädt zum Backend-Benutzer hoch und startet den systemd-Dienst neu.
- `backend deploy --ansible` legt das Jar in `~/jar/` des Ansible-Benutzers ab und übergibt an das Ansible-Reset;
  dieses initialisiert auch die Datenbank aus `~/db-dumps/` neu.
- Das neue `backend upload` kopiert nur, ohne Neustart: `--ansible` (Vorgabe) nach `~/jar/`, `--backend` nach `~/backend/`.
- `remote ssh` kennt ebenfalls `--backend` (Vorgabe) und `--ansible`;
  `tools/demo-data` prüft `pg_restore`-Version und freien Platz nun mit `--ansible`, denn dort landet der Dump.
- Alle übrigen Verben (systemd-Lebenszyklus, `sql`, `sql-tunnel`, `log`, `login-monitor ...`) nutzen den Backend-Benutzer.

## Offene Fragen

- [ ] TODO.tool: das Ansible-Reset-Kommando in `ansibleReset` (`tools/remote`) nachrüsten, sobald Ops es benennt;
  bis dahin bricht `backend deploy --ansible` nach dem Ablegen des Jars mit einem Hinweis ab.

## Zusätzliche Änderungen

- Beifang: die drei Tabellen-Changesets in `1086-rbac-api-key.sql` und `1087-rbac-subject-sync-report.sql`
  tragen jetzt explizit `runOnChange:false` samt Warnkommentar an den `grant`-Zeilen.
- Beifang: in `doc/PR/IDEAS.md` eine Idee für eine Booking-Item-Types-API notiert,
  damit eine adaptive Oberfläche die Typen nicht fest verdrahten muss.
