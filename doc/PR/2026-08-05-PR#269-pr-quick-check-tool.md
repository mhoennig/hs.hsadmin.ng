# PR#269: `pr-quick-check` — lokale Qualitäts-Prüfungen vor dem Review

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/269

## Das Problem

Immer wieder gehen Pull-Requests in den Review, obwohl sie an einfachen, lokal prüfbaren Punkten scheitern:
ein fehlendes oder falsch benanntes PR-Doc, offene Review-Befunde aus `tools/fixmes`, versehentlich eingecheckte `.env*`-Secret-Dateien,
oder ein Diff, der für einen einzigen PR bzw. vor allem dessen Review zu groß geworden ist.
Diese Punkte führen zu Verzögerungen, vor allem wenn sie spät entdeckt werden.

## Nicht-Ziele

- Kein Ersatz für den fachlichen Code-Review oder die CI-Test-Suite; das Tool prüft nur einfache, lokale Vorbedingungen.
- Keine automatische Behebung: das Tool meldet Befunde, ändert aber keine Dateien.

## Die Lösung

`tools/pr-quick-check` bündelt einfache Prüfungen, die vor einer Review-Anforderung grün sein müssen,
und läuft ohne Build oder Datenbank in Sekunden gegen den Diff zum Ziel-Branch (Default `origin/master`).

Geprüft werden unter anderem:

- lokaler `master` und `origin/master` zeigen auf denselben Commit, und der Branch enthält `origin/master`;
- `git diff --check` (keine Konflikt-Marker, keine Whitespace-Fehler);
- genau ein geändertes PR-Doc, das Datei- und Abschnitts-Konventionen erfüllt und im dynamischen Zeilenbudget bleibt;
- `tools/fixmes` meldet keine offenen Befunde;
- keine eingecheckten `.env*`-Secret-Dateien (nur explizite Beispiel-, Local- und Template-Namen sowie `.envrc` sind erlaubt);
- keine eingecheckten Dateien unter `build/`;
- die Änderung bleibt in einem definierten Scope-Budget, keine übergroßen Änderungen, keine übergroßen Dateien;
- alle nicht ignorierten, nicht getrackten Dateien sind über `--allow-untracked` als gewollt deklariert.

Muss ein PR das Scope-Budget berechtigt überschreiten,
deklariert sein PR-Doc eine begründete Ausnahme (Syntax siehe `pr-quick-check --help`, empfohlen unter Nicht-Ziele).
Der deklarierte Score ersetzt das reguläre Limit und bildet den ausnahmsweise genehmigten Umfang des PR.

Die Budgets skalieren mit der Änderungsgröße:
Scope-Score und PR-Doc-Zeilenbudget wachsen mit der Zahl logischer Dateien und der substantiell geänderten Zeilen;
generierte RBAC-Dateien und `released-prod-schema-*.sql`-Dumps zählen dabei nicht als substantielle Zeilen.
Die Grenzen und Details erklärt `pr-quick-check --help`.

Mit von `direnv` geladener `.envrc` liegt das Tool auf dem `PATH`; ein Aufruf aus dem Repository-Wurzelverzeichnis genügt.
Das README beschreibt Aufruf und `--help` im Abschnitt „Quick PR Quality Check".

Der Gradle-Task `prQuickCheck` ruft das Tool auf und hängt sich per `mustRunAfter` hinter `smokeTest`, `check`, `jacocoTestReport` und `generateDocumentation`.
`.gitTally` führt ihn als letzten Task des Build-Kommandos aus, sodass er am Ende des CI-Builds läuft.
Werden die Kriterien nicht erfüllt, schlägt der Build fehl, und der Branch kann im GitEA nicht ge-merged werden.

`prQuickCheck` und `smokeTest` schreiben ihre Konsolenausgabe wie die JUnit-Reports als `index.html` und `output.log` nach `build/reports/<task>/`, auch im Fehlerfall; `.gitTally` archiviert diese Verzeichnisse mit.

## Zusätzliche Änderungen

- Die Scenario-Test-Reports werden jetzt nach `build/reports/scenarios` statt `build/doc/scenarios` geschrieben,
  damit ihr Index wie die JUnit-Reports direkt auf der Build-Übersichtsseite im gitTally verlinkt wird;
  Jenkins, obwohl derzeit nicht aktiv, archiviert sie entsprechend aus dem neuen Verzeichnis, um Inkonsistenzen zu vermeiden.

## Voraussetzungs-PRs

- Keine: Dieser PR ist unabhängig und kann in beliebiger Reihenfolge gemergt werden.
