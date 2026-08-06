# PR#266: Backend-Entwicklerdokumentation zu RBAC-Triggern und Datenbank-View-Typen

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/266

## Das Problem

Das Wissen über die RBAC-Triggerstruktur und die Datenbank-View-Typen (`_rv`, `_iv`, `_ev`, `_hv`, `_xv`)
steckt bisher nur verteilt im SQL-Code der Changelogs und in den Java-Generatoren.
Für Reviews, Sicherheitsbetrachtungen und die Einarbeitung neuer Backend-Entwickler fehlte eine kompakte Referenz,
welcher Trigger bzw. welche View woher kommt (generiert oder direktes Changeset) und wofür sie gedacht ist.

## Nicht-Ziele

- keine Verhaltensänderung an Java-Code, SQL oder Changesets
- keine vollständige RBAC-Konzeptdokumentation, dafür gibt es weiterhin [rbac.md](../rbac.md)
- keine Bewertung oder Behebung dabei aufgefallener Befunde, nur nüchterne Dokumentation des Ist-Zustands

## Die Lösung

Zwei neue Nachschlage-Dokumente unter `doc/`:

- [rbac-triggers-de.md](../rbac-triggers-de.md) (deutsch):
  generische Übersicht der Trigger auf RBAC-verwalteten Tabellen und deren Restricted Views,
  inklusive der Durchsetzungs-Matrix je Operation und der alphabetischen Ausführungsreihenfolge samt latenter Namensfalle.
- [database-view-types.md](../database-view-types.md) (englisch):
  tabellarische Referenz aller View-Typ-Suffixe mit Herkunft
  (zweistufige Generierungspipeline via `rbacGenerate` bzw. direktes Changeset),
  Zeilenfilter, Schreibbarkeit, typischen Konsumenten und den Quelldateien im Repository.

Beide Dokumente sind als Referenz für Reviews und Sicherheitsbetrachtungen gedacht,
nicht als Ersatz für die Konzeptdokumentation.

## Offene Fragen

- [ ] TODO.docs: Sollen die beiden Dokumente in `documentationMarkdownSources`
  (`buildSrc/src/main/kotlin/hsadmin.documentation.gradle.kts`) aufgenommen werden,
  damit `./gradlew generateDocumentation` auch PDFs davon erzeugt?
