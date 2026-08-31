# Kommentar-Quote im pr-quick-check begrenzen

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/292

## Das Problem

`AGENTS.md` verlangt, dass ein Kommentar den Grund nennt und nie wiederholt, was der Code ohnehin zeigt.
Bisher setzt das allein der Review durch, und zwar Fundstelle für Fundstelle.
Gerade agentisch erzeugter Code neigt zu erklärender Prosa vor jeder zweiten Anweisung;
solche Sätze veralten mit der ersten Änderung am Code darunter und führen den nächsten Leser dann in die Irre.

## Nicht-Ziele

- Keine Bewertung, ob ein einzelner Kommentar gut ist; das bleibt Sache des Reviews.
- Keine Prüfung bestehender Dateien, nur der vom PR hinzugefügten Zeilen —
  Altlasten sollen nicht jeden PR blockieren, der eine solche Datei anfasst.
- Keine Erfassung von Markdown; dort ist Prosa der Inhalt und nicht der Kommentar.
- Keine Ausnahme-Deklaration im PR-Dokument wie beim Scope-Budget:
  die meist überflüssigen Kommentare sollen ja gerade verhindert werden,
  und eine deklarierbare Ausnahme würde genau sie wieder zulassen.

## Die Lösung

`tools/pr-quick-check` prüft zusätzlich, dass höchstens 12 % der Zeilen,
die ein PR zu Programmcode hinzufügt, reine Kommentarzeilen sind.
Erfasst sind `.java`, `.kt`, `.kts`, `.sql`, `.sh`, `.py`, `.yaml` und `.yml`
sowie die Werkzeuge ohne Endung, erkannt an ihrem Shebang.
Auch in Tooling-Skripten soll kein Kommentar-Ballast entstehen,
und YAML ist Konfiguration und damit Code, keine Prosa.

Gezählt wird nur erklärende Prosa, denn nur sie kann mit dem Code auseinanderlaufen.
Da `/* … */` einen Bereich aufzieht, dessen Innenzeilen kein eigenes Zeichen tragen müssen,
lässt sich eine Zeile nicht für sich betrachten:
die Prüfung liest die ganze Datei und verfolgt Blockkommentare und Zeichenketten über Zeilengrenzen.
Ohne das Überspringen der Zeichenketten würde ein `"http://…"` einen Kommentar eröffnen.

Nicht als Kommentar zählen:

- Leerzeilen, auf keiner der beiden Seiten der Quote,
- ein angehängter Kommentar hinter einer Anweisung,
- Liquibase-Direktiven wie `--changeset` oder `--validCheckSum`, die die Migration steuern,
- Trennlinien ohne Buchstaben und leere Block-Kommentar-Ränder wie `/**` oder `*/`,
- die Konventions-Marker `// given`, `// when` und `// then`; sie benennen eine Stelle, keinen Grund,
- Anweisungen an ein Werkzeug statt an einen Leser, also `// @formatter:off` und `//noinspection`;
  erkannt am Präfix, denn sie tragen Argumente und im Bestand vereinzelt Tippfehler wie `@formatter:ff`,
- der Kopfblock eines Skripts, also Shebang und der Kommentarblock darunter:
  er ist laut `AGENTS.md` die Quelle der `--help`-Ausgabe und damit Dokumentation, nicht Beiwerk.
  Alles darunter zählt, und in einer Konfigurationsdatei zählt auch der Kopf,
  sonst wäre er das Versteck für die Prosa.

Einzelne Dateien sind ganz ausgenommen, aufgezählt in `COMMENT_RATIO_EXEMPT` am Kopf des Werkzeugs.
Jedes Muster trifft auf den Pfad oder auf den bloßen Dateinamen zu, ein Name allein also überall.
Ausgenommen sind `.gittally.yml` und `application*.yml`,
deren Kommentare den Grund einer Einstellung tragen, den der Schlüssel selbst nicht nennen kann,
sowie `gradlew`, das aus dem Gradle-Wrapper stammt und hier niemand schreibt.
Was die Prüfung übersprungen hat, nennt sie in ihrer Meldung.

Erlaubt sind immer mindestens zwei Kommentarzeilen, damit auch eine kleine Änderung Platz
für einen Kommentar behält, der sich verdient hat.
Die Regel bleibt an dieser Kante stetig, denn 12 % von 20 Zeilen sind ebenfalls zwei.
Eine Untergrenze, unterhalb derer gar nicht geprüft wird, gibt es dadurch nicht:
zehn Kommentarzeilen zu neunzehn Zeilen Code sind genau der Ballast, um den es geht.
Die Fehlermeldung nennt die fünf Dateien mit den meisten Kommentarzeilen, damit klar ist, wo zu kürzen ist.

Die 12 % sind an der bisherigen Praxis geeicht:
Von den vierzehn zuletzt gemergten PRs bestehen zehn, sechs davon unter 9 %.
Zwei bestehen erst über die zwei Zeilen Untergrenze, weil sie kaum Code hinzufügen: #290 und #282.
Vier liegen darüber: #264 mit 13,4 %, #285 mit 13,6 %, #275 mit 16,5 % und #281 mit 18,4 %.
Ohne die Ausnahmeliste wäre #282 bei 34,5 % gelandet, allein wegen seiner neuen `.gittally.yml`.
Die API-Definitionen unter `api-definition/` liegen dagegen bei rund 1 % und brauchen keine Ausnahme.

## Zusätzliche Änderungen

- `.aider.conf.yml` entfernt, samt seiner beiden Erwähnungen im `README.md`.
  Die Datei war die unveränderte Beispiel-Konfiguration von aider, die alle Einträge auskommentiert aufzählt,
  und fiel nur auf, weil sie mit 315 von 462 Kommentarzeilen sonst eine Ausnahme gebraucht hätte.
