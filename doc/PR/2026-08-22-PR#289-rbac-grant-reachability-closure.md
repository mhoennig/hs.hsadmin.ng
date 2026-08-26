# PR#289: Grant-Erreichbarkeit als Fixpunkt statt Tiefen- und Mengen-Heuristik

Die Änderung entstand als vorgezogener Commit auf dem Branch von PR#283 und bleibt dort enthalten.

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/289

## Das Problem

Die zentrale Funktion `rbac.queryAccessibleObjectUuidsOfSubjectIds`,
auf der alle generierten Restricted Views und deren Instead-of-Trigger basieren,
traversierte den Grant-Graphen mit `union all` und einer mitgeführten `level`-Spalte.
Zwei Sicherheitsnetze begrenzten die Rekursion:
ein Abbruch ab 22 Grant-Ebenen (`too many grant-levels`)
und eine Obergrenze von 400.000 Zeilen für das Zwischenergebnis.
Beide Netze schlagen auch bei legitimen Daten zu,
denn `union all` traversiert bei Diamant-Strukturen jeden geteilten Teilgraphen pro Pfad erneut,
das Zwischenergebnis wächst also mit der Pfad- statt mit der Knoten-Anzahl.

Die übrigen rekursiven Grant-Funktionen (`rbac.isGranted`, `rbac.findGrantees`, `rbac.queryPermissionsGrantedToSubjectId`,
`rbac.queryAllRbacSubjectsWithPermissionsFor`) traversierten ebenso per `union all`, aber ganz ohne Sicherheitsnetz.

### Befund auf der Dev-Stage (2026-08-22): echter Zyklus im Grant-Graphen

Ein `POST /api/hs/office/relations` hing auf der Dev-Stage 300 Sekunden bis zum Abbruch
und füllte dabei über `pgsql_tmp` die Platte der VM (mehrere Gigabyte Spill in `rbac.isGranted`),
verursacht durch einen echten Zyklus der Länge 4 im Grant-Graphen:

    person#Krause…:ADMIN → relation#Krause…-with-REPRESENTATIVE-…:OWNER
    → relation…:ADMIN → person…:OWNER → person…:ADMIN

Die zyklusschließende Kante entstand kurz zuvor durch einen *erfolgreichen* `POST /api/hs/office/relations`
mit angenommener Rolle `person#…:ADMIN` (laut `base.tx_journal_v`):
der Rollen-Zweig von `rbac.grantRoleToCurrentContext` prüfte vor dem Kontext-Grant nur auf Duplikat, nicht auf Zyklus —
und bei einer als Anker-`person:ADMIN` angelegten `REPRESENTATIVE`-Relation schließt die Kontext-Kante
`person:ADMIN → relation:OWNER` den Kreis, denn der Spec-Grant gewährt `relation:ADMIN` bereits den Anker-`person:OWNER`.
Der Zyklus ist zugleich eine Rechteausweitung (`person:ADMIN` erreicht transitiv `person:OWNER`)
und erklärt mutmaßlich auch den ursprünglich beobachteten Fehler `too many grant-levels: 22`.

## Nicht-Ziele

- Keine Zyklen-Diagnose in den Leseabfragen:
  der `union`-Fixpunkt terminiert auf Zyklen, meldet sie aber nicht; die Zyklen-Verhinderung liegt im Schreibpfad.
- Keine Änderung der `REPRESENTATIVE`-Spec: dass die vertretene Person ihre Vertretung nicht selbst widerrufen kann,
  folgt aus der Rückwärtskante `relation:ADMIN → anchorPerson:OWNER` und bleibt eine eigene Spec-Entscheidung.

## Die Lösung

