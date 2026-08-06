# PR#272: `pr-quick-check` — auf Basis-Branches ohne Änderungen trivial bestehen

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/272

## Das Problem

Seit `prQuickCheck` am Ende jedes CI-Builds läuft, schlägt der Build auf dem `master` selbst fehl:
Dort gibt es keinen Diff zur Base und damit kein PR-Doc, was das Tool als Fehler meldet.
Dasselbe würde auf `main` oder einem künftigen `prod`-Branch passieren.

## Nicht-Ziele

- Keine explizite Ausnahmeliste mit Branch-Namen, weder im Tool noch in der CI-Konfiguration;
  solche Listen veralten und müssten für jeden neuen Basis-Branch gepflegt werden.

## Die Lösung

Statt Branch-Namen aufzuzählen, nutzt das Tool das git-native Kriterium:
Ist der Diff zur Base leer und gibt es keine untracked Dateien, existiert schlicht kein PR-Inhalt —
der Check besteht dann trivial mit `PASSED: no changes against <base>`.
Das deckt `master`, `main` und auch ein künftiges `prod` als Release-Schnitt ab.
Sobald irgendeine Änderung vorliegt, greifen alle Prüfungen unverändert.

Zusätzlich ermittelt `default_base()` die Default-Base jetzt zuerst über `origin/HEAD`,
mit Fallback auf `origin/master`, `master`, `origin/main`, `main`;
das Tool funktioniert damit auch in Repositories mit `main` als Hauptbranch.

## Voraussetzungs-PRs

- Keine: Dieser PR ist unabhängig und kann in beliebiger Reihenfolge gemergt werden.
