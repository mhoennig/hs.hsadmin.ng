# PR#317: Mutation-CRAP mit werkcrappit, Schwellwert 10

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/317

## Das Problem

Der PIT-Report von hsadmin-ng zeigt, welche Mutanten überleben, aber nicht, wo Komplexität und schwache Tests zusammenkommen.
Eine komplexe Methode mit wenigen getöteten Mutanten fällt dort nicht auf.

CRAP (Change Risk Anti-Patterns) ist eine Metric zur Bewertung des Änderungsrisikos von Code
anhand von Komplexität und Testabdeckung.

Ein Mutation-CRAP geht einen Schritt weiter und berechnet je Methode den Mutation-CRAP: `C² × (1 − MS)³ + C`,
mit `C` der Komplexität und `MS` dem Anteil der getöteten Mutanten.
Ein solcher Mutation-CRAP soll nun als Gate im build verwendet werden.

## Nicht-Ziele

- Kein Schwellwert für alle Zeiten: 10 hält den heutigen Stand fest, er ist kein bewiesener Grenzwert.
- Keine Änderung an PITs eigenen Schwellwerten (`mutationThreshold`, `coverageThreshold`, `testStrengthThreshold`).
- Kein Gate in `check`: `mutationCrapCheck` läuft nur, wo es ausdrücklich aufgerufen wird; wie auch piTest selbst.

## Die Lösung

Werkcrappit ist solch ein Mutation-CRAP-Plugin für piTest und Gradle:
https://git.javagil.de/mi/werkcrappit, Anleitung für hsadmin-ng in `docs/guides/hsadmin-ng.md`.
Ein Probelauf am 2026-10-01 auf master `c8955b2e5` fand sechs Methoden mit einem Mutation-CRAP-Wert über 10;
der höchste Wert war 12,72.
Alle übrigen 1152 bewerteten Methoden haben einen Mutation-CRAP-Wert von höchstens 10.

- `settings.gradle`: die Maven-Registry von git.javagil.de für das Gradle-Plugin.
- `build.gradle.kts`: Plugin `de.javagil.werkcrappit` 0.8.0, dieselbe Registry für `werkcrappit-pit`,
  und `werkcrappit { maxMutationCrap = 10 }`.
- `.werkator.yml`: `mutationCrapCheck` nach `pitestPartial` in den Branch-Builds
  und nach `pitestFull` im Build für `master`.
- `.werkator.yml`: `mutationCrapCodeCharta` in den Branch-Builds; es schreibt `build/reports/codecharta/mutation-crap.cc.json` mit einer `index.html`,
  eine Code-City für [CodeCharta](https://codecharta.com/): Fläche `rloc`, Höhe `complexity`, Farbe `mutation_crap`.
  Ob der Export auch oder nur im Build für `master` läuft, entscheiden wir nach seiner Laufzeit.

`maxMutationCrap = 10` heißt „höchstens 10“: Das Gate scheitert erst bei einem Wert über 10.
Der Schwellwert steht von Anfang an im Branch;
die sechs Methoden darüber behebt dieser PR, siehe „Zusätzliche Änderungen“.

## Zusätzliche Änderungen

Mit dem Schwellwert 10 scheiterte das Gate an den ersten sechs dieser Methoden.
Die siebte lag mit 10,00 genau auf dem Schwellwert und hätte bei der nächsten Verzweigung gestoppt.
Bei fünf genügte ein weiterer getöteter Mutant;
`toZonefileString` brauchte keine Tests, sondern eine Aufteilung,
denn der Mutation-CRAP ist nie kleiner als die Komplexität.

| Jetzt | Klasse und Methode                                            |  C | getötet | danach |
|------:|---------------------------------------------------------------|---:|--------:|-------:|
| 12,72 | `Context.maskProperties`                                      |  6 |     3/7 |   6,00 |
| 12,41 | `IntegerProperty.deferredInit`                                |  5 |     2/6 |   5,00 |
| 12,00 | `HsDomainDnsSetupHostingAssetValidator.toZonefileString`      | 12 |   12/12 |   1,00 |
| 12,00 | `Stringify.<init>`                                            |  3 |     0/2 |   3,00 |
| 12,00 | `HsOfficePartnerController.optionallyCreateExPartnerRelation` |  3 |     0/2 |   3,00 |
| 11,60 | `HsOfficeDebitorController.postNewDebitor`                    |  7 |    6/11 |   7,04 |
| 10,00 | `HsOfficeBankDirectoryImporter.fields`                        | 10 |    alle |   7,00 |

Was jeweils fehlte:

1. `maskProperties`: ein zu maskierendes Feld in einem verschachtelten Objekt und eines in einem Array.
2. `deferredInit`: `min` zusammen mit `minFrom` und `max` zusammen mit `maxFrom` lösen die Exception aus.
3. `toZonefileString`: keine Tests, sondern eine Aufteilung;
   die Hilfsmethode `autoRR` wählt je Record-Typ zwischen Records und Kommentar.
4. `Stringify.<init>`: ein `Stringify` ohne expliziten Namen, mit und ohne `@DisplayAs`,
   im Testrumpf gebaut, denn PIT führt Static-Initializer nicht je Mutant neu aus.
5. `optionallyCreateExPartnerRelation`: Eine gewechselte Partner-Person speichert eine `EX_PARTNER`-Relation,
   eine unveränderte nicht.
6. `postNewDebitor`: `debitorRel` und `debitorRelUuid` zugleich, keines von beiden
   oder `debitorRel.mark` ergeben je eine eigene 400-Meldung.
7. `fields`: keine Tests, sondern eine Aufteilung;
   die Hilfsmethode `isEscapedQuote` erkennt das verdoppelte Anführungszeichen im quotierten Feld (C 5, 5,00).

Die beiden Controller-Methoden prüften vielleicht auch schon Integrationstests.
Die zählen hier nicht, denn PIT nutzt nur `*UnitTest` und `*RestTest`.

`maskProperties` maskiert Passwörter im Request-Body im Log.
Zuvor bemerkte kein Test, wenn die Rekursion in verschachtelte Objekte oder Arrays branch;
ein Passwort dort gelangte dann im Klartext ins Log.

- [ ] TODO.impl: `toZonefileString` liest für `auto-AAAA-RR` den Schlüssel `auto-AAA-RR`
  und für `auto-WILDCARD-MX-RR` den Schlüssel `auto-SPF-RR`;
  die Aufteilung ändert dieses Verhalten bewusst nicht.
