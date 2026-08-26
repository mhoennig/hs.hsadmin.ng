# Smoke-Test: stderr des tools/remote-Schritts in die Fehlermeldung aufnehmen

## Relevante Links (Hostsharing-intern)

- [Gitea-PR #290](https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/290)
- [fehlgeschlagener master-Build vom 2026-08-26](https://vm4006.hostsharing.net/builds/master-fc613b4dfd67-2026-08-26T11_35_59.760147174Z-9f54a168f11f)

## Das Problem

Der Smoke-Test-Schritt für `tools/remote backend sql` verwarf die Fehlerausgabe des Tools (`2>/dev/null`).
Schlug der Schritt fehl, meldete er nur „tools/remote backend sql failed:“ ohne jede Begründung.

Genau so geschah es in den beiden fehlgeschlagenen master-Builds vom 2026-08-26:
`tools/remote` lehnte das veraltete `HSADMINNG_DEPLOY_HOST=smoke-test@stubbed` (seit PR #281 nur noch nackter Host)
mit einer klaren Meldung ab, aber diese Meldung erreichte das Build-Log nie.
Die Diagnose erforderte Vergleiche über mehrere Commits statt eines Blicks ins Log.

## Nicht-Ziele

- Keine Änderung an `tools/remote` selbst.
- Der Negativ-Test gegen `GET /api/rbac/roles` behält sein `2>/dev/null`:
  dort ist der Fehlschlag erwartet, seine Fehlerausgabe wäre nur Rauschen.

## Die Lösung

Der Schritt fängt stderr jetzt mit `2>&1` zusammen mit stdout in `$response` ein;
im Fehlerfall enthält die Meldung von `failed` damit die eigentliche Ursache.
Die Erfolgsprüfung `grep -qx 1` bleibt unberührt,
weil sie zeilenweise auf exakt `1` prüft und zusätzliche stderr-Zeilen (z. B. von `psql --version`) ignoriert.
