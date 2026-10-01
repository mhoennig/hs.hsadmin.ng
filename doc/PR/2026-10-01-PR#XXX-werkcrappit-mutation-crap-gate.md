# PR#XXX: Mutation-CRAP mit werkcrappit, Schwellwert 10

## Relevante Links (Hostsharing-intern)

- GitEA-PR: noch nicht angelegt; die Nummer ersetzt `XXX` in Dateiname und Titel.

## Relevante Links (öffentlich)

- werkcrappit: https://git.javagil.de/mi/werkcrappit, Anleitung `docs/guides/hsadmin-ng.md`

## Das Problem

Der PIT-Report von hsadmin-ng zeigt, welche Mutanten überleben, aber nicht, wo Komplexität und schwache Tests zusammenkommen.
Eine komplexe Methode mit wenigen getöteten Mutanten fällt dort nicht auf.

werkcrappit berechnet je Methode den Mutation-CRAP: `C² × (1 − MS)³ + C`,
mit `C` der JaCoCo-Komplexität und `MS` dem Anteil der getöteten Mutanten.
Ein Probelauf am 2026-10-01 auf master `c8955b2e5` fand sechs Methoden über 10, die höchste mit 12,72.
Alle übrigen 1152 bewerteten Methoden liegen bei höchstens 10.

## Nicht-Ziele

- Kein Schwellwert für alle Zeiten: 10 hält den heutigen Stand fest, er ist kein bewiesener Grenzwert.
- Keine Änderung an PITs eigenen Schwellwerten (`mutationThreshold`, `coverageThreshold`, `testStrengthThreshold`).
- Kein Gate in `check`: `mutationCrapCheck` läuft nur, wo es ausdrücklich aufgerufen wird.
- Keine Tests für die 161 Methoden ganz ohne getöteten Mutanten;
  144 davon haben Komplexität 1 und können höchstens den Wert 2 erreichen.

## Die Lösung

### werkcrappit einbinden

- `settings.gradle`: die Maven-Registry von git.javagil.de für das Gradle-Plugin.
- `build.gradle.kts`: Plugin `de.hoennig.werkcrappit`, dieselbe Registry für `werkcrappit-pit`,
  und `werkcrappit { maxMutationCrap = 10 }`.
- `.werkator.yml`: `mutationCrapCheck` nach `pitestPartial` bzw. `pitestFull`.

`maxMutationCrap = 10` heißt „höchstens 10“: Das Gate scheitert erst bei einem Wert über 10.

### Vorher zu beheben

Mit dem Schwellwert 10 scheitert das Gate an diesen sechs Methoden.
Bei fünf genügt ein weiterer getöteter Mutant;
`toZonefileString` braucht keine Tests, sondern eine Aufteilung,
denn der Mutation-CRAP ist nie kleiner als die Komplexität.

| Jetzt | Klasse und Methode | C | getötet | danach |
| ---: | --- | ---: | ---: | ---: |
| 12,72 | `Context.maskProperties` | 6 | 3/7 | 8,83 |
| 12,41 | `IntegerProperty.deferredInit` | 5 | 2/6 | 8,13 |
| 12,00 | `HsDomainDnsSetupHostingAssetValidator.toZonefileString` | 12 | 12/12 | ≤ 9 |
| 12,00 | `Stringify.<init>` | 3 | 0/2 | 4,13 |
| 12,00 | `HsOfficePartnerController.optionallyCreateExPartnerRelation` | 3 | 0/2 | 4,13 |
| 11,60 | `HsOfficeDebitorController.postNewDebitor` | 7 | 6/11 | 9,36 |

Was jeweils fehlt:

1. `maskProperties`: ein zu maskierendes Feld in einem verschachtelten Objekt und eines in einem Array.
2. `deferredInit`: `min` zusammen mit `minFrom` und `max` zusammen mit `maxFrom` lösen die Exception aus.
3. `toZonefileString`: keine Tests, sondern eine Aufteilung, z. B. je Record-Typ eine Methode.
4. `Stringify.<init>`: ein `Stringify` ohne expliziten Namen, mit und ohne `@DisplayAs`.
5. `optionallyCreateExPartnerRelation`: Eine gewechselte Partner-Person speichert eine `EX_PARTNER`-Relation,
   eine unveränderte nicht.
6. `postNewDebitor`: `debitorRel` und `debitorRelUuid` zugleich oder keines von beiden ergeben 400.

Die beiden Controller-Methoden prüfen vielleicht schon Integrationstests.
Die zählen hier nicht, denn PIT nutzt nur `*UnitTest` und `*RestTest`.

`maskProperties` maskiert Passwörter im geloggten Request-Body.
Heute bemerkt kein Test, wenn die Rekursion in verschachtelte Objekte oder Arrays bricht;
ein Passwort dort gelangte dann im Klartext ins Log.

## Offene Fragen

- Soll die Mutation-CRAP-Prüfung auch im Build für `master` laufen oder nur in den Branch-Builds?

## Voraussetzungs-PRs

- werkcrappit 0.3.1 in der Maven-Registry von git.javagil.de (werkcrappit PR#9 und PR#10).
