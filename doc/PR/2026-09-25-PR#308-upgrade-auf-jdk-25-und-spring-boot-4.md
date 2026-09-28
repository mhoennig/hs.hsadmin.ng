# PR#308: Upgrade auf JDK 25 und Spring Boot 4

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/308

## Das Problem

Der Build läuft aktuell auf der Java-Toolchain 21 und auf Spring Boot 3.5.16.

Spring Boot 3.5 ist die letzte 3.x-Minor und seit dem 2026-06-30 am Ende der offenen Wartung;
3.5.16 war der letzte frei verfügbare Patch.
Damit kommen keine Sicherheits-Fixes mehr an, und die im Build gepflegten CVE-Anhebungen
(`jackson-bom`, `logback`, `log4j2`, `postgresql`) müssten dauerhaft von Hand nachgezogen werden.

Java 21 ist weiterhin gewartet, aber Java 25 ist das aktuelle LTS,
und Spring Boot 4 setzt ohnehin eine neuere Toolchain voraus.
Beide Upgrades zusammen zu machen erspart einen zweiten Durchlauf durch dieselben Build-, Test- und Deploy-Stellen.

## Nicht-Ziele

- Keine fachlichen Änderungen an REST-API, Persistenz, RBAC oder Domain-Logik.
  Zwei Fehlerantworten ändern dennoch ihren Wortlaut, weil die neuen Bibliotheken sie so formulieren:
  die 413-Antwort nennt „Content Too Large“ nach RFC 9110, und eine unauflösbare Referenz
  meldet den Hibernate-7-Wortlaut; die Statuscodes bleiben 413 und 400.
- Kein Umstieg auf Java-25-Sprachfeatures im bestehenden Code; nur die Toolchain steigt.
- Kein Ersatz von ModelMapper, obwohl dessen `sun.misc.Unsafe`-Nutzung mittelfristig bricht (siehe Folge-PRs).
- Keine Library-Upgrades ohne Bezug zur JDK-/Spring-Boot-Migration oder zu gemeldeten Sicherheitslücken.
- Keine Änderung an der Server-Installation selbst; `tools/remote` prüft nur die neue JDK-Version und meldet sie.

pr-quick-check:max-scope-score=130: Framework-Upgrade und Entfernung des unbenutzten Windows-Wrappers.

pr-quick-check:max-pr-doc-lines=240: Das Upgrade berührt Build, Konfiguration, Fehlerbehandlung und Testaufbau,
und der Code-Review hat zu jedem dieser Bereiche Entscheidungen beigetragen, die hier begründet sind.
Die umbenannten Test-Slice-Pakete und Jackson-3-APIs betreffen Anwendung, REST-Tests, Integrationstests und Testwerkzeuge.
Die zusammengehörenden API-Umstellungen müssen gemeinsam kompilieren; hinzu kommen die beschriebenen Sicherheitsupdates.

## Die Lösung

Vorab: Versionsupgrades führen durch Ripple-Effekte leider leicht vom Hundertsten ins Tausendste.
Neue JDK- und Framework-Versionen ziehen Anpassungen an Plugins, Bibliotheken, Konfiguration und Tests nach sich.
Behobene Build-Fehler machen dabei oft erst die nächsten Inkompatibilitäten oder Deprecations sichtbar.
Diese Folgeanpassungen erklären den Umfang des PR und die Änderungen außerhalb der eigentlichen Versionsangaben.

### Schritt 1: JDK 25

- `JAVA_VERSION` in `build.gradle.kts` von 21 auf 25 angehoben; Vendor und Toolchain-Mechanik bleiben unverändert.
- JaCoCo von 0.8.10 auf 0.8.15 angehoben:
  0.8.10 bricht mit „Error while creating report“ ab, weil es Class-File-Version 69 nicht instrumentieren kann.
- `--enable-native-access=ALL-UNNAMED` für alle `Test`-Tasks und für `bootRun` gesetzt:
  `LinuxEtcShadowHashGenerator` lädt `libcrypt` über JNA,
  was die JDK seit 24 als restricted method meldet und später blockieren wird.
  In `bootRun` ersetzt dafür ein additives `jvmArgs(...)` die frühere Zuweisung, damit die Debug-Option erhalten bleibt.
