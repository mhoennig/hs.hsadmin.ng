
## Relevante Links (Hostsharing-intern)

Dieser Abschnitt ist optional.

Eine Stichpunktliste mit Links auf den GitEA-PR, das Taiga-Ticket und andere verwandte Ressourcen.

## Das Problem

Dieser Abschnitt ist Pflicht.

Eine Prosa-Beschreibung des Problems, das dieser PR lösen soll.
Kurz halten!

## Nicht-Ziele

Dieser Abschnitt ist Pflicht.

Was dieser PR bewusst nicht tut, um den Umfang abzugrenzen.
Meist eine knappe Stichpunktliste.

Muss dieser PR das Scope-Budget von `tools/pr-quick-check` berechtigterweise überschreiten,
hier die genehmigte Obergrenze als Scope-Budget-Ausnahme deklarieren (siehe `pr-quick-check --help`).
Die Obergrenze darf Raum für weitere zusammengehörige Änderungen lassen, zum Beispiel:

- `pr-quick-check:max-scope-score=<n>`: Grund, warum ein Aufteilen dieses PRs nicht sinnvoll wäre

## Die Szenarien

Dieser Abschnitt ist optional für Wartungs- und Bugfix-PRs, aber Pflicht für Features.

Eine schematisierte Spezifikation der Anforderungen,
bevorzugt mit [Gherkin](https://cucumber.io/docs/gherkin/reference/)-Vokabular.
Für manche PR-Arten ergibt Gherkin allerdings keinen Sinn.

Markdown-natives Pseudo-Gherkin statt eingezäunter Gherkin-Codeblöcke verwenden.
So bleiben die Szenarien verlinkbar und erlauben direkte Links auf Tests, Issues, ADRs oder Erläuterungen im Anforderungstext.
Die Gherkin-Strukturwörter bleiben auch in deutschen PR-docs Englisch:
`Feature:`, `Background`, `Scenario#...`, `Given`/`When`/`Then`, `Verified by`.

### Feature: Überschrift des Features

#### Background

- Begriffsdefinitionen
- weitere Hintergrundinformationen

#### Scenario#236.01: Beschreibung einer Anforderung in Form eines Szenarios!

damit ... (hier das Ziel hinter der Anforderung beschreiben).

- **Given** eine Vorbedingung
    - **and** eine weitere Vorbedingung
- **When** was auch immer getan wird
- **Then** Nachbedingung
    - **and** eine weitere Nachbedingung

##### Verified by

- [ExampleScenarioTests.exampleScenario](../../src/test/java/net/hostsharing/hsadminng/example/ExampleScenarioTests.java)
  using the use-case [ExampleUseCase](../../src/test/java/net/hostsharing/hsadminng/example/ExampleUseCase.java)

Sowohl die Szenario-Testklasse als auch die zugehörige UseCase-Klasse verlinken.
Methoden lassen sich in Markdown nicht verlinken, Klassendateien schon — zumindest in einer IDE.

#### Scenario#236.02: Beschreibung einer weiteren Anforderung in Form eines Szenarios!

...

Siehe [Beispiele](2026-06-25-PR%23236-realm-prefix-based-user-and-group-subject-visibility.md).

Solche Feature-Beschreibungen helfen auch sehr beim Ableiten von Tests und können agentische Coding-KI gut führen.

## Die Lösung

Dieser Abschnitt ist Pflicht.

Hier die vorgenommenen Änderungen beschreiben und warum sie so umgesetzt wurden.

Bei Bedarf auf ein ADR (Architecture Decision Record) verlinken.

## Offene Fragen

Dieser Abschnitt ist optional.

Eine Stichpunktliste mit Entscheidungen, die bewusst dem Reviewer oder einem Folge-PR überlassen werden.

## Zusätzliche Änderungen

Dieser Abschnitt ist optional.

Hier weitere Änderungen auflisten, z.B. "Formatierung in ... korrigiert" oder "einige Benennungen bereinigt".
Kurz halten!

## Voraussetzungs-PRs

Dieser Abschnitt ist optional.

Hier PRs auflisten, auf denen dieser PR aufbaut.

Zielt dieser PR auf den Zweig eines dieser PRs statt auf die Vorgabe-Basis,
diesen Zweig hier benennen, damit `tools/pr-quick-check` den richtigen Diff misst:

- `pr-quick-check:base=<ref>`

## Folge-PRs

Dieser Abschnitt ist optional.

Hier Arbeit auflisten, die bewusst auf spätere PRs verschoben wird.

## Anhänge

Dieser Abschnitt ist optional.

Hier können längere Abschnitte stehen, die den Lesefluss in den vorigen Abschnitten stören würden.
Jeden Anhang unter eine eigene Level-3-Überschrift ('### ...') setzen.

---

## Release Note

released: <YYYY-MM-DD>

Nur für Änderungen, die Nutzer der REST-API beobachten können, sonst diesen Anhang weglassen.
Der Build sammelt diese Notizen auf die öffentliche Neuigkeiten-Seite unter `/doc/`,
darum diesen Anhang samt Überschrift auf Englisch schreiben.
`released:` nennt den Merge-Tag; `tools/pr-quick-check` besteht darauf, dass es das aktuelle Datum ist.
Die Notiz als datierte Aussage über die Änderung schreiben, nicht als Zustandsbeschreibung, zum Beispiel:

### Booking projects no longer require a debitor UUID

Since 2026-05-15, booking projects can be fetched without a debitor UUID.
