# PR#265: Autorisierungslücken im Booking/Hosting-Bereich aus der zweiten Runde des Backend-Security-Audits

## Verwandte Links (Hostsharing-intern)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/545
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/265

## Aufteilung der zweiten Audit-Runde

Der ursprüngliche Branch der zweiten Audit-Runde wurde zu groß und ist in vier PRs aufgeteilt:

- [PR#260](2026-07-30-PR%23260-security-hardening-second-audit-round.md): Fixes in Office und Accounts.
- [PR#264](2026-08-03-PR%23264-security-hardening-tooling-and-dependencies.md): Fixes im Tooling und Dependencies.
- PR#265 (dieser PR): Fixes im Booking/Hosting-Bereich.
- [PR#288](https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/288): Generator-Härtung, gestapelt auf diesem Branch.

## Das Problem

Das Security-Audit meldete den Befund A-02 im Hosting-Bereich.
Schreibpfade akzeptierten UUIDs vom Client ohne Sichtbarkeitsprüfung.

Drei Probleme:
- Der `alarmContact`-Schreibpfad akzeptierte einen unsichtbaren fremden Contact.
- `POST /api/hs/hosting/assets` erlaubte Top-Level-Assets auf fremden Booking-Items.
- Die Referenz-Lookups verrieten über `400`/`403`, ob eine UUID existiert.

## Nicht-Ziele

- Die Rollen-Graphen ändern sich nicht.
- Keine neue Autorisierungsschicht; alle Fixes nutzen bestehende `_rv`-Restricted-Views oder generierte Checks.

## Hintergrund

[PR#254](2026-07-28-PR%23254-security-hardening-authorization-gaps.md) erklärt die `_rv`-Restricted-Views.
Dieses PR folgt demselben Muster: Code-Pfade erreichen *raw tables* direkt und haben keine Sichtbarkeitsprüfung.

## Die Lösung

### A-02 — `alarmContact`-Schreibpfad ohne Sichtbarkeitsprüfung

`em.getReference` auf `HsOfficeContactRealEntity` prüft keine Sichtbarkeit.
Ein Asset-`ADMIN` konnte so einen fremden Contact anhängen.

**Lösung:** Der Patcher und die `DomainSetupHostingAssetFactory` lösen die Client-UUID jetzt über `findVisibleByUuid` auf.
Unsichtbare Referenzen ergeben `404`.

### A-02 — Top-Level-Assets brauchten keine Booking-Item-Permission

Der `InsertTriggerGenerator` ließ den Insert-Permission-Check aus, wenn ein INSERT-Grant an `GLOBAL:GUEST` ging.
Er ignorierte, dass dieser Grant an einen `switchOnColumn`-Fall gebunden ist.

**Lösung:** Der Generator erzeugt den Check wieder.
Das legte zwei weitere Generator-Defekte offen, beide behoben:
- Zwei Grants an dieselbe Tabelle erzeugten kollidierende Namen; die Grantee-Rolle unterscheidet sie jetzt.
- Der `_z_`-Infix sortiert nicht immer hinter den Rollen-Aufbau-Trigger; wir stellen ein Token voran, wenn nötig.

**Backfill:** Bestehende Asset-Zeilen bekommen ihre INSERT-Permission jetzt per DO-Schleife.
Das neue Opt-in `grantPermissionAlsoForPreExistingRows` begrenzt das auf `hs_hosting.asset`.
Nur `7013-hs-hosting-asset-rbac.sql` wird neu generiert; die anderen RBAC-Changelogs bleiben stabil.

### A-02 — Referenz-Lookups waren ein Existenz-Orakel

Die Lookups für `bookingItemUuid` und `parentAssetUuid` lasen die *raw tables*.
Eine unbekannte UUID ergab `400`, eine existierende, aber unsichtbare `403` —
ein Aufrufer konnte existierende UUIDs so herausfinden.

**Lösung:** Beide Lookups nutzen jetzt `findVisibleByUuid` auf RBAC-Repositories.
Unbekannte und unsichtbare UUIDs ergeben einheitlich `404`.

## Zusätzliche Änderungen

- `assertEventStatus` in `DomainSetupHostingAssetFactoryUnitTest` schlägt jetzt fehl, wenn kein Event persistiert wurde.
- Das deckte zwei leerlaufende Tests auf; der eine prüft jetzt die tatsächliche Abweisung eines überspezifizierten Sub-Assets,
  der andere entfiel.
- Tote Template-Parameter im `InsertTriggerGenerator` entfielen.
- Der `findVisibleByUuid`-Kommentar verweist jetzt auf die kanonische Erklärung.
- Der fünffache `MANAGED_SERVER`-Builder in `HsHostingAssetControllerAcceptanceTest`
  wurde zu `givenSomeTemporaryManagedServer(...)` extrahiert.
- Der falsche Tabellen-Kommentar im emittierten Text bleibt (das released `6303-hs-booking-item-rbac.sql` muss stabil bleiben);
  ein Kommentar im Generator erklärt das.

## Ausgegliederte Generator-Härtung

Vier weitere Befunde betreffen latente Generator-Defekte, die für die Security-Fixes nicht gebraucht werden:
Locale-abhängige Bezeichner, kollidierende Grant-Namen, `isConditional()` bei Ein-Fall-`switchOnColumn` und die private Kopie des Rollen-Trigger-Namens.
Sie sind in Follow-up-PR#288 ausgegliedert, der auf diesem Branch aufsetzt.

## Voraussetzungs-PRs

- keine

## Folge-PRs

- [PR#288](https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/288): Generator-Härtung, gestapelt auf diesem Branch.
