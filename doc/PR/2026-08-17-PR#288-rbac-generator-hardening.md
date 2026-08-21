# PR#288: Härtung des RBAC-Generators gegen latente Defekte

## Verwandte Links (Hostsharing-intern)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/545
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/288
- Basis: [PR#265](2026-08-03-PR%23265-security-hardening-booking-hosting.md) - mittlerweile auf master

## Das Problem

Das Code-Review von PR#265 fand vier latente Defekte des RBAC-Generators,
die für die dortigen Security-Fixes nicht gebraucht werden,
aber künftige Generator-Nutzung still etwas falsch machen würden:

- Bezeichner-Konvertierungen hingen von der System-Locale ab;
  unter tr-Locale wird aus `ADMIN` das `admın` (punktloses ı),
  und die generierten Changelogs weichen von den committeten ab.
- Die Grant-Trigger-/Funktionsnamen kollidieren,
  wenn dieselbe Super-Tabelle über mehrere Aliase mit derselben Rolle granted wird
  (oder zwei Super-Tabellen denselben Short-Name tragen);
  eine Funktion überschreibt dann still die andere.
- `RbacSpec.isConditional()` behandelte einen Grant als unbedingt,
  sobald er alle deklarierten Fälle abdeckte — auch ohne deklarierten `inOtherCases`-Fall.
  Bei einem Ein-Fall-`switchOnColumn` erzeugte der Rollen-Aufbau-Trigger fall-gebundene Grants
  für Zeilen aller Typen (Über-Grant).
- `InsertTriggerGenerator` hielt den Namen des Rollen-Aufbau-Triggers als private Kopie;
  eine Umbenennung in `RolesGrantsAndPermissionsGenerator` hätte still die Feuer-Reihenfolge
  der Trigger zerstört (Grant-Trigger vor Rollen-Aufbau).

## Nicht-Ziele

- Keine Änderung an generierten SQL-Changelogs (`*.sql`):
  alle bestehenden Specs regenerieren byte-identisch, alle Checksummen bleiben stabil.
  Von den generierten Mermaid-Diagrammen (`*.md`) ändern sich nur die zu 7013, siehe Lösung.
- Keine Änderung am Verhalten der Security-Fixes aus PR#265.

## Die Lösung

- Alle Groß-/Kleinschreibungs-Konvertierungen im Generator-Paket nutzen `Locale.ROOT`
  (auch in `StringWriter`, wo tr-Locale zusätzlich Platzhalter wie `${IDENTIFIER}` zerbrochen hätte).
- Reicht die Grantee-Rolle zur Unterscheidung nicht,
  wird zusätzlich der pro Spec eindeutige Alias-Name an die Grant-Trigger-/Funktionsnamen angehängt.
- Der Fix für fall-gebundene Grants sitzt zentral in `isConditional()`:
  ein Grant gilt nur als unbedingt, wenn die deklarierten Fälle einen `inOtherCases`-Fall enthalten.
  Die lokale Abhilfe `appliesToAllCases` im `InsertTriggerGenerator` entfiel.
  Das 7013-Basisdiagramm verliert dadurch die Kante `GUEST ==> INSERT`,
  die fälschlich für alle Asset-Typen statt nur für `DOMAIN_SETUP` dargestellt war.
- Damit fall-gebundene Grants nicht aus allen Diagrammen verschwinden,
  entstehen Fall-Diagramme jetzt auch für Switches mit nur einem deklarierten Fall;
  ohne `inOtherCases`-Fall bleibt zusätzlich das Basisdiagramm mit den unbedingten Grants erhalten.
  Neu generiert: `7013-hs-hosting-asset-rbac-DOMAIN_SETUP.md` mit der `GUEST ==> INSERT`-Kante.
- Der Name des Rollen-Aufbau-Triggers ist eine gemeinsame Konstante beider Generatoren.

## Test-Nachweis

Jeder Test, der einen Defekt zeigt, wurde *vor* seinem Fix committet;
die Branch-Historie zeigt ihn rot ohne den Fix und grün mit ihm.

- `HsHostingAssetRbacSpecGenerationUnitTest` — generiert die Hosting-Spec unter tr-Locale
  und prüft die Bezeichner.
- `InsertTriggerGeneratorUnitTest` — synthetische Spec mit zwei Aliasen derselben Super-Tabelle;
  dazu ein Wächter-Test, der die Trigger-Feuer-Reihenfolge gegen Umbenennungen absichert
  (für die gemeinsame Konstante ist kein Rot-Zustand möglich, weil die Werte übereinstimmen).
- `RolesGrantsAndPermissionsGeneratorUnitTest` — synthetische Ein-Fall-Spec;
  der fall-gebundene Grant muss im Fall-Block landen statt im unbedingten Teil.
- `RbacViewMermaidFlowchartGeneratorUnitTest` — synthetische Ein-Fall-Spec;
  der fall-gebundene Grant muss im Fall-Diagramm erscheinen und im Basisdiagramm fehlen.
- Die synthetischen Test-Entities tragen bewusst kein `@Entity`,
  damit Hibernates Entity-Scan der Integrationstests sie nicht aufsammelt.

## Zusätzliche Änderungen

- Ein Begründungs-Kommentar am fallgebundenen `GLOBAL:GUEST`-INSERT-Grant in `HsHostingAssetRbacEntity`,
  damit der Grant nicht als freies Einfügerecht für DOMAIN_SETUP-Assets missverstanden wird.

## Voraussetzungs-PRs

- [PR#265](2026-08-03-PR%23265-security-hardening-booking-hosting.md) - bereits auf master:
  dieser Branch setzt darauf auf; nach dessen Merge wird die PR-Basis auf `master` umgestellt.
