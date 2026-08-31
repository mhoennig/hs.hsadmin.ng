# PR#293: Migration auf Gradle 9.7.1

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/293

## Das Problem

Der Build läuft noch auf Gradle 8.5 (`gradle-wrapper.properties` und `wrapper { gradleVersion = "8.5" }`).
Gradle 8.5 ist von Ende 2023 und liegt eine Major hinter dem aktuellen Stand.
Die Gradle-9-Linie bringt relevante Verbesserungen für Java-21-Toolchains, Konfigurations-Cache und Build-Performance, enthält Sicherheits- und Bugfixes und entfernt in 8.x als deprecated markierte APIs.
Auf 8.5 zu bleiben erhöht den künftigen Migrationsaufwand und blockiert Plugin-Updates, die bereits Gradle 9 voraussetzen.
Die Wrapper-Distribution und die Plugin-Versionen im Build müssen daher auf Gradle 9.7.1 angehoben und alle dabei auftretenden Deprecations und Inkompatibilitäten bereinigt werden.

## Nicht-Ziele

- Keine fachlichen Änderungen an REST-API, Persistenz, RBAC oder Domain-Logik.
- Keine Anhebung der Java-Sprachversion oder der Toolchain über Java 21 hinaus.
- Keine fachfremden Library-Upgrades über die für Gradle-9-Kompatibilität zwingenden hinaus.
- Plugin-Upgrades auf neuere, weitestgehend kompatible Versionen sind dagegen ausdrücklich Teil dieses PRs, ebenso alle Upgrades, die Gradle 9.7.1 erfordert.
- Kein Umbau der Build-Phasen oder Task-Graphen, außer er ist für Gradle 9 erforderlich.
- Keine Änderung am Build-Verhalten für Entwickler über die Gradle-Version hinaus.

## Die Lösung

- Die Wrapper-Distribution in `gradle/wrapper/gradle-wrapper.properties` auf `gradle-9.7.1-bin.zip` angehoben.
- Den `wrapper`-Task in `build.gradle.kts` auf `gradleVersion = "9.7.1"` aktualisiert.
- Das Gradle-Wrapper-Jar und -Skript (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`) per `wrapper` regeneriert.
- Auf Gradle-9-Inkompatibilitäten geprüft und behoben:
  `settings.gradle` (Groovy-DSL `url '...'` → `url = uri("...")`), `buildSrc`-Plugins (`hsadmin.quality`, `hsadmin.test-tasks`, `HsadminTestTasksPlugin`, `hsadmin.documentation`: `Project.exec`/`delete` → `ProcessBuilder`/`deleteRecursively`), `build.gradle.kts` (`val by extra` → `extra.set`, `Wrapper.DistributionType`-API) und Drittanbieter-Plugins.
- Plugin-Versionen auf neuere, weitestgehend kompatible Stände angehoben:
  `org.gradle.toolchains.foojay-resolver-convention` 0.7.0 → 1.0.0,
  `com.diffplug.spotless` 7.0.2 → 8.10.0,
  `org.owasp:dependency-check-gradle` 12.1.1 → 12.1.3,
  `com.gorylenko.gradle-git-properties` 2.5.0 → 2.5.7 (Fix für `generateGitProperties` unter Gradle 9),
  `io.openapiprocessor.openapi-processor` 2023.2 → 2026.2 und `openapi-processor-spring` 2022.5 → 2026.2,
  `info.solidsoft.pitest` 1.15.0 → 1.19.0 (Fix für `reporting.baseDir` unter Gradle 9),
  `com.github.ben-manes.versions` 0.52.0 → 0.53.0.
- Library-Versionen nur angehoben, wo sie für Gradle-9- bzw. Plugin-Kompatibilität zwingend sind.
- Verbleibende Deprecation-Warnungen mit `--warning-mode all` ausgewertet und behoben, damit der Build unter Gradle 9 warnungsfrei läuft (z. B. `extra`-Delegate, `Task.project` in `openapi-processor` durch Plugin-Update behoben).
- Verifiziert durch lokalen Build und CI: `./gradlew openApiGenerate`, `compileJava`, `spotlessCheck`, `pmdMain`/`pmdTest`, `unitTest` (1345 Tests grün), `jacocoTestReport`/`jacocoTestCoverageVerification` grün; kein neues Verhalten, nur Tooling-Stand aktualisiert.

## Zusätzliche Änderungen

- `HsOfficeMembershipControllerRestTest.byUnavailableMemberNumber` von `M-0000000` auf `M-1000000` korrigiert:
  Der aktualisierte `openapi-processor` generiert nun korrekte `@Min(1000000)`/`@Max` Validierung für `membershipNumber`, sodass die ungültige Nummer `M-0000000` jetzt `400` statt `404` liefert.
