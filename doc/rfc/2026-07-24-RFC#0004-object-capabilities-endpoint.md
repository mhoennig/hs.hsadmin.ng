# RFC#0004: Object-Capabilities-Endpoint — welche Operationen darf ein Subject an einer Object-UUID?

- Status: Entwurf
- Stand: 2026-07-24
- Author: Michael Hönnig

Ein Endpoint, der zu einer gegebenen Object-UUID die Menge der Operationen liefert, die ein
Subject an genau diesem Objekt ausführen darf (SELECT/UPDATE/DELETE). Motiviert durch die
Frontend-Frage „welche Aktionen — Bearbeiten, Löschen — biete ich für diese Zeile an?" und die
Kontrollfrage „würde mein Speichern/Löschen überhaupt durchgehen?". Aus der Design-Diskussion
rund um Freitextsuche und ReBAC-Erreichbarkeit hervorgegangen; hier auf den Capabilities-Aspekt
fokussiert.

## Ausgangslage

Die Zugriffskontrolle steckt in den `*_rv`-Restricted-Views: sie erlauben SELECT/UPDATE/DELETE
nur für Zeilen, die der aktuelle Kontext erreichen darf. Durchgesetzt wird das über
`rbac.queryAccessibleObjectUuidsOfSubjectIds(<op>, <table>, rbac.currentSubjectOrAssumedRolesUuids())`.

Es gibt bereits `GET /api/rbac/subjects/{subjectUuid}/permissions`. Der liefert aber die
**gesamte** Permission-Menge eines Subjects über *alle* Objekte, gestützt auf
`rbac.queryPermissionsGrantedToSubjectId` — also **subjekt-seeded** (die volle Grant-Hülle nach
unten) und **all-edges** (ohne `assumed`-Filter). Auf ein einzelnes Objekt filtern könnte man
nachgelagert, das rechnet aber erst alles aus.

Zwei Erreichbarkeits-Primitiven mit unterschiedlicher Semantik sind zentral:

- `rbac.isGranted` / `queryPermissionsGrantedToSubjectId` folgen **allen** Grant-Kanten
  (ignorieren `assumed`).
- `queryAccessibleObjectUuidsOfSubjectIds` folgt **nur `assumed=true`**-Kanten — das ist die
  tatsächliche Durchsetzung in den `_rv`-Triggern.

Diese Unterscheidung ist nicht akademisch: eine Kante kann `assumed=false` sein, sodass die
Berechtigung zwar besteht, aber erst nach **explizitem** Assumen der zugehörigen Rolle greift.

## Motivation / Zielbild

