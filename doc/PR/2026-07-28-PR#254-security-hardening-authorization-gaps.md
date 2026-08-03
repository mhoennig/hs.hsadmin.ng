# PR#254: Autorisierungslücken aus der ersten Runde des Backend-Security-Audits schließen

## Verwandte Links (Hostsharing-intern)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/545
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/254

## Das Problem

Ein Backend-Security-Audit (Hostsharing-intern) lief in zwei Runden und meldete Autorisierungs-Findings.
Dieser PR schließt die Findings der ersten Runde in den Modulen rbac, office und accounts.
PR#260 schließt die Findings der zweiten Runde; sein Branch baut auf diesem Branch auf.

Die erste Runde meldete vier Findings.
Drei davon sind real, und dieser PR behebt sie:

- A-01: die INSERT-Permission-Prüfung ignorierte das `assumed`-Flag.
- A-02: die Relation-Schreibpfade akzeptierten eine UUID vom Client ohne Sichtbarkeitsprüfung.
- A-03: die Account-Endpoints hatten keine Sichtbarkeitsbedingung.

Das vierte Finding (A-04) ist so, wie gemeldet, nicht korrekt; ein Abschnitt weiter unten nennt die Gründe.

## Nicht-Ziele

- Der Rollen-Graph von `hs_office.relation` ändert sich nicht, nur die Permission-*Prüfung* ändert sich.
- Keine neue Autorisierungsschicht:
  jeder Fix nutzt die `_rv`-Restricted-Views oder die `VISIBLE_SUBJECT_CONDITION` der Subjects.
- Dieser PR spezifiziert nicht, wer einen Account löschen darf.
  Diese Spezifikation kommt später; bis dahin gilt die engste Regel.

## Hintergrund

Die Repositories lesen und schreiben normalerweise über die `_rv`-Restricted-Views, und dort greifen die Sichtbarkeitsprüfungen.
Aber diese Views sind die einzige Zeilen-Beschränkung in diesem System; es gibt keine PostgreSQL-RLS-Policies.
Ein Code-Pfad, der eine Basistabelle direkt erreicht — mit `em.getReference`, mit einer nativen Query,
oder über eine Tabelle ohne `_rv`-View wie `hs_accounts.account` — hat also keine zweite Schutzschicht:

- Ein direkter Schreibzugriff umgeht den `INSTEAD OF`-Trigger der View, und damit die Permission-Prüfung.
- Ein direkter Lesezugriff liefert alle Zeilen; die Query selbst muss die Sichtbarkeitsbedingung enthalten.

Es gibt zwei Läufe durch den Grant-Graphen, und sie sind mit Absicht verschieden:
`rbac.queryAccessibleObjectUuidsOfSubjectIds` folgt nur den Kanten mit dem `assumed`-Flag,
während `rbac.isGranted` allen Kanten folgt.
Um zu entscheiden, was ein Aufrufer in diesem Moment tun darf, ist der erste Lauf der richtige.

## Die Lösung

### A-01 — die INSERT-Permission-Prüfung ignorierte das `assumed`-Flag

`rbac.isPermissionGrantedToSubject`, und damit `rbac.hasInsertPermission`,
akzeptierte eine Permission, die der Aufrufer erst nutzen kann, nachdem er die gewährende Rolle assumed.
Dadurch konnte der Owner einer Person eine neue Relation an einer fremden Person verankern.

Der rekursive Lookup folgt jetzt nur noch den Kanten mit dem `assumed`-Flag,
genau wie `rbac.queryAccessibleObjectUuidsOfSubjectIds`.

Die Korrektur ist ein neues wiederholbares Changeset in `1050-rbac-base.sql`,
weil das freigegebene Changeset `rbac-base-GRANTS` nicht wiederholbar ist.
Das neue Changeset ersetzt nur Funktionen, ein Deployment muss die Grants also nicht neu aufbauen.
Es droppt außerdem die freigegebene skalare Overload-Variante von `isPermissionGrantedToSubject`:
die Overload hat keinen Aufrufer, und der Drop entfernt die Vor-Fix-Semantik auch aus aktualisierten Datenbanken.

Der Defekt blieb verborgen, weil die einzigen Insert-Verweigerungstests `rbactest.customer` nutzen,
dessen Trigger `hasInsertPermission` nie aufruft.
Die Tabellen, die es aufrufen, hatten nur Positivtests,
und eine Prüfung, die zu viel erlaubt, kann an einem Positivtest nicht scheitern.

### A-02 — die Relation-Referenzen hatten keine Sichtbarkeitsprüfung

Die Schreibpfade machten aus einer Person- oder Contact-UUID vom Client mit `em.getReference` eine Entity,
und diese Methode prüft keine Sichtbarkeit.
Dadurch konnte ein Aufrufer eine Person oder einen Contact, die er nicht sehen darf, in eine Relation ziehen.
Der Insert-Trigger verdrahtet die referenzierten Objekte dann in den Rollen-Graphen der Relation,
was den Rollen-Inhabern der Relation Rechte an ihnen gewährt — bis zu `contact:ADMIN`, Schreibzugriff auf den fremden Contact.
Die Lücke war bekannt und bislang harmlos, solange nur Global-Admins die API benutzten;
mit der geplanten Öffnung der API wird sie zum Defekt.

