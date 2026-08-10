# PR#279: Werkzeuge: `pr-quick-check`-Prüfungen, `fixmes`-Zitatregel und PR-Vorlagen

`tools/pr-quick-check` prüft schärfer und erklärt seine Befunde besser,
`tools/fixmes` unterscheidet Erwähnung von Verwendung des Marker-Worts,
und die PR-Vorlagen bekommen ein deutsches Gegenstück samt Sammelstelle für lose Ideen.

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/279
- Hervorgegangen aus PR#277: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/277

## Das Problem

`tools/pr-quick-check` meldet bei offenen `FIXME`s nur deren Anzahl,
während die Fundstellen von `tools/fixmes` Bildschirm-weit früher im Terminal stehen.
Es misst stets gegen `origin/master`:
zielt ein PR auf den Zweig eines anderen, sieht es beide PRs, zwei PR-Dokumente und einen aufgeblähten Scope.
Das Entlasten eines zu langen PR-Dokuments nach `doc/rfc/` erhöht den Scope-Score,
bestraft also genau das Verhalten, zu dem das Zeilen-Budget auffordert — in PR#270 sieben Punkte für einen reinen Umzug.

Dazu Wildwuchs an mehreren Stellen:
Das Qualifier-Vokabular für `TODO` und `FIXME` wucherte zu fünf Einzelgängern wie `TODO.docs` neben `TODO.doc`.
RFC-Nummern können auf parallelen Branches doppelt vergeben werden, ohne dass es jemand bemerkt.
Eine bloße Erwähnung von `FIXME` in Markdown musste bisher von `disable`/`enable`-Kommentaren
eingeklammert werden, was Zeilen aufbläht.
`tools/fixmes` verarbeitete zudem je Zeile nur einen Marker:
eine Zeile mit Disable- und Enable-Kommentar schaltete den Rest der Datei stumm,
sodass offene `FIXME`s unbemerkt blieben.
Und eine deutsche PR-Vorlage fehlte, obwohl `pr-quick-check` deutsche Überschriften prüft.

## Nicht-Ziele

- Keine Überarbeitung bestehender PR-Dokumente; alte `disable`/`enable`-Klammern bleiben gültig.
- Derzeit noch keine Begrenzung der RFC-/ADR-Dateigröße; siehe den offenen Punkt unten.
- `tools/fixmes` kennt weiterhin genau die Marker-Konventionen dieses Repos.

## Die Lösung

`pr-quick-check` zitiert die Befunde von `tools/fixmes` jetzt in die Fehlermeldung.
`tools/fixmes` wertet Marker und Funde in der Reihenfolge ihrer Position innerhalb der Zeile aus.
In Markdown gelten Backtick-Spans als Zitat:
ein erwähntes `FIXME` zählt nicht, denn um ein echtes ergeben Backticks keinen Sinn;
die `disable`/`enable`-Klammern bleiben nur als Notbremse für Sonderfälle.
Dieselbe Zitatregel gilt für die Qualifier-Prüfung.

Das PR-Dokument darf seinen Ziel-Zweig deklarieren, etwa `pr-quick-check:base=<ref>`;
das gilt, bis dieser gemergt oder gelöscht ist, danach liefert die Vorgabe-Basis dasselbe.
Bei tiefer gestapelten PRs tragen auch die Voraussetzungs-Dokumente ihre Deklarationen;
als eigene gilt nur die des Dokuments, das in keiner der deklarierten Basen schon enthalten ist.

Drei neue Prüfungen:

- Dokumente unter `doc/rfc/` und `doc/adr/` zählen wie das PR-Dokument derzeit keine substantiellen Zeilen;
  das Entlasten eines zu langen PR-Dokuments dorthin kostet keinen Scope-Score mehr.
- In hinzugefügten Zeilen sind nur definierte Qualifier für `TODO` und `FIXME` zulässig;
  Altbestand bleibt stumm, bis seine Zeile ohnehin angefasst wird.
- RFC-Nummern: doppelt vergeben im eigenen Arbeitsstand schlägt fehl,
  dieselbe Nummer anders benannt auf einem offenen origin-Branch warnt nur, wie bei Merge-Konflikten;
  schon der erste Lauf fand die doppelte Nummer 4 auf `origin/mihoe/rfc0004-global-auditor-role`.

Konflikt-Warnungen gegen andere origin-Branches nennen nur noch Dateien, die der geprüfte Branch selbst ändert;
Konflikte, die ein anderer Branch allein durch seinen Rückstand zur Basis hat,
sind dessen Nachricht und erscheinen bei dessen eigenem Lauf.

Die neue `doc/PR/TEMPLATE-de.md` trägt genau die deutschen Überschriften, die `pr-quick-check` kennt;
ein PR-Dokument ist deutsch oder englisch, nie gemischt (abgesehen von den Release-Notes),
und `doc/PR/README.md` verweist auf die Vorlagen, statt die Abschnitte ein zweites Mal aufzuzählen.
Das neue `doc/PR/IDEAS.md` sammelt Ideen und TODOs ohne konkrete Code-Stelle;
`pr-quick-check` behandelt es nicht als PR-Dokument.

## Offene Punkte

- [ ] TODO.tool: Das Aufblähen von RFC- und möglichst auch ADR-Dokumenten begrenzen,
      denn ohne Zeilenzählung fehlt dort jeder Gegendruck; drei der fünf RFCs liegen schon bei 486–657 Zeilen.
      Vorschlag: fester Deckel je Datei (etwa 300 bzw. 200 Zeilen) als Merge-Base-Ratchet nur für geänderte Dateien,
      dazu eine deklarierte Ausnahme im PR-Dokument analog zum Scope-Budget.
- [ ] TODO.tool: Separate Release-Notes für extern (mit API-Bezug) und intern (Architektur/Tooling etc.).

## Zusätzliche Änderungen

- Der Hook `.claude/hooks/reject-final-var.sh` weist `final var` (statt Lombok `val`) schon beim
  Schreiben zurück; er prüft nur den frisch geschriebenen Text, damit Altbestand still bleibt.

## Folge-PRs

- Evtl. Folge-PRs aus den TODOs oben.