- Eine **billige** per-Objekt-Abfrage statt der teuren subjekt-weiten Enumeration.
- Klare Antwort für die UI: welche Zeilen-Aktionen (Bearbeiten/Löschen) einzublenden sind.
- Bewusste, dokumentierte Entscheidung zur Semantik (Capability vs. „jetzt ausübbar"), weil
  genau daran der geplante Global-Auditor-Fall hängt.

## Entwurfsentscheidungen

### Endpoint als Sub-Resource, Antwort = Op-Liste

```
GET /api/rbac/subjects/{subjectUuid}/permissions/{objectUuid}
→ 200 ["SELECT", "UPDATE"]
```

Kleineres DTO als der volle `RbacSubjectPermission`-Report — der Client braucht für Button-Logik
nur die Op-Strings. Autorisierung wie beim bestehenden Endpoint (self, oder global-admin für
fremde Subjects). Ohne die `{objectUuid}`-Sub-Resource bleibt der bisherige subjekt-weite Report
unverändert.

### Objekt-seeded statt subjekt-seeded (Kosten)

Der entscheidende Kostenhebel ist die Seed-Richtung. Der bestehende Pfad ist subjekt-seeded und
rechnet die komplette Permission-Hülle des Subjects aus — bei global-admin praktisch die ganze
DB. Für „nur dieses Objekt" dreht man die Richtung um: eine Object-UUID hat nur eine Handvoll
Permissions (SELECT/UPDATE/DELETE, ggf. INSERT), von denen läuft man **nach oben** und prüft, ob
das Subject erreicht wird.

```sql
with recursive up(perm, node) as (
    select uuid, uuid from rbac.permission where objectuuid = :objUuid   -- Seed: ≤ 4 Zeilen
  union
    select up.perm, g.ascendantuuid
      from rbac.grant g join up on g.descendantuuid = up.node            -- all-edges (kein g.assumed)
)
select distinct p.op, p.optablename
  from up join rbac.permission p on p.uuid = up.perm
  where up.node = :subjectUuid;
```

Denselben Aufwärts-Walk pro Permission macht bereits `rbac.isPermissionGrantedToSubject(permissionId, subjectId)`,
seit dem A-01-Fix allerdings **assumed-gefiltert** (`where g.assumed`) — für die all-edges-Semantik
taugt es daher nicht als Basis; das all-edges-Vorbild ist `rbac.isGranted`.
Die Batch-Query oben ist die zusammengefasste all-edges-Variante. Als
neue DB-Primitive vorgeschlagen: `rbac.permissionsOnObject(objUuid, subjectUuid)` → set of op.

### Global-admin-Shortcut ohne Rekursion

Hält das Subject die global-admin-Rolle (`rbac.hasGlobalRoleGranted`), erreicht es jede
Permission — dann gibt es nichts zu traversieren:

```sql
select op, optablename from rbac.permission where objectuuid = :objUuid;
```

Erst `hasGlobalRoleGranted` prüfen → alle definierten Objekt-Ops zurück; sonst der objekt-seedete
Walk. Derselbe Sonderfall, den Enforcement und `grantedPermissionsRaw` bereits kennen.

### Semantik: all-edges („Capability") — mit ausdrücklicher Grenze

Default-Semantik ist **all-edges**: das Subject *hält* die Permission über irgendeine Rolle; ob
es sie durch Assumen aktiviert, ist seine freie Entscheidung. all-edges zeigt also die volle
Handlungsfähigkeit — das ist bewusst mehr als „was geht ohne alles". Es ist **keine**
Impersonation (siehe Abgrenzung).

**Grenze / Wider:** die Durchsetzung ist `assumed`-gefiltert. Eine all-edges-Op kann daher eine
sein, deren Kante `assumed=false` ist — ausübbar erst nach **explizitem** Assumen der zugehörigen
Rolle. Damit die UI nicht Buttons zeigt, die im Default-Kontext ins Leere laufen, trägt jede
all-edges-Antwort optional den `roleName`, über den die Op erreichbar ist („zum Aktivieren Rolle
X assumen").

### assumed-gefilterte Variante („jetzt sofort")

Der geplante **Global Auditor** (global, aber nur-lesen; global-admin nur mit `assumed=false`
gegrantet, damit Schreiben eine explizite Eskalation ist) macht die Unterscheidung
sicherheitsrelevant: ein Auditor mit latentem global-admin darf `DELETE` nicht als aktiv
angezeigt bekommen. Deshalb zwei Sichten:

- **assumed-gefiltert** (`where g.assumed`, gegen `currentSubjectOrAssumedRolesUuids()`): „das
  darf ich *jetzt sofort*" — deckt sich exakt mit dem, was ein Schreib-Trigger zulassen würde.
- **all-edges** (+ `roleName`): „das *könnte* ich — ggf. nach Assumen von Rolle X".

Empfehlung: der Endpoint liefert per Default die **assumed-gefilterte** (sichere) Sicht; die
all-edges-Sicht ist per `?includeAssumable=true` zuschaltbar und markiert jeden Zusatz-Treffer
mit seinem `roleName`. So passt derselbe Endpoint für normale User (Capability-Sicht erwünscht)
wie für den Auditor-Fall (Default darf nicht über-melden).

### INSERT-Permissions gesondert behandeln

INSERT ist strukturell etwas anderes als die row-lifecycle-Ops SELECT/UPDATE/DELETE:

- SELECT/UPDATE/DELETE betreffen *diese Zeile* — eindimensional, ein Op-String genügt.
- INSERT ist keine Permission an der Zeile, sondern „darf ich ein **Kind** eines bestimmten
  *Zieltyps* unter diesem Objekt anlegen". Sie trägt deshalb ein zweites Feld `opTableName`, und
  ein Objekt kann mehrere INSERT-Permissions für verschiedene Zieltabellen haben (z. B. am
  Debitor-Rel: INSERT auf `hs_office.sepamandate`).

Ein bares `"INSERT"` in der flachen Liste wäre mehrdeutig (verschweigt *was*). Daher: der Default
liefert nur `[SELECT, UPDATE, DELETE]`; INSERT nur getrennt und **immer mit `tableName`**, z. B.
per `?includeCreatable=true` als `[{ "op":"INSERT", "tableName":"hs_office.sepamandate" }]` oder
als eigene Sub-Resource `…/{objectUuid}/creatable`. Zusätzlich: das Anlegen von **Top-Level**-
Entitäten (person, contact) hängt an `global:ADMIN`, nicht an einer Business-Object-UUID — es
gehört ohnehin nicht in eine per-Objekt-Op-Liste.

### Impersonation ist abgegrenzt, nicht Teil dieses RFC

Assume ≠ Impersonation. Assumen ist Selbst-Beschränkung bzw. -Eskalation innerhalb der Rollen,
die man *selbst* hält; die Permission besteht ohnehin. Impersonation hieße „als *anderes* Subject
handeln" — dann hält man die Permission gerade **nicht** und bekäme sie über diesen Endpoint auch
nicht. Impersonation müsste ein eigener Mechanismus mit eigener Autorisierung sein und ist hier
ausdrücklich nicht abgedeckt.

## Kostenanalyse

| Weg | Aufwand | global-admin |
|---|---|---|
| Objekt-seeded (dieser Entwurf) | Index-Lookup `rbac.permission(objectuuid)` (≤ 4 Zeilen) + kleiner Aufwärts-Walk je Permission, begrenzt durch Rollen-Pyramide + externe Grants *über* dem Objekt | ~ms, **unabhängig** von der Mächtigkeit des Subjects (Shortcut ohne Rekursion) |
| Subjekt-seeded (Enumerate-all filtern) | volle Permission-Hülle des Subjects, danach filtern | teuer, skaliert mit Subject-Mächtigkeit (bei global-admin ~ganze DB) |

Ein Volltext-/Trigram-Index ist nicht nötig; `rbac.permission.objectuuid` ist ein normaler
Index-Zugriff, der rekursive Teil bleibt auf den kleinen Teilgraphen oberhalb des Objekts
beschränkt.

### Verworfen: den subjekt-weiten `grantedPermissions` filtern

Naheliegend („objectUuid als `WHERE` anhängen"), aber genau der teure Pfad: er berechnet erst die
komplette Permission-Hülle des Subjects und filtert dann. Für mächtige Subjects unbrauchbar.

### Verworfen: INSERT flach in die Op-Liste

Verliert die Zieltabelle und ist damit irreführend. INSERT bleibt getrennt und mit `tableName`.

### Verworfen: reine assumed-Sicht ohne all-edges-Option

Wäre enforcement-genau, verschweigt aber dem User seine tatsächlichen Möglichkeiten (was er nach
Assumen dürfte). Die all-edges-Sicht bleibt als Opt-in erhalten, mit `roleName`-Markierung.

## Nicht-Ziele / Follow-Ups

- Keine Änderung an der tatsächlichen Schreib-Durchsetzung; die `_rv`-Trigger bleiben
  `assumed`-gefiltert. Dieser Endpoint ist rein informativ.
- Die Modellierung der **Global-Auditor**-Rolle selbst (jede Entität bekäme
  `incomingSuperRole(GLOBAL, AUDITOR)` an ihrer REFERRER-Rolle) ist ein eigenes Vorhaben; dieses
  RFC setzt sie nur als Motivation für die assumed-Unterscheidung voraus.
- Die Freitextsuche (eigene Diskussion) teilt sich mit diesem Entwurf die objekt-seedete
  Erreichbarkeits-Idee, ist aber nicht Teil dieses RFC.
- Neue DB-Primitive `rbac.permissionsOnObject(objUuid, subjectUuid, assumedOnly boolean)` als
  Rückgrat; Signatur/Feinschliff offen.

## Begriffsabgrenzung

- **all-edges** vs. **assumed-gefiltert**: all-edges folgt allen Grant-Kanten (volle Capability);
  assumed-gefiltert nur `assumed=true`-Kanten (was ohne Assumen jetzt gilt, = Enforcement).
- **Capability** („könnte ich, ggf. nach Assumen") vs. **jetzt ausübbar** (ohne etwas zu
  assumen).
- **Assume** (Selbst-Eskalation innerhalb eigener Rollen) vs. **Impersonation** (als anderes
  Subject handeln — hier ausgeschlossen).
- **row-lifecycle** (SELECT/UPDATE/DELETE an der Zeile) vs. **child-creation** (INSERT, per
  `opTableName` an ein Elternobjekt gebunden).
