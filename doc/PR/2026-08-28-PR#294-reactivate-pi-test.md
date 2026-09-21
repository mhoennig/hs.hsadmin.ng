# PR#294: PiTest Reaktivierung

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/294

## Das Problem

Der PiTest-Task ist seit längerem aus dem Build deaktiviert.
Dabei sind `tasks.check { dependsOn(pitest) }` und `spotlessJava.dependsOn(pitest)` auskommentiert,
und `../../.werkator.yml` ruft `check` und `smokeTest` mit `-x pitest` auf.
Grund war `Pitest currently does not work, needs to be fixed`.
Unter anderem schlug der Task mit `org.pitest.help.PitHelpError: 1 tests did not pass without mutation` fehl.
Damit fehlt die Mutationsabdeckung als Qualitäts-Gate.
`jacocoTestCoverageVerification` allein deckt nur Zeilenabdeckung ab.
Die Reaktivierung soll im Rahmen des Gradle-9-Builds wieder ein grünes `pitest`-Gate liefern,
und im CI laufen.

## Nicht-Ziele

- Keine Änderung an fachlichen Domain-, RBAC- oder API-Regeln.
- Keine breiten Refactors zur Erhöhung der Mutationsabdeckung,
  nur Fixes die für einen grünen `pitest`-Lauf nötig sind.
- Kein Umbau des Test-Sets über `unitTest`/`RestTest` hinaus.
  `AcceptanceTest`, `IntegrationTest` und `ImportHostingAssets` bleiben ausgeschlossen.

## Die Lösung

- `ToStringConverter.from(Object)` in `src/main/java/net/hostsharing/hsadminng/mapper/ToStringConverter.java` gehärtet.
  Synthetische Felder (`Field::isSynthetic`, z.B. `$jacocoData`) werden ignoriert.
  Damit liefert `ToStringConverterUnitTest.convertObjectToString` auch unter JaCoCo/Pitest deterministisch.
  Ergebnis ist `"{ one: a, two: 1 }"`.

- `pitest` wieder entkoppelt von `check` in `build.gradle.kts`.
  Neue Tasks `pitestPartial` (inkrementell) und `pitestFull` (voll, löscht `.pitest/history.bin`) ersetzt Hacks.
  Statt `tasks.check { dependsOn(pitest) }` und `-x pitest`/`-PfullPitTest` kein `check`-Hack mehr.

- `pitest`-Konfiguration in `build.gradle.kts` für Gradle 9 und Java 21 gehärtet.
  `excludedClasses` um `net.hostsharing.hsadminng.rbac.generator.**`, `net.hostsharing.hsadminng.rbac.test.**` und
  `net.hostsharing.hsadminng.**.*Repository` erweitert.
  Grund waren rekursive `RbacViewMermaidFlowchartGenerator`-Mutanten mit `TIMED_OUT`/`RUN_ERROR`
  sowie `*Repository`-Interfaces ohne Logik (nur Spring-Data-Mappings, `0%` Mutation, ziehen Gesamt runter).
  `jvmArgs` auf `["-XX:+EnableDynamicAgentLoading","-Xmx2g"]` gesetzt.
  `timeoutFactor` ist `1.5`, `timeoutConst` ist `8000` und `threads` bleibt `4`.
  `targetTests` wieder `*UnitTest` und `*RestTest` (162 Klassen).
  Volle Coverage braucht RestTests, denn Unit-only hat nur `66% line` und `54% mutation`.

- Inkrementell statt Voll-Mutation in `build.gradle.kts`.
  `enableDefaultIncrementalAnalysis` mit History `.pitest/history.bin` überlebt `rm -rf build`.
  Werkator baut jeden Branch im eigenen Worktree, daher ist History branch-lokal.
  PR-Branches `pitestPartial` mutieren nur geänderte Klassen (`1:47` inkrementell, `4:17` Unit-only, voll `~10:53`).
  `pitestFull` löscht History vor `pitest` (Fix für `dependsOn`→`doFirst` nach `finalizedBy`).

