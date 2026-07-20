# RFC#0003: Shell-Tooling — `.sh`-Konvention, Tab-Einrückung und `tools/lib`-Kapselung

- Status: Entwurf
- Stand: 2026-07-20
- Author: Michael Hönnig

Untypisches, rein technisches RFC (kein fachliches Feature): Konventionen für die
Shell-Skripte unter `tools/`. Motiviert durch die API-Key-Tooling-Arbeit in PR#244,
aber davon unabhängig und in einem eigenen Branch umzusetzen.

## Ausgangslage

Die Skripte unter `tools/` sind uneinheitlich benannt: ausführbare Kommandos ohne
Endung (`http`, `remote`, `api`, …), gesourcte Fragmente mit `.sh`
(`tools/lib/apikey-login.sh`, `tools/lib/logout.sh`).

Ihre `--help`- und Request-Body-Texte stehen in Heredocs. Für lesbaren Quelltext soll
der Heredoc-Body zusammen mit dem Code eingerückt sein, im Output aber bündig links.
Bash bietet dafür `<<-`, das führende **Tabs** aus Body *und* Terminator-Zeile strippt.

Die repo-weite `.editorconfig` setzt jedoch `indent_style = space`. Editoren formatieren
Tabs zu Spaces um und zerstören damit still den `<<-`-Delimiter (`bash -n`:
„here-document delimited by end-of-file"). `<<-` ist unter dieser Konfiguration also
unbrauchbar.

Als Zwischenlösung dient in PR#244 der Filter `tools/lib/unindented.sh`: space-basiert,
dedentet zur Laufzeit anhand der Einrückung der ersten Zeile. Das funktioniert, hat aber
zwei Nachteile:

- ein zusätzlicher Helfer plus eine `source`-Zeile je Tool,
- der Terminator muss zwingend in Spalte 1 stehen (asymmetrisch zum eingerückten Body).

## Motivation

- Nativer, einheitlicher Umgang mit eingerückten Heredocs ohne Zusatz-Helfer und ohne
  Asymmetrie zwischen Body und Terminator.
- Eine klare, einheitliche Namens- und Ablage-Konvention für alle Shell-Skripte.

## Zielbild

- **Alle** Shell-Skripte tragen die Endung `.sh`.
- Die hübschen, endungslosen Kommandonamen entstehen ausschließlich über Aliase bzw.
  Functions in `.aliases` (viele existieren schon: `HTTP`, `LOGIN`, `APIKEY`, `remote`,
  `fixmes`, `api`, `howto`).
- Implementierungen, die nicht direkt aufgerufen werden, liegen unter `tools/lib/`;
  dorthin wandern auch `http` und `jwt-login`.
- Eine eigene `.editorconfig`-Regel für `*.sh` setzt `indent_style = tab`.
- Damit werden **native `<<-`-Heredocs** möglich — Body und Terminator dürfen mit Tabs
  eingerückt sein — und `tools/lib/unindented.sh` entfällt ersatzlos.

## Entwurfsentscheidungen

### `<<-` mit Tabs statt Laufzeit-Dedent

Native Bash-Semantik, symmetrisch (auch der Terminator ist eingerückt), kein Helfer und
keine `source`-Zeile. `<<-` strippt allerdings **nur Tabs**. Im Heredoc-Body gilt daher
„Tabs zum Einrücken, Spaces zum Ausrichten": die strukturelle Einrückung sind Tabs (im
Output gestrippt), die relative Sub-Einrückung (Aufzählungs-Bullets, ausgerichtete
Fortsetzungszeilen) sind Spaces hinter den Tabs (bleiben erhalten).

### Eigene `.editorconfig` für `*.sh` statt globaler Ausnahme

`indent_style = tab` (mit `tab_width = 4` zur Anzeige) stellt die **ganze** `.sh`-Datei
auf Tabs — bewusst auch den Code, nicht nur die Heredocs, denn EditorConfig kann nicht
„Tabs nur im Heredoc" ausdrücken. Ein Mischstil (Space-Code, Tab-Heredoc) wäre nicht
stabil formatierbar. Die übrigen Dateien bleiben unter der `[*]`-Regel space-basiert.

### `.sh` für alle, Kommandonamen aus `.aliases`

Eine einheitliche Endung gibt Editoren und Tooling eine klare Zuordnung; die endungslosen
Kommandonamen bleiben über die Wrapper in `.aliases` erhalten. `tools/lib/` markiert
„nicht direkt aufgerufen".

### Verworfen: `unindented` dauerhaft beibehalten

Funktioniert, kostet aber je Tool Helfer und `source`-Zeile und lässt den Terminator in
Spalte 1. Nur nötig, solange die `.editorconfig` Spaces erzwingt — genau das hebt dieses
RFC auf. `unindented` bleibt der Zwischenstand in PR#244, bis dieser Umbau erfolgt.

### Verworfen: explizite Pfadliste in der globalen `.editorconfig` ohne Rename

`[tools/{http,jwt-login,…}] indent_style = tab` wäre ohne Umbenennen möglich, ist aber
pflegeintensiv (jede neue Datei nachtragen) und ohne die klare `.sh`-Konvention. Dieselbe
Ganzdatei-Tab-Konsequenz gilt auch hier.

## Umsetzung

Eigener Branch/PR, **nicht** PR#244. Reines Konventions- und Formatierungs-Refactor,
keine Verhaltensänderung der Tools. Umfang:

- Renames auf `.sh`; Verschieben von `http` und `jwt-login` nach `tools/lib/`.
- Tab-Umstellung der betroffenen Dateien (mechanisch, ganze Datei).
- Entfernen von `tools/lib/unindented.sh` und aller `unindented`-Aufrufe; Heredocs auf
  `<<-'HELP'` mit Tab-Einrückung umstellen.
- Nachziehen der Aufrufer: `.aliases` (Pfade in `HTTP`/`LOGIN`/`APIKEY`/`LOGOUT` und den
  `alias …=tools/…`-Zeilen), `README.md`, Memory-Verweise, geschwister-aufrufende Tools
  (`tools/remote`).
- `.gitattributes` passt bereits (`*.sh text eol=lf`).

## Nicht-Ziele / Follow-Ups

- Keine funktionale Änderung der Tools; Ein- und Ausgaben bleiben identisch.
- Nicht Teil von PR#244; dort bleibt der space-basierte `unindented`-Zwischenstand.
- `.aliases`, `.tc-environment`, `.environment` behalten (mangels `.sh`) die
  Space-Einrückung der `[*]`-Regel.

## Begriffsabgrenzung

- **`<<`** vs. **`<<-`**: nur `<<-` entfernt führende Tabs aus Body und Terminator-Zeile;
  Spaces bleiben in beiden Fällen unangetastet.
- **„Tabs zum Einrücken, Spaces zum Ausrichten"**: strukturelle Einrückung per Tab
  (wird von `<<-` gestrippt), Ausrichtung von Bullets/Spalten per Space (bleibt erhalten).
