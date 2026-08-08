# PR#276: PMD Code-Komplexitätsprüfung mit Schwellwert

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/276

## Das Problem

Der Build misst Formatierung (Spotless), Testabdeckung (JaCoCo) und Abhängigkeiten (OWASP, Lizenzen),
aber nichts misst die Komplexität des Codes selbst.
Methoden dürfen daher beliebig wachsen, ohne dass ein Review-unabhängiges Signal darauf hinweist.

Besonders die kognitive Komplexität ist relevant, weil sie näher an der Lesbarkeit liegt
als die zyklomatische, die etwa eine lange flache `switch`-Kaskade genauso hart bestraft
wie tief verschachtelte Bedingungen.

## Nicht-Ziele

- Keine Refaktorierung der bestehenden Ausreißer, die nur markiert und ausgenommen werden.
- Keine weiteren PMD-Regelkategorien wie `bestpractices` oder `errorprone`; dieser PR führt nur Komplexitätsmaße ein.
- Keine Änderung der CI-Konfiguration über die automatische Einbindung in `check` hinaus.

## Die Lösung

PMD 7.13.0 kommt als Gradle-Kernplugin in `buildSrc/src/main/kotlin/hsadmin.quality.gradle.kts`,
neben Spotless und JaCoCo, und hängt sich damit selbsttätig an `check`.
`ruleSets = emptyList()` schaltet die PMD-Vorgaberegeln ab, sodass ausschließlich der eigene Regelsatz greift.

`pmdTest` läuft gleichrangig zu `pmdMain`, weil die RBAC-SQL-Generatoren und das Szenariotest-Gerüst unter `src/test` liegen.
Beide Quellbäume bekommen aber einen eigenen Regelsatz, `config/pmd/complexity.xml` und `config/pmd/complexity-test.xml`,
weil ein gemeinsamer Regelsatz die Schwellen für `src/main` auf das Niveau des Legacy-Imports anheben würde.

Bei den PMD-Vorgabeschwellen meldet der Bestand 69 Verstöße, 25 in `src/main` und 44 in `src/test`.
Die Schwellen sind daher auf den heute schlechtesten Wert gesetzt,
sodass die Regel nur eine Verschlechterung meldet, statt den Bestand rot zu färben:

| Regel                          | `src/main` | schlechtester Fall                                              | `src/test` | schlechtester Fall                                    |
|--------------------------------|------------|-----------------------------------------------------------------|------------|-------------------------------------------------------|
| CyclomaticComplexity (Methode) | 17         | `HostingAssetEntityValidator.validate` = 16                     | 14         | `EntityManagerWrapperUnitTest.getDefaultValue` = 13   |
| CyclomaticComplexity (Klasse)  | 80         | keine Klasse erreicht die Vorgabe                               | 163        | `ImportHostingAssets` = 162                           |
| NPathComplexity                | 2049       | `HsDomainDnsSetupHostingAssetValidator.toZonefileString` = 2048 | 4097       | `EntityManagerWrapperUnitTest.getDefaultValue` = 4096 |
| TooManyMethods                 | 45         | `RbacSpec` = 45                                                 | 110        | `ImportHostingAssets` = 110                           |

`CognitiveComplexity` ist die Ausnahme und steht in beiden Regelsätzen auf 13 statt auf dem schlechtesten Wert 22 bzw. 16.
Die Verteilung hat zwischen 12 und 15 eine Lücke: oberhalb von 12 liegen im gesamten Code nur drei Methoden.
Die Schwelle 13 kostet deshalb genauso wenige Ausnahmen wie die PMD-Vorgabe 15 und ist doch neun Punkte schärfer
als der schlechteste Wert 22, der jeder neuen Methode ein Wachstum bis 21 erlauben würde.

Die drei Ausreißer tragen ein `@SuppressWarnings("PMD.CognitiveComplexity")` mit einem `TODO.refa` an der Methode:

| Methode                                      | kognitive Komplexität |
|----------------------------------------------|-----------------------|
| `HostingAssetEntityValidator.validate`       | 21                    |
| `RbacGrantsDiagramService.traverseGrantsTo`  | 17                    |
| `MarkdownTableCellRenderer.escapeWithIndent` | 15                    |

Die Marker stehen bewusst nur am Code und nicht zusätzlich im Regelsatz,
damit die Liste nicht an zwei Stellen gepflegt werden muss und veralten kann.

Dass die Schwellen wirklich greifen, ist gegen den Bestand geprüft:
Jede um eins verschärfte Schwelle bricht den Build mit genau dem erwarteten Fund,
und bei `CognitiveComplexity` auf 12 meldet PMD ausschließlich die beiden nicht ausgenommenen Methoden mit Wert 12,
während die ausgenommenen 21 und 17 stumm bleiben.

## Weitere Änderungen

- Drei `@SuppressWarnings("PMD.CognitiveComplexity")` samt `TODO.refa` an den oben genannten Methoden.

## Folge-PRs

- Die drei mit `TODO.refa` markierten Methoden zerlegen und danach die Ausnahmen entfernen.
- Die übrigen Schwellen schrittweise senken, sobald ihr jeweils schlechtester Fall entschärft ist.
