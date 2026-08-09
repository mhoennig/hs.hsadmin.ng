# PR#278: Werkzeuge: `demo-data` für Demo-Datensätze

`tools/demo-data` bündelt das Erzeugen, Sichern, Wiederherstellen, Prüfen und Übertragen
der generierten Demo-Datensätze in ein Werkzeug mit einem Verb je Schritt,
statt von Hand anzupassender Beispiele in der Dokumentation.

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/278
- Hervorgegangen aus PR#277: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/277 (verworfen)

## Das Problem

Alle vier Schritte des Erstellens von Demo-Daten stehen in `doc/demo-datasets.md` als Kommandoblöcke,
die man für jeden Datensatz von Hand anpasst und beim Wechsel neu zusammen kopiert.

Das ist nicht nur mühsam, sondern verleitet auch zu folgenden potenziellen Problemen:

### Kommandoblöcke, die niemand prüft

Ein Dokument veraltet leicht, und von Hand angepasste Schnipsel müssen korrekt zusammenpassen,
etwa bei UUIDs und Identifiern.
Ebenso leicht könnte man die Ausschlüsse beim PostgreSQL-Dump vergessen: 
ohne sie liegt der API-Schlüssel aus `ScenarioTest` im Dump,
statt dass die Stage beim Start einen neuen Provisioning-Key erzeugt.

### Bauen und Starten brauchen verschiedene Umgebungen

`bootRun` baut die Anwendung stets neu, obwohl oft ein fertiges Jar vorliegt,
und ein einzelner Gradle-Aufruf kann Bauen und Starten auch nicht trennen:
das Bauen braucht die Testcontainers-Datenbank, der Lauf die Datensatz-Datenbank.
Die Trennung wiegt schwerer als die Bauzeit:
mit gesetzter `HSADMINNG_POSTGRES_JDBC_URL` schriebe `scenarioTest` die Szenarien in den Demo-Datensatz,
und ohne das Profil `without-test-data` schriebe der Start Test-Partner und Test-Subjects dauerhaft hinein;
beides beschädigt den Datensatz und fällt erst viel später auf.

## Nicht-Ziele

- Keine Änderungen an der Java-Seite der API-Schlüssel (`net.hostsharing.hsadminng.config.ApiKey`),
  am Schlüsselformat, an der serverseitigen Speicherung
  oder an der clientseitigen Schlüsselverwaltung (`APIKEY`, `.apikeys.gpg`).
- Kein allgemeines Werkzeug für beliebige Datenbanken.
  `demo-data` setzt viele Parameter wie den Datenbank-Admin, HSAdmin NG Admin und Profile der Demo-Umgebung fest ein.
- Keine Übernahme der Aufgaben von `tools/remote`, das die Jar auf den Testserver ausbringt.
- In `tools/remote` keine Spezifika für `tools/demo-data` einbauen.
- Kein direktes Einspielen eines Dumps auf dem Ziel-PostgreSQL.
  `demo-data upload` legt die Datei nur ab; das Ersetzen der Backend-Datenbank bleibt ein bewusster Handgriff.

## Die Lösung

`tools/demo-data` fasst die Schritte in Verben:
`generate`, `dump`, `restore`, `run`, `verify`, `list` und `drop`.
Einzige Eingabe ist der Datensatz; Datenbank, Personen-CSV, Dump-Name und Wiederherstellungsziel leiten sich daraus ab,
müssen also nicht mehr von Hand zusammenpassen.
Allgemeine Optionen stehen vor dem Verb, Werte dahinter.
`--pg-version=16` bzw. `--pg-version=15` (Bookworm) erzeugt einen eigenen Container für Ziele ohne PostgreSQL 17.

Auf den Testserver kommt ein Dump mit `demo-data upload <dataset>`.
Das Verb liegt bei `demo-data`, weil dort das Wissen über Dumps liegt: Namensschema, Version, Auswahl des jüngsten.
`tools/remote` bekommt mit `remote upload <datei> <verzeichnis>` nur einen allgemeinen Datei-Transport
und kennt weiterhin ausschließlich den Host samt `HSADMINNG_DEPLOY_HOST`, `ssh` und `scp`;
damit hängt das allgemeinere Werkzeug nicht am spezielleren, sondern umgekehrt.
Nebenbei fordert `remote ssh` ein Terminal nur noch an, wenn es eines gibt, sonst rauschte `ssh` in jede Skript-Ausgabe.
`remote backend setup` installiert (oder erneuert) die systemd-User-Unit des Backends nach dem hsh11-Muster,
damit eine neue Stage reproduzierbar entsteht, statt aus handgepflegten Unit-Dateien;
die Stage-Secrets bleiben in `~/backend/.environment` auf dem Server.
`HSADMINNG_DEPLOY_HOST` hat bewusst keinen Vorgabewert mehr:
ohne explizit gesetztes Ziel verweigert `remote` jede Server-Aktion,
damit kein Kommando versehentlich eine ungewollte Stage trifft.