- `tools/remote` erwartet die JDK nun unter `~/opt/jdk-25`, inklusive Installationshinweis, `JAVA_HOME`
  und `ExecStart` der systemd-Unit; `ExecStart` bekommt dieselbe `--enable-native-access`-Option wie der lokale Lauf.
- Verifiziert unter Temurin 25 mit `jacocoTestCoverageVerification`, `migrationTest`, `spotlessJavaCheck`,
  `pmdMain`/`pmdTest` sowie den Integrationstests der Module general, office, booking und hosting.

### Schritt 2: Spring Boot 4

Auf 4.1.1 angehoben, den aktuellen Stand der 4er-Linie.
Die bisherigen CVE-Anhebungen sind durch die Boot-BOM und gezielte Overrides für neuere Sicherheitskorrekturen ersetzt.

Spring Boot 4 zerlegt seine Sammel-Starter in kleinere Module, daher:
`spring-boot-starter-web` → `-webmvc`,
`spring-boot-starter-oauth2-resource-server` → `spring-boot-starter-security-oauth2-resource-server`,
`org.liquibase:liquibase-core` → `spring-boot-starter-liquibase`,
und die Test-Slices als eigene Starter (`-webmvc-test`, `-web-server-test`, `-data-jpa-test`, `-security-test`, `-actuator-test`).
Deren Pakete heißen ebenfalls neu, zum Beispiel `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.

Mitgezogene Bibliotheken:
springdoc 2.8.17 → 3.1.1,
hypersistence-utils-hibernate-63 3.10.3 → -hibernate-73 3.16.0:
die `-73`-Variante passt zum verwalteten Hibernate 7.4 und ist gegen Jackson 3 gebaut,
sodass auch die JSONB-Spalten nicht mehr über Jackson 2 serialisiert werden,
Testcontainers 1.21.4 → 2.0.5 (die Artefakte heißen jetzt `testcontainers-junit-jupiter` und `testcontainers-postgresql`),
ArchUnit 1.4.1 → 1.5.1,
rest-assured mit eigener Version 6.0.1, weil Spring Boot sie nicht mehr verwaltet.

Mehrere Punkte brauchten mehr als eine Versionsnummer:

- Gradle legt aus seiner Distribution (`lib/junit-bom.properties`) `org.junit:junit-bom` 5.12.2 fest
  und hält damit `junit-platform-launcher` auf 1.14.4, während Spring Boot 4 JUnit 6 mitbringt.
  Die Mischung bricht jeden Test-Task mit `NoSuchMethodError`, und ein `strictly` in den Dependencies wird überstimmt.
  `testing.suites.named<JvmTestSuite>("test") { useJUnitJupiter(junitVersion) }` setzt die Vorgabe dagegen sauber außer Kraft.
- Spring Framework 7 konvertiert HTTP-JSON mit Jackson 3; der bisherige Mapper und sein `JsonNullableModule` verwenden Jackson 2.
  Der Anwendungsmapper verwendet deshalb `tools.jackson` und das `JsonNullableJackson3Module` aus Version 0.2.11.
  Die generierten API-Resources behalten ihre gemeinsamen Annotationen unter `com.fasterxml.jackson.annotation`.
  `spring-boot-starter-jackson` ersetzt `spring-boot-jackson2`; die Mapper-Auswahl in `application.yml` entfällt.
  Java-Time-Unterstützung ist in Jackson 3 eingebaut und braucht kein zusätzliches Modul.
  JSON-Fehlerbehandlung, Audit-Maskierung und Testwerkzeuge verwenden ebenfalls Jackson 3.
  Swagger Core benötigt weiterhin transitive Jackson-2-Bibliotheken; deren Sicherheits-Pin bleibt erhalten.
  Bei unbekannten JSON-Feldern lautet die Jackson-Fehlermeldung nun „Unrecognized property“ statt „Unrecognized field“.
- `EntityManagerWrapper` implementiert `EntityManager` vollständig und braucht daher die in Jakarta Persistence 3.2
  hinzugekommenen Methoden, unter anderem `callWithConnection`, `runWithConnection` und die Varianten mit `FindOption`.
- `TestRestTemplate` kommt nicht mehr von selbst; die drei betroffenen Integrationstests tragen jetzt
  `@AutoConfigureTestRestTemplate`, und deren Autokonfiguration braucht `spring-boot-starter-restclient-test`.
- Hibernate 7 meldet eine unauflösbare Referenz als `org.hibernate.ObjectNotFoundException`
  statt als `jakarta.persistence.EntityNotFoundException`, verpackt in einer `JpaSystemException`.
  Damit fiel der Fehler in `RestResponseEntityExceptionHandler` auf 500 statt 400.
  `httpStatus(...)` kennt nun beide Typen, und `handleJpaExceptions` übergibt dorthin die Ursache statt der Hülle,
  wie es die übrigen Behandlungen schon taten.
  Die beiden betroffenen Acceptance-Tests erwarten entsprechend den neuen Wortlaut der Meldung.
- Liquibase 5.0.3 steht unter `FSL-1.1-ALv2` statt Apache-2.0, weshalb `checkLicense` den Build abbrach.
  In `etc/allowed-licenses.json` ist die Lizenz nur für `org.liquibase:*` erlaubt, nicht pauschal:
  Die Konkurrenzklausel der FSL ist für Liquibase unkritisch, weil wir es als Bibliothek im Eigenbetrieb nutzen
  und die Lizenz zwei Jahre nach jedem Release zu Apache-2.0 wird,
  doch für eine künftige Bibliothek muss dieselbe Frage neu beantwortet werden.

## Zusätzliche Änderungen

- Veraltete Spring-Nullability-Annotationen sind durch JSpecify ersetzt;
  Reflections, AssertJ, Commons Lang und Locale verwenden ihre aktuellen APIs.
- Der Jackson-3-Mapper verwendet direkt `JsonMapper.builder()` statt des veralteten Spring-Builders für Jackson 2.
  Tests sichern Kommentare, Dezimalpräzision, unbekannte Felder, PATCH-Nullwerte, ISO-Datumswerte und JSON-Views ab.
  Nach der Jackson-3-Migration sind Unit-/REST-Tests, Coverage und Migrationstests lokal erfolgreich.
  Der JSON-Fehlertest prüft HTTP-Status und Parse-Fehler statt Jacksons versionsabhängiger Positionsangabe.
- HTTP 413 verwendet `CONTENT_TOO_LARGE`; damit heißt auch `statusPhrase` jetzt „Content Too Large“.
- Die globale `GUEST`-Rolle bleibt als DSL-Marker für INSERT ohne Rollenbeschränkung erhalten.
  `GUEST` trägt deshalb den Hinweis „to be avoided“ statt `@Deprecated`; Warnungsunterdrückungen sind nicht nötig.
- Das Versions-Plugin verwendet seine aktuelle ID `io.github.ben-manes.versions`.
  Das Lizenzreport-Plugin steigt von 2.9 auf 3.1.4, um veraltete Gradle-API-Aufrufe zu beseitigen.
  Die bestehende Liquibase-Ausnahme verwendet den vom neuen Plugin normalisierten FSL-Lizenznamen.
- Dokumentation und PiTest erfassen Pfade vor der Task-Ausführung, um das abgekündigte `Task.project` zu vermeiden.
- Die bisherigen lokalen Prüfungen von Coverage, Migrationen, allgemeinen Integrationstests, Formatierung und Lizenzen bestehen.
- JUnit 6.1.3 gilt für Testsuite und Boot-BOM; Lombok steigt auf 1.18.48.
  PiTest 1.30.0 benötigt für inkrementelle Läufe zusätzlich `org.pitest:pitest-history-plugin:0.0.1`.
- Gradle bleibt auf 9.7.1: Unter 9.8 ruft das PiTest-Plugin 1.19.0 die neu abgekündigte API `Configuration.setVisible` auf.
- Weitere Versionsupdates betreffen Git-Properties, das Versions-Plugin, den OpenAPI-Prozessor,
  Datasource-Proxy, Jackson Nullable, Commons Text, JNA, ModelMapper, iban4j, Argon2 und WireMock.
  Der gezielte Lauf von `ImportHostingAssets` mit diesem Stand besteht alle 71 Tests.
- `HsOfficeContact` verwendet `@GeneratedValue(strategy = GenerationType.UUID)` statt des veralteten `@GenericGenerator`.
  `HsOfficeContactRbacRepositoryIntegrationTest` und `spotlessJavaCheck` sind erfolgreich.
- `use-latest-versions` 0.2.19 behebt den unter Gradle 9 fehlenden Zugriff auf `groovy.util.FileNameFinder`.
  `check` läuft danach separat oder in der CI, damit es die aktualisierte Konfiguration lädt.
  Das Updater-Plugin selbst meldet weiterhin veraltete `Task.project`-Zugriffe.
- `tomcat.version` überschreibt die Boot-Vorgabe 11.0.24 mit 11.0.26 wegen der enthaltenen Sicherheitskorrekturen.
- Jackson 2.21.7 und 3.1.7 beheben die gemeldeten Deserialisierungs- und DoS-Lücken,
  darunter CVE-2026-89425, CVE-2026-91776 und CVE-2026-91777.
- Logback 1.6.3 behebt die MDC-basierte Pfadmanipulation CVE-2026-19880.
- PMD 7.28.0 enthält den Fix für CVE-2026-28338; die SLA-Zuordnung im Import ist für die Komplexitätsprüfung dedupliziert.
- Swagger UI 5.32.15 enthält den DOMPurify-Fix für CVE-2026-75838;
  die Versionsangabe in der gemeinsamen `application.yml` stimmt mit dem WebJar überein.
- Die Testkonfiguration heißt `application-test.yml` und enthält nur noch Test-Abweichungen und testspezifische Einstellungen.
  `config/application.yml` im Test-Classpath importiert sie ohne Aktivierung des Security-Profils `test`.
  Gemeinsame Werte wie der Swagger-UI-Pin, der Jackson-Mapper und der Hibernate-Dialekt kommen aus `main`.
- Der JDBC-Treiber wird aus der URL ermittelt, damit sowohl PostgreSQL als auch Testcontainers funktionieren.
- Die Konfigurationsvererbung ist mit `DocumentationWebConfigUnitTest`, `jacocoTestCoverageVerification`,
  `migrationTest`, `generalIntegrationTest`, `spotlessJavaCheck` und `verifySwaggerUiVersionPin` geprüft.
- Verifiziert mit `jacocoTestCoverageVerification`, `migrationTest`, `pmdMain`, `pmdTest`, `spotlessJavaCheck`,
  `verifySwaggerUiVersionPin` und `WebSecurityConfigIntegrationTest` einschließlich Swagger UI.
- Dependency-Check meldet den HTML-Berichtslink über Gradles Problems API,
  damit er auch bei einem CVSS-bedingten Build-Abbruch im Problems-Bericht erscheint.
- Dependency-Check schreibt in `build/reports/dependency-check`,
  damit sein Ausgabeverzeichnis nicht mit den Berichten der Lizenzprüfung überlappt.
- Der .NET Assembly Analyzer ist deaktiviert, da die gefundenen DLLs native Windows-Bibliotheken von JNA und Argon2 sind.
- `AGENTS.md` nennt Java 25 als Sprachstand.
- Die ArchUnit-Regel gegen `org.junit.Test` und der auskommentierte `testCompileOnly`-Block samt `TODO.test` sind entfallen:
  Testcontainers 2 bringt JUnit 4 nicht mehr mit, die Klasse steht gar nicht mehr im Classpath.
- `EntityManagerWrapperUnitTest` liefert für Array-Parameter ein leeres Array statt eines Mocks,
  denn Mockito kann Array-Typen nicht mocken.
- Der Kommentar in `etc/local-reference-build-env/Dockerfile` nennt die toolchain-bereitgestellte Adoptium-Version 25.

### Verbleibende Dependency-Check-Funde

- Hibernate 7.4.10.Final ersetzt 7.4.5.Final wegen CVE-2026-77874.
  Der Quellcode der neuen Version maskiert auch das erste JSON-Pfadsegment in
  `JsonPathHelper.appendInlinedJsonPathIncludingPassingClause`.
  [Herstellerhinweis](https://access.redhat.com/security/cve/cve-2026-77874),
  [Release](https://hibernate.org/orm/releases/7.4/).
- Kotlin-Standardbibliothek und Reflection des OpenAPI-Prozessors sind auf 2.4.20 angehoben.
  CVE-2026-53914 betrifft den Kotlin-Build-Cache; die gemeldeten Bibliotheken sind kein Teil des Anwendungs-Classpaths.
  Das Update vermeidet hierfür eine Suppression.
  [Advisory](https://github.com/advisories/GHSA-r937-wjx7-w2jp).
- Der unbenutzte `spring-boot-starter-data-rest` und seine Konfiguration entfallen.
  Die Anwendung stellt ihre REST-Endpunkte über eigene OpenAPI-Controller bereit.
  Damit entfällt auch die falsche Zuordnung von Boot 4.1.1 zu einer verwundbaren Spring-Data-REST-Version.
- In den sieben OpenAPI-Prozessor-Classpaths ersetzt `antlr4-runtime` den unbenutzten ANTLR-Grammatikcompiler.
  Die Prozessoren verwenden bereits generierte Parser; Grammatikgenerierung gehört nicht zum Build.
  Damit entfallen CVE-2026-13500, CVE-2026-13501 und CVE-2026-13503 ohne Suppression.

Die folgenden Ausnahmen gelten jeweils nur für die geprüften Paketversionen und die genannte CVE.
Die Suppression-Datei enthält die Begründungen und Quellen.

| CVE | Paket | Begründung |
| --- | --- | --- |
| CVE-2026-47842 | Spring Security Crypto 7.1.1 | Korrigiert; die betroffenen Verschlüsselungs-APIs werden nicht verwendet. |
| CVE-2026-47838 | Spring Security Web 7.1.1 | Versionszweig nicht betroffen; keine X.509-Authentifizierung. |
| CVE-2022-31691 | Spring Boot DevTools 4.1.1 | Falsches Produkt: Spring-Tools-IDE. |
| CVE-2020-27225 | Eclipse-Core-Module des OpenAPI-Prozessors | Eclipse-Help-Server fehlt in allen Projekt-Classpaths. |
| CVE-2014-125035 | Eclipse Core Jobs 3.15.700 | Betrifft das WordPress Jobs-Plugin, nicht Eclipse. |

`dependencyCheckAnalyze` meldet nach diesen Änderungen null ununterdrückte Schwachstellen.
Die CVSS-Abbruchgrenze bleibt bei 5,0 und `failOnError` bleibt aktiv.
Die vollständige API-Generierung mit `openApiGenerate --rerun-tasks`, `jacocoTestCoverageVerification`,
`migrationTest`, `generalIntegrationTest`, `bootJar` und `spotlessJavaCheck` sind erfolgreich.
Zusätzlich sind `HsOfficePartnerRbacRepositoryIntegrationTest`, `HsBookingProjectRepositoryIntegrationTest`,
`HsHostingAssetRepositoryIntegrationTest` und `checkLicense` erfolgreich.
Das Boot-JAR enthält Hibernate 7.4.10 und weder Spring Data REST noch Kotlin-, Eclipse- oder ANTLR-Generator-Bibliotheken.

## Folge-PRs

- ModelMapper ersetzen oder kapseln:
  dessen eingebettetes `TypeResolver` ruft `sun.misc.Unsafe::staticFieldOffset`,
  auch in der neuesten Version 3.2.6, und die Methode wird in einer künftigen JDK entfernt.
  Ein Upgrade der Bibliothek löst das also nicht.
- Byte Buddy meldet dieselbe Warnung, betrifft aber nur Mockito im Testlauf
  und erledigt sich mit einem künftigen Spring-Boot-Stand.

---

## Release Note

released: 2026-09-27

### Two error responses now carry the wording of their new libraries

Since 2026-09-27, a request body above the size limit answers with the reason phrase
`Content Too Large` instead of `Payload Too Large`, as RFC 9110 renamed status 413.
A request which references a non-existing entity answers with
`No row with the given identifier exists for entity [...]` instead of `Unable to find ... with id ...`.
Both keep their status codes, 413 and 400.