Das Person- und das Contact-RBAC-Repository bekamen eine Methode `findVisibleByUuid`.
Sie liest die `_rv`-View, und sie liefert die Real-Entity, weil das der Typ ist, den eine Referenz hält.
Die `HsOfficeRelation*`- und `HsOfficePartner*`-Schreibpfade lösen jede Client-Referenz darüber auf.
Eine Referenz auf eine unsichtbare Zeile ergibt jetzt `404`, und keinen Grant.

Die Patcher behalten ihr Patch-auf-null-Verhalten:
eine `null`-UUID leert die Referenz weiterhin und lässt den Lookup nicht fehlschlagen.
Für die Not-Null-Spalte holder scheitert der Flush dann am `NOT NULL`-Constraint, wie zuvor.

### A-03 — die Account-Endpoints gaben Zugriff auf fremde Accounts

Die Account-Tabelle hat keine `_rv`-View, und die Account-Lookups hatten auch keine eigene Sichtbarkeitsbedingung.
Dadurch konnte jedes authentifizierte Subject die Accounts anderer Organisationen lesen, auflisten und löschen.

Die UUID eines Accounts ist gleich der UUID seines Subjects,
daher gilt `RealSubjectRepository.VISIBLE_SUBJECT_CONDITION` unverändert.
Neue `findVisibleBy*`-Queries beschränken den Einzel-Lookup und die Liste-nach-Person.
Das unbeschränkte `findByPerson` verlor seinen letzten Aufrufer und ist gelöscht,
weil ein unbeschränkter Zwilling den nächsten Aufrufer einlädt, A-03 wieder zu öffnen.