Die PostgreSQL-Version fragt `demo-data` über `remote ssh` beim Host ab und wählt den Dump danach aus,
denn ein mit einer neueren Version geschriebener Dump ließe sich dort nicht zurückspielen;
ein widersprechendes `--pg-version=` bricht ab, statt still einen anderen Dump zu übertragen.
Der Server liefert derzeit `pg_restore 15.18` und begründet damit `--pg-version=15`.
Vor der Übertragung wird der freie Platz gegen die Dump-Größe geprüft,
und die Datei bekommt ihren Namen erst nach vollständigem Transfer, damit kein halber Dump liegenbleibt.

`--dry-run` druckt die Kommandos, statt sie auszuführen, und ersetzt damit die Kommandoblöcke der Dokumentation:
die Ausgabe stammt aus demselben Code, ist also nie veraltet, und sie ist in Sekunden prüfbar.
`verify` leistet, was ein Dokument gar nicht kann — Dump wiederherstellen, Anwendung starten, anmelden, abfragen, Exit-Code.

Der Dokumentations-Bau läuft in einer Subshell mit `.tc-environment` und ohne die Datensatz-Variablen,
denn nur er braucht über `scenarioTest` eine Testcontainers-Datenbank und dürfte den Datensatz nicht sehen.
Umgekehrt sourced jeder Lauf `.unset-environment`, damit die aufrufende Shell den Start nicht beeinflusst.
Die Profile sind auf `fake-jwt,without-test-data` festgelegt und nicht wählbar,
denn der Schaden aus einem fehlenden `without-test-data` ist nicht rückgängig zu machen und im Betrieb nicht zu bemerken.
Gefragt wird nur, wo wirklich etwas verlorengeht, also wenn die Zieldatenbank existiert und Daten enthält;
ohne Terminal bricht der Lauf dort ab, und `--yes` beantwortet die Frage für Aufrufe aus Skripten.
Der Datensatz-Name muss eine Kundenzahl oder ein einfacher Kleinbuchstaben-Bezeichner sein,
denn er steht unmaskiert in SQL-Kommandos wie `drop database`.

## Offene Punkte

- [x] Die Kommandoblöcke in `doc/demo-datasets.md` sind durch die Verben ersetzt;
      das Dokument behält die Begründungen, die der Hilfetext nicht trägt.
- [ ] TODO.tool: `tools/remote` für sichere Argumentbehandlung und gezielte Tests nach Python 3 migrieren.
- [ ] TODO.tool: Die Umgebungs-Handhabung des Backend-Toolings an die des Frontends (eigenes Repository) angleichen,
      das in dieser Hinsicht moderner ist: je Umgebung eine Datei als einzige Quelle,
      das Deploy-Ziel in derselben Datei, keine still wirkenden exportierten Shell-Variablen.
      Zu klären: das Zusammenspiel mit `.apikeys.gpg`, damit Secrets nicht im Klartext in Umgebungs-Dateien wandern.
- [ ] TODO.tool: Einen Datensatz auch ohne Kopie auf dem Server einspielen,
      also lokales `pg_restore` durch den Tunnel aus `remote backend sql-tunnel`;
      das spart Plattenplatz und `pg_restore` auf dem Server, überträgt den Dump aber bei jedem Einspielen erneut.
      Die Wahl hängt an Dump-Größe und Leitung und fällt erst mit den erzeugten Datensätzen.
- [x] `remote backend deploy --with-documentation` baut das Jar mit gerenderter Dokumentation
      statt der Platzhalterseite unter `/doc/`
      (`scenarioTest` und `bootJar` in einer Subshell mit `.tc-environment`, wie bei `demo-data`)
      und meldet fehlende Render-Werkzeuge wie `pandoc` schon vor dem Bau.

## Zusätzliche Änderungen

Arbeit an diesem PR sind einig weitere Problem aufgefallen, die mit behoben wurden:

- API-Schlüssel-Werkzeuge zu `api-key-*` vereinheitlicht:
  `api-key-generate <subject-name>` verlangt den subject-name, statt still die namenlose Altform zu erzeugen;
  das neue `api-key-hash` ersetzt die mehrfach kopierte `printf | sha256sum | cut`-Zeile,
  deren `echo`-Falle Schlüssel erzeugte, die stillschweigend nie authentifizieren.
- `tools/howto` überspringt nicht als UTF-8 lesbare Dateien wie `.apikeys.gpg` jetzt still,
  statt sie als Lesefehler zu melden; echte Lesefehler gehen weiter nach stderr.

## Voraussetzungs-PRs

- PR#270 (generierte Demo- und Testdaten), inzwischen in master gemergt:
  er bringt die Datensätze, den von `demo-data generate` aufgerufenen Generator und `doc/demo-datasets.md`.

## Folge-PRs

- Aus den offenen `TODO.tool`-Punkte sollten Folge-RPs entstehen.
