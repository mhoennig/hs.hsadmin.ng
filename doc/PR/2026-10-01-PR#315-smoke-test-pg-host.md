# PR#315: `tools/smoke-test` erreicht seine Datenbank auf vm4006 nicht

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/315

## Das Problem

Seit 2026-10-01 schlägt `smokeTest` auf vm4006 fehl: „bootRun terminated prematurely … Connection refused“.

Rootless Docker auf vm4006 nutzt jetzt den Port-Treiber `slirp4netns`.
Veröffentlichte Ports lauschen dann im Netz des Docker-Daemons auf `10.0.2.100`, nicht auf `127.0.0.1`.
Der Build-Container läuft mit `--network host` in genau diesem Netz.
`tools/smoke-test` verbindet sich aber fest mit `127.0.0.1:$PG_PORT`.

Werkator setzt dort bereits `TESTCONTAINERS_HOST_OVERRIDE=10.0.2.100`; damit laufen die Testcontainers-Tests wieder.

## Nicht-Ziele

- Keine Änderung an `-p "127.0.0.1:$PG_PORT:5432"`:
  diese Adresse bezieht sich auf das Netz des Docker-Daemons, in dem er den Port bindet,
  und hält die Wegwerf-Datenbank lokal vom LAN fern.
  Mit slirp4netns bietet der Daemon den Port dann auf `10.0.2.100` an, nicht auf dem `127.0.0.1` seiner Clients.
- Keine Änderung an den Prüfungen in `portInUse`:
  sie prüfen vor dem Start, ob der Port lokal frei ist, und dafür ist `127.0.0.1` lokal richtig.
- Keine Änderung am lokalen Verhalten: ohne beide Variablen bleibt es bei `127.0.0.1`.

## Die Lösung

`tools/smoke-test` nimmt die Adresse der Datenbank aus `HSADMINNG_SMOKE_TEST_PG_HOST`,
sonst aus `TESTCONTAINERS_HOST_OVERRIDE`, sonst `127.0.0.1`:

```bash
PG_HOST=${HSADMINNG_SMOKE_TEST_PG_HOST:-${TESTCONTAINERS_HOST_OVERRIDE:-127.0.0.1}}
export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://$PG_HOST:$PG_PORT/postgres
```

`HSADMINNG_SMOKE_TEST_PG_HOST` ist im Kopfkommentar bei den übrigen Variablen dokumentiert.

Prüfung: Der Branch-Build auf vm4006 ist grün, und `smokeTest` läuft durch.

## Zusätzliche Änderungen

- `doc/environment-variables.md` dokumentiert nun alle `HSADMINNG_SMOKE_TEST_*`-Variablen und `TESTCONTAINERS_HOST_OVERRIDE`;
  die drei bisherigen Variablen des Smoke-Tests fehlten dort.