Alle rekursiven Grant-Traversierungen berechnen die Erreichbarkeitsmenge jetzt als Fixpunkt
mit `union` statt `union all`, nach dem Vorbild der `reachable_role`-CTE in `rbac.grants_gv`:
jeder erreichbare Knoten wird genau einmal besucht und die Rekursion terminiert selbst auf zyklischen Graphen.
Der Fixpunkt ist zugleich schneller, denn der Aufwand skaliert mit der Knoten- statt mit der Pfad-Anzahl:
auf dem am stärksten vernetzten Knoten des Dev-Graphen misst `explain analyze` 5,6 ms statt 19,0 ms,
bei 1.322 statt 12.887 materialisierten Zwischenzeilen; kleine, flache Teilgraphen bleiben gleichauf.
In `rbac.queryAccessibleObjectUuidsOfSubjectIds` entfallen damit beide Sicherheitsnetze ersatzlos;
die Obergrenze `maxObjects` (Fehler `[400] Too many accessible objects`) bleibt bestehen.
`rbac.isGranted` und `rbac.findGrantees` stammen aus dem gemischten, nicht-wiederholbaren `rbac-base-GRANTS`-Changeset
(mit `create table`) und sind deshalb über ein neues `runOnChange`-Changeset dahinter ersetzt;
die reinen Funktions-Changesets der beiden Query-Funktionen wurden dagegen selbst `runOnChange` und in-place umgestellt.
Die *released* generierten `_rv`-Views enthalten die Rekursion samt beider Netze noch inline
(sichtbar in den `released-prod-schema-*`-Fixtures, die als Schnappschuss unverändert bleiben);
ihre `runOnChange`-Changesets erzeugen sie beim Deploy neu, wonach sie die zentrale Funktion aufrufen —
auf dev verifiziert: kein Datenbank-Objekt enthält den 400.000er-Check mehr.
`RbacGrantGraphCycleIntegrationTest` belegt per Red-Green-Nachweis,
dass alle vier auf einem künstlich zyklischen Graphen terminieren statt in den Statement-Timeout zu laufen.

Der Rollen-Zweig von `rbac.grantRoleToCurrentContext` vergibt gar keine OWNER-Rolle mehr:
Ein Erzeuger-Kontext mit angenommener Rolle erhält stattdessen die direkten
SELECT/UPDATE/DELETE-Permissions des neuen Objekts (Erzeuger-Permissions-Bündel).
Permissions sind Senken im Grant-Graphen, Erzeuger-Zugriff kann so konstruktiv keinen Zyklus mehr schließen —
auch nicht in der zweiten gefundenen Konstellation,
in der die Holder-Person einer REPRESENTATIVE-Relation zuvor vom Anker-Kontext selbst angelegt wurde
und deren Anlage deshalb mit „Duplicate role grant detected“ scheiterte.
Permissions, die der Kontext ohnehin erreicht (z. B. als `rbac.global#global:ADMIN`), werden übersprungen;
Subjekt-Kontexte erhalten unverändert die OWNER-Rolle, denn Subjekt-Grants sind zyklusfrei.
Red-Green-Nachweise:
`creatingARepresentativeRelationAsAnchorPersonAdmin_doesNotCreateAGrantCycle` (kein OWNER-Grant an den Kontext)
und `creatingARepresentativeRelationForAHolderPersonCreatedByTheSameContext_doesNotCloseAGrantCycle`
(vormals „Duplicate role grant detected“, jetzt erfolgreich, azyklisch und mit Erzeuger-Permissions).

Die zyklusschließende Kante auf der Dev-Stage wurde am 2026-08-22 manuell entfernt
(`delete from rbac.grant where uuid = '5728f363-…'`, mit `base.defineContext(...)` im Journal nachvollziehbar);
der Graph ist wieder azyklisch, die beiden übrigen Kontext-Kanten desselben Musters schließen keinen Zyklus.

---

## Release Note

released: 2026-08-22

### Deep role hierarchies no longer fail with "too many grant-levels"

Since 2026-08-22, the reachability queries behind all restricted views and grant checks
compute a set-based closure instead of enumerating every path through the role graph;
the former guard errors `too many grant-levels` and its row-count companion are gone.
Also since then, creating an object under an assumed role no longer grants the new object's OWNER role
to that role; the creating role gets the object's SELECT/UPDATE/DELETE permissions instead.
This keeps the grant graph free of privilege-escalating cycles,
for example when the anchor-person's ADMIN first creates a person and then makes it a REPRESENTATIVE,
which formerly failed with "Duplicate role grant detected".
