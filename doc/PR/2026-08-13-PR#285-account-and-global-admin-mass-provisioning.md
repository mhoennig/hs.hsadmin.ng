# PR#285: Massen-Provisionierung von Accounts und Global-Admin-Grants

`tools/create-accounts-from-csv` verknüpft Accounts jetzt mit Subjects,
die der Keycloak-Sync bereits angelegt hat,
und `tools/grant-global-admin-to-subject` vergibt das Grant auf Wunsch an alle USER-Subjects eines Realm-Präfixes.

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/285

## Das Problem

`tools/create-accounts-from-csv` schickte in `POST /api/hs/accounts/accounts` immer das volle `subject`-JSON-Objekt.
Existiert das Subject bereits — der Normalfall, nachdem der Keycloak-Sync gelaufen ist — antwortet der Endpunkt mit `409 Conflict`.
Das Script wertete jeden 409 als „Account existiert bereits“ und übersprang die Zeile,
der Account wurde also nie angelegt und die Lauf-Statistik war falsch.

## Nicht-Ziele

- Keine Änderung an der REST-API:
  Der strikte Kontrakt der `subject`-Variante („Subject darf noch nicht existieren“) bleibt,
  denn in integrierten Installationen ist Keycloak (per Sync) die einzige Quelle für Subjects.
  Ein zunächst erwogener nachsichtiger Endpunkt (existierendes Subject bei identischen Daten übernehmen)
  wurde deshalb wieder verworfen.
- Keine Änderung bestehender Subject-Daten.

## Die Lösung

Das Script fragt pro Zeile `GET /api/rbac/subjects/{subjectUuid}` ab und verzweigt:

- Subject vorhanden:
  Name gegen die CSV-Spalte `subjectName` prüfen (Abweichung wird gemeldet und übersprungen),
  dann den Account nur mit `subject.uuid` anlegen.
  Ein 409 bedeutet auf diesem Pfad eindeutig „hat bereits einen Account“ und wird so gezählt.
- Subject nicht vorhanden (z.B. Installation ohne Keycloak):
  wie bisher das volle `subject`-Objekt schicken.
  Ein 409 kann hier nur heißen, dass der Keycloak-Sync das Subject inzwischen angelegt hat;
  das Script holt es dann erneut, prüft den Namen und verknüpft per `subject.uuid`.

Damit ist die Race-Condition mit einem parallel laufenden Keycloak-Sync im selben Lauf behandelt
und jeder 409 eindeutig interpretierbar, ohne übersetzte Fehlertexte zu parsen.
Verifiziert Ende-zu-Ende gegen einen Mock-Server mit allen fünf Pfaden,
einschließlich simulierter Sync-Race.

Der echte Endpunkt beantwortet ein fehlendes Subject mit 404 ohne Antwortkörper
(anders als der Mock-Server, weshalb der Smoke-Test dies erst auf dem CI-Server aufdeckte),
und ältere curl-Versionen unterdrücken Fehler-Antwortkörper ohnehin.
Das Script liest den HTTP-Status deshalb über die curl-Option `-w` mit aus,
statt ihn aus dem Antwortkörper zu deuten.

## Zusätzliche Änderungen

- `tools/grant-global-admin-to-subject` akzeptiert jetzt auch ein Realm-Präfix mit `-` am Ende
  (z.B. `hostsharing-`) und vergibt das Grant dann an alle USER-Subjects mit diesem Präfix;
  Subject-Namen können nie auf `-` enden, die beiden Formen sind also kollisionsfrei.
- `tools/grant-global-admin-to-subject` nutzt den zuvor per `APIKEY <name>` gesetzten API-Key,
  statt selbst den fest verdrahteten `provisioning`-Key zu laden.
- `tools/http` toleriert einen Schluss-Schrägstrich in `HSADMINNG_API_BASE_URL`;
  bisher entstand daraus `//api/...`, worauf ein vorgeschalteter Apache mit 404 antwortete.
- `tools/api-key-generate` bettet zusätzlich zum Subject-Namen den Erzeugungs-Zeitpunkt ein
  (`hsak_<name>.2026-08-13-09:44:12.<64 Hex>`), damit die Keys wiederholter Rotationen eines Subjects unterscheidbar sind;
  serverseitig zählt weiterhin nur der SHA-256-Hash des ganzen Keys.
- In beiden Provisionierungs-Tools gewinnen explizit exportierte Werte für
  `HSADMINNG_API_BASE_URL` und `HSADMINNG_API_KEY` (z.B. aus `APIKEY <name>`) über die
  persönliche `.environment`, die `source .aliases` sonst still darüberlegt.