Die Delete-Operation hatte gar keine Autorisierungsprüfung.
Sie beginnt jetzt mit `context.requireGlobalAdmin`, derselbe Guard wie in `RbacSubjectController`.
Diese engste Regel gilt, bis die Spezifikation existiert (Hostsharing-intern Task#5637):
eine spätere Erweiterung ist möglich, eine spätere Verengung ist nicht nötig.

### A-04 — bewertet, kein Finding

Das Audit meldete: wenn die Partner-Person wechselt,
verschiebt `HsOfficePartnerController` die Relations mit Bulk-Updates auf die Basistabelle,
und diese Updates umgehen den `INSTEAD OF UPDATE`-Trigger von `relation_rv`.

Diese Beobachtung ist korrekt, und die Updates schreiben jetzt über `relation_rv`.
Die Ex-Partner-Relation wird jetzt ebenfalls über das RBAC-Relation-Repository erzeugt.

Die gemeldete Auswirkung ist nicht korrekt, aus drei unabhängigen Gründen:

1. Das Update selektiert die Zeilen über `anchorUuid`.
   Der Bericht beschreibt eine natürliche Person, die Repräsentant bei mehr als einem Partner ist,
   aber so eine Person ist der *Holder* dieser Relations, das Update selektiert sie also nie.
2. Alle Relations der alten Partner-Person zu verschieben ist der Zweck der Operation, kein Nebeneffekt.
   Die Operation modelliert eine Rechtsnachfolge;
   die `EX_PARTNER`-Relation bleibt als Aufzeichnung des Vorgängers.
3. Ein Kunde kann die Operation nicht erreichen.
   Der Anchor einer PARTNER-Relation ist Hostsharing,
   und nur der `person:ADMIN` des Anchors bekommt `relation:ADMIN`, das `UPDATE` braucht.
   Der Rollen-Graph hält diese Operation also bei den Hostsharing-Mitarbeitern und den Global-Admins.

Für einen unassumed Global-Admin — den einzigen Aufrufer, der die Operation heute erreicht —
ändert sich das Verhalten nicht, daher begleitet kein Test diese Änderung.
Unter einer assumed Rolle unterscheidet sich das Verhalten:
die View überspringt Relations außerhalb der sichtbaren Menge stillschweigend,
eine sichtbare, aber nicht änderbare Relation bricht den ganzen Patch mit `403` ab,
und der Update-Trigger enumeriert die erreichbare Menge mit einer Obergrenze von 8000 Zeilen.
Wie sich die Nachfolge unter assumed Rollen verhalten soll, ist eine offene Frage (siehe unten);
bis zu dieser Entscheidung bleibt die Änderung als Defense-in-Depth.

### Test-Nachweis

Jeder Test, der ein Finding zeigt, wurde *vor* seinem Fix committet.
Die Branch-Historie zeigt den Test also rot ohne den Fix und grün mit ihm.
Jeder Test trägt einen Marker `verifies Fix for Security-Finding <id>`.

- A-01: `HsOfficeRelationRepositoryIntegrationTest` — Verankern an der eigenen Person klappt, an einer fremden nicht.
- A-02: `HsOfficeRelationControllerAcceptanceTest` — POST und PATCH mit einem unsichtbaren Contact;
  `HsOfficePartnerControllerAcceptanceTest` — PATCH der Partner-Person auf eine unsichtbare Person.
- A-03: `HsAccountControllerAcceptanceTest` — Lesen, Auflisten und Löschen eines fremden Accounts.

## Offene Fragen

- [ ] TODO.spec: festlegen, wer eine Partner-Nachfolge (Wechsel der Partner-Person) durchführen darf,
  und das als explizite Prüfung in `patchPartner` verankern statt als Folge der Daten (Hostsharing als Anchor).
  Der naheliegende Kandidat ist: nur ein unassumed Global-Admin.
  Diese Entscheidung machte auch das heutige Zufallsverhalten unter assumed Rollen zum beabsichtigten (siehe A-04):
  ein assumed `partnerRel:ADMIN` scheitert am `EX_PARTNER`-INSERT mit `403`, trotz seines Partner-UPDATE-Grants,
  und die Bulk-Updates würden stilles Überspringen oder den 8000er-Cap zeigen.
- [ ] TODO.spec: festlegen, wer einen Account löschen darf (Hostsharing-intern Task#5637); siehe A-03.
- [ ] TODO.spec: wie eine fremde Person oder ein fremder Contact fachlich korrekt in eine Relation kommt,
  obwohl keine Grant-Verbindung existiert; bis dahin bleibt alles dicht (siehe A-02).

### Constraint: ein Nachfolger darf die Aufzeichnung seines Vorgängers nicht kontrollieren

Die `EX_PARTNER`-Relation hat die **neue** Partner-Person als Anchor, nicht Hostsharing.
Der `person:ADMIN` des Anchors bekommt `relation:OWNER`, und damit `DELETE` und `UPDATE`.
Der Nachfolger kontrolliert also die Aufzeichnung, die beantworten soll, wer der Partner vorher war —
aber die Partei mit einem Interesse an dieser Antwort darf sie nicht kontrollieren.

Heute kann kein Aufrufer das ausnutzen, weil alle administrative Arbeit als Global-Admin läuft.
Der Constraint wird wichtig, wenn schwächer privilegierte Admin-Rollen kommen,
oder wenn Kunden `person:ADMIN` auf ihrer eigenen Person bekommen.
Das ist ein Problem des Rechte-Designs, kein Security-Defekt;
der realistische Schaden ist ein Verlust verlässlicher Partner-Historie.
Aus diesem Grund ist der Constraint hier festgehalten, und dieser PR behebt ihn nicht.

## Zusätzliche Änderungen

Ein Ultra-Review lief über diesen Branch.
Die bestätigten Findings ohne Design-Entscheidung sind in diesem PR umgesetzt;
die Findings, die eine Entscheidung brauchen, stehen unter Offene Fragen und A-04.

- Die Holder-Validierung in `postNewRelation` meldet jetzt `holder, holder.uuid`,
  weil sie die Holder-Felder validierte, aber die Anchor-Felder in der Fehlermeldung nannte.
- Das neue `RFC#0004` (Object-Capabilities-Endpoint) nennt `rbac.isGranted` als Alle-Kanten-Präzedenz,
  weil der A-01-Fix `isPermissionGrantedToSubject` auf die assumed Kanten beschränkte.
- Der `TODO.impl`-Kommentar am `GLOBAL:ADMIN`-INSERT-Grant von `HsBookingItemRbacEntity` ist aufgelöst:
  der Grant erzeugt den `hasGlobalAdminRole()`-Kurzschluss,
  ohne den der Nur-assumed-Lauf unterhalb der unassumed `debitorRel:AGENT`-Kante in einer Sackgasse endet.
- Test-Aufräumarbeiten: der Partner-Acceptance-Test baut seinen Assumed-Role-Header mit `WithRoleId.roleId(ADMIN)`,
  ein toter Relation-Repository-Stub verließ den Partner-REST-Test,
  und das `ForeignContactReference`-Javadoc beschreibt das Vor-Fix-Verhalten nicht mehr als aktuell.
- Kommentare und Member-Reihenfolge folgen auf gemeinsamen Zeilen dem PR#260-Branch, für einen konfliktfreien Merge.
- PR-docs sind ab diesem PR auf Deutsch (`doc/PR/README.md` angepasst):
  die Leserschaft ist deutsch, und Reviews prüfen in der Muttersprache präziser — gerade bei Security-Themen.

## Folge-PRs

- PR#260 schließt die Findings der zweiten Audit-Runde; sein Branch baut auf diesem Branch auf.
- [ ] TODO.spec: den Anchor der `EX_PARTNER`-Relation entscheiden, oder die Aufzeichnung anders schützen (siehe oben).
- Spezifizieren, wer einen Account löschen darf, und die Interims-Global-Admin-Regel ersetzen (Checkbox bei Offene Fragen).