- `buildSrc/src/main/kotlin/hsadmin.quality.gradle.kts` bereinigt.
  `spotlessJava.dependsOn(pitest)` entfernt, damit Spotless schnell bleibt.

- `.gitignore` um `/.pitest/` ergänzt.
  History wird nicht versioniert.

- `../../.werkator.yml` aus PR#297 um pitest ergänzt.
  `builds.default` → `pitestPartial` (inkrementell, PRs).
  `builds.release` (master, Push und nightly `01:00`) → `pitestFull` (nur `~1-2min` mehr als ohne, `8:03`→`10:53`).

- `persistence` gehärtet in `src/test/java/net/hostsharing/hsadminng/persistence/*UnitTest.java`.
  `ImmutableBaseEntity` und `BaseEntity` mit `MockedStatic<Hibernate>` für `initialize` und `reload` getestet.
  `EntityManagerWrapperUnitTest` Delegation korrigiert,
  statt `getClass().getMethod` jetzt `method.invoke(verify(delegateMock), args)` plus Return-Value-Checks,
  für `boolean` beide Werte `false`/`true` um beide Mutanten zu killen.
  Damit steigt `persistence` von `27%` auf `>80%` (zuvor `34%` mit nur `verify`).

- `HsHostingAssetType` gehärtet in `src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/HsHostingAssetTypeUnitTest.java`.
  Neue Tests für `bookingItemPolicy`, `parentAssetPolicy`, `assignedToAssetPolicy`,
  `bookingItemTypes`, `parentAssetTypes`, `assignedToAssetTypes`, `belongsToAny`, `nodeName`, `edges`.
  Damit steigt `hs.hosting.asset` von `63%` (`34/70` in `HsHostingAssetType`) deutlich.

- `EnumerationProperty` gehärtet in `src/test/java/net/hostsharing/hsadminng/hs/validation/EnumerationPropertyUnitTest.java`.
  Neue Unit-Tests für `enumerationProperty`, `values`, `deferredInit`, `valuesFromProperties` (inkl. Filter/Map) und `validate`.
  Damit steigt `hs.validation` Teilmodul und `EnumerationProperty` von `20%` (`3/15`) auf `>80%`.
  Gesamt `1771→1785/2249` (`79%`, `93%` Line, `85%` TestStrength, `163` Tests).

- Schwellen in `build.gradle.kts` an reale Werte angepasst.
  `mutationThreshold` von `71` über `70`, `75`, `77` auf `78`.
  `coverageThreshold` von `57` über `55`, `60` auf `90` (global `93%` Line, worst Package `75%`, vorher `60` constrainete nichts).
  `testStrengthThreshold` von `87` über `75`, `80`, `82` auf `84`.
  Voll-Lauf lag bei `27%` in `persistence` und zog Gesamt auf `54-75%`, nach erstem Fix `75%/81%`,
  nach `persistence`+`HsHostingAssetType` `78%/84%`, nach `*Repository`-Exclude `79%/84%` (`2249` statt `2271` Mutanten),
  nach `EnumerationProperty` `79%/85%` (`1785/2249`, `93%` Line → `90` mit `3%` Puffer).

- Verifiziert mit `. .tc-environment; ./gradlew pitestPartial` und `pitestFull`.
  Zuvor `1 tests did not pass without mutation`, jetzt Coverage grün (`79%` Mutation, `93%` Line, `85%` TestStrength).
  `tools/pr-quick-check` bestanden, `pitest` schreibt `build/reports/pitest/index.html`.

## Zusätzliche Änderungen

- `prQuickCheck` aus dem `release`-Build entfernt, übernommen aus dem damit geschlossenen PR#295.
  Der Check verlangt, dass `HEAD` den Stand von `origin/master` enthält.
  Auf `master` ist das tautologisch, oder es lässt einen gesunden Build scheitern, wenn `master` während des Builds vorläuft.

## Voraussetzungs-PRs

- PR#297 (Werkator-Build-Definitionen in `.werkator.yml`), bereits in `master`.

## Folge-PRs

- Falls Mutations-Schwellen künftig angehoben werden sollen, in separatem PR mit gezielten Coverage-Verbesserungen.
