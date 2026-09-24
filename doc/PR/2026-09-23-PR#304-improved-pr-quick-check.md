# PR#304: Verbessertes `tools/pr-quick-check`

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/304

## Das Problem

Die bisherige Fassung von `tools/pr-quick-check` misst in drei Punkten an den falschen Größen
und drängt dadurch zu schlechterer Arbeit.
Aufgefallen ist das bei der Arbeit an [PR#302](2026-09-22-PR%23302-bank-directory-and-bic-validation.md)
und [PR#303](2026-09-22-PR%23303-bankaccount-bank-name.md).

1. Ihr Zeilenbudget für das PR-Dokument wächst nur mit dem Code.
   Der Dokumentationsbedarf wächst aber mit der Zahl der Anforderungen:
   In PR#302 nimmt allein der Abschnitt mit den Gherkin-Szenarien 55 % des Dokuments ein.
   Ein kleiner, fachlich dichter PR muss deshalb Begründungen und Szenarien streichen.
2. Für das Zeilenbudget sieht sie keine Ausnahme vor, anders als für den Scope-Score.
   Ein PR, der nur den CR-Abschnitt enthält, kann die Prüfung darum grundsätzlich nicht bestehen,
   und dies verleitet KI-Agenten dazu, die Anforderungen zu kürzen.
3. Sie unterscheidet eine Deklaration nicht von ihrer Erwähnung.
   Enthält eine Zeile etwa `extra-pr-docs`,
   verlangt sie dort fälschlicherweise eine vollständige Deklaration mit Anzahl und Grund, trotz Backticks.
   Ein PR-Dokument, das diese Regeln beschreibt oder ändert, scheitert deshalb als „fehlerhafte Deklaration“.

## Nicht-Ziele

- Keine Änderung an den Grenzwerten für Scope-Score, Kommentaranteil oder Dateigröße.
- Keine Änderung an der Form der Szenarien selbst.
- Keine Änderung an den Szenario-Reports oder ihrer Titelbildung.
- Keine `@Scenario`-Annotationen für Szenarien bereits gemergter PRs;
  ohne Annotation meldet `tools/generate-verified-by` nichts, das Nachtragen steht in [IDEAS.md](IDEAS.md).
- Keine fachlich geordnete Kennung für `@Scenario`, siehe [IDEAS.md](IDEAS.md);
  die Kennungen hängen vorerst an der PR-Nummer.
- Keine Ausnahme für generierte Datenskripte;
  das Bankenverzeichnis der Bundesbank gehört schon aus Lizenzgründen nicht als Changeset ins Repository.

## Die Lösung

`tools/pr-quick-check` misst jetzt an dem, was die drei Punkte tatsächlich treibt;
Regeln und Formeln stehen in `tools/pr-quick-check --help`.

1. Das Zeilenbudget des PR-Dokuments wächst zusätzlich um 12 Zeilen je Szenario.
   Das deckt ein kurzes Szenario; jedes längere kostet ein paar Zeilen des allgemeinen Budgets
   und begrenzt so die Zahl der Szenarien in einem PR.
2. Die neue Ausnahme `max-pr-doc-lines` ergänzt `max-scope-score`.
3. Eine Deklaration wirkt nur außerhalb von Backticks; in Backticks ist sie Dokumentation.
   README und beide Vorlagen beschreiben das.

## Zusätzliche Änderungen

- `tools/generate-verified-by` erzeugt die `Verified by`-Abschnitte aus `@Scenario`-annotierten Szenario-Tests,
  statt sie abschreiben zu lassen; beim Abschreiben fehlte in PR#302 ein Report-Link,
  und alle `master`-Links zeigten ins Leere.
  `tools/pr-quick-check` prüft die Abschnitte damit; Regeln und Fehlerfälle stehen in `tools/generate-verified-by --help`.
  Beide Vorlagen zeigen den erzeugten Abschnitt.
- Beide Vorlagen halten fest, dass Gherkin-Szenarien fachliche Anforderungen beschreiben
  und Anforderungen an Werkzeuge knapp in Prosa.
- `doc/PR/README.md` hält fest, dass „Das Problem“ die bisherige Fassung im Präsens beschreibt,
  und begründet, warum Szenarien in Pseudo-Gherkin statt in umzäunten Gherkin-Blöcken stehen.

## Folge-PRs

- [PR#302](2026-09-22-PR%23302-bank-directory-and-bic-validation.md) und
  [PR#303](2026-09-22-PR%23303-bankaccount-bank-name.md) bauen darauf auf und brauchen diese Budgets.
  Ihre `Verified by`-Abschnitte sind mit dem Werkzeug erzeugt.
