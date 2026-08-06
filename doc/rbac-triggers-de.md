# RBAC-Trigger auf Tabellen und Restricted Views

Diese Übersicht beschreibt **generisch**, welche Trigger das RBAC-System an einer
RBAC-verwalteten Tabelle und an deren Restricted View anlegt — also nach Art, nicht je konkreter
Tabelle. Sie ist als Nachschlagewerk für Reviews und Sicherheitsbetrachtungen gedacht.

## Platzhalter

| Platzhalter | Bedeutung | Beispiel |
|---|---|---|
| `T` | qualifizierte RBAC-Basistabelle | `hs_office.relation` |
| `t` | deren Kurzname ohne Schema | `relation` |
| `T_rv` | die zugehörige Restricted View | `hs_office.relation_rv` |
| `S` / `s` | eine Sub-Entität, deren INSERT-Permission an einer Rolle von `T` hängt | `hs_office.debitor` / `debitor` |

## Trigger auf der Basistabelle `T`

| Trigger | Timing / Event | Trigger-Funktion | Erzeugt durch | Zweck |
|---|---|---|---|---|
| `createRbacObjectFor_t_insert_tg_1058_25` | BEFORE INSERT | `rbac.insert_related_object()` | `rbac.generateRelatedRbacObject('T')` | legt die `rbac.object`-Zeile an und setzt `NEW.uuid`, falls nicht vorgegeben |
| `t_insert_permission_check_tg` ¹ | BEFORE INSERT | `T_insert_permission_check_tf()` bzw. `T_insert_permission_missing_tf()` | `InsertTriggerGenerator` | **setzt die INSERT-Berechtigung durch**; einzige Prüfstelle für INSERT |
| `build_rbac_system_after_insert_tg` | AFTER INSERT | `T_build_rbac_system_after_insert_tf()` | `RolesGrantsAndPermissionsGenerator` | baut Rollen und Grants der neuen Zeile auf |
| `update_rbac_system_after_update_tg` ² | AFTER UPDATE | `T_update_rbac_system_after_update_tf()` | `RolesGrantsAndPermissionsGenerator` | verdrahtet Rollen und Grants neu, wenn sich referenzierende Spalten ändern |
| `s_z_grants_after_insert_tg` ³ | AFTER INSERT | `S_grants_insert_to_t_tf()` | `InsertTriggerGenerator` **der Sub-Entität `S`** | erteilt der neuen `T`-Zeile die INSERT-Permission auf `S` |
| `tx_0_journal_tg` ⁴ | AFTER INSERT/UPDATE/DELETE | `base.tx_journal_trigger()` | `base.create_journal('T')` | schreibt das Audit-Journal |
| `createRbacObjectFor_t_delete_tg_1058_35` | AFTER DELETE | `rbac.delete_related_rbac_rules_tf()` | `rbac.generateRelatedRbacObject('T')` | räumt Objekt, Rollen und Grants ab |

Alle Trigger sind `for each row`.

¹ Entfällt vollständig, wenn INSERT an `rbac.global GUEST` erteilt ist, also jeder einfügen darf.
  Ist überhaupt keine INSERT-Permission definiert, entsteht die Variante
  `T_insert_permission_missing_tf()`, die unabhängig vom Subjekt `[403]` wirft.

² Nur, wenn mindestens ein Entity-Alias als updatable deklariert ist. Real betrifft das derzeit
  `hs_office.relation`, `hs_office.partner` und `hs_hosting.asset`.

³ Ein Trigger je Sub-Entität. **Der Trigger liegt auf `T`, trägt aber den Namen von `S` und steht
  im Changelog von `S`.** Wer die Trigger einer Tabelle sucht, findet diese daher nicht in deren
  eigenem Changelog.

⁴ Wird pro Tabelle ausdrücklich per `call base.create_journal('T')` aktiviert, ist also nicht
  automatisch vorhanden.

## Trigger auf der Restricted View `T_rv`

| Trigger | Timing / Event | Trigger-Funktion | Bedingung | Geprüfte Permission |
|---|---|---|---|---|
| `instead_of_insert_tg` | INSTEAD OF INSERT | `T_instead_of_insert_tf()` | immer | **keine**; schreibt nur nach `T` durch |
| `instead_of_update_tg` | INSTEAD OF UPDATE | `T_instead_of_update_tf()` | nur mit `columnUpdates` | `UPDATE` über `rbac.queryAccessibleObjectUuidsOfSubjectIds` |
| `instead_of_delete_tg` | INSTEAD OF DELETE | `T_instead_of_delete_tf()` | immer | `DELETE` über `rbac.queryAccessibleObjectUuidsOfSubjectIds` |

Wird `generateRbacRestrictedView` ohne `columnUpdates` aufgerufen, entsteht kein
Update-Trigger. Die View ist dann nicht updatable, und ein `update` darauf scheitert.

`SELECT` ist bewusst **kein** Trigger, sondern steckt im Rumpf der View: `T_rv` ist definiert als
`select * from rbac.select_t_rv()`, und diese Funktion filtert über
`rbac.queryAccessibleObjectUuidsOfSubjectIds('SELECT', …)`. Für Global-Admins entfällt der Filter.

## Durchsetzung je Operation

Diese Matrix ist der sicherheitsrelevante Kern der beiden Tabellen oben:

| Operation | über `T_rv` | direkt auf `T` | durchgesetzt durch |
|---|---|---|---|
| SELECT | eingeschränkt | **uneingeschränkt** | View-Rumpf `rbac.select_t_rv()` |
| INSERT | geprüft | **geprüft** | `t_insert_permission_check_tg` auf `T` |
| UPDATE | geprüft | **ungeprüft** | `instead_of_update_tg` auf `T_rv` |
| DELETE | geprüft | **ungeprüft** | `instead_of_delete_tg` auf `T_rv` |

Daraus folgen zwei Regeln für den Anwendungscode:

- Jeder Lesezugriff, der Zeilen einschränken soll, muss über `T_rv` laufen. Eine Abfrage auf `T`
  liefert alle Zeilen, unabhängig vom aktuellen Subjekt.
- Jeder Schreibzugriff außer INSERT muss über `T_rv` laufen. Ein `update` oder `delete` direkt auf
  `T` umgeht die Prüfung vollständig.

INSERT ist die einzige Operation, die auch bei direktem Tabellenzugriff geprüft bleibt, weil ihr
Trigger auf der Basistabelle sitzt und nicht auf der View.

Die Trigger `build_rbac_system_after_insert_tg`, `update_rbac_system_after_update_tg`,
`s_z_grants_after_insert_tg` und `createRbacObjectFor_t_delete_tg_1058_35` prüfen **keine**
Berechtigung. Sie pflegen ausschließlich die Rollen- und Grant-Struktur.

## Ausführungsreihenfolge

PostgreSQL führt Trigger derselben Timing- und Event-Klasse in alphabetischer Reihenfolge ihrer
Namen aus. Zwei Namensbestandteile nutzen das absichtlich aus:

- `tx_0_journal_tg` — die `0` sortiert den Journal-Trigger vor andere Trigger, deren Name mit
  `tx` beginnt.
- `s_z_grants_after_insert_tg` — das `_z_` soll den Trigger hinter
  `build_rbac_system_after_insert_tg` einsortieren, damit die Rollen der neuen Zeile bereits
  existieren, wenn die Permission erteilt wird.

### Latente Namensfalle

Die Wirkung von `_z_` hängt allein am Namen der Sub-Entität. Sie greift nur, solange dieser Name
alphabetisch hinter `build_…` liegt. Derzeit ist das bei allen Sub-Entitäten der Fall
(`coopassettx`, `coopsharetx`, `item`, `partner`, `project`, `relation`, `sepamandate`).

Eine künftige Sub-Entität mit einem Namen wie `account` oder `asset` würde vor
`build_rbac_system_after_insert_tg` ausgeführt und damit auf noch nicht existierende Rollen
grantieren. Der Kommentar im Generator sagt eine Garantie zu, die der Name allein nicht gibt.

## Quellen im Repository

| Gegenstand | Datei |
|---|---|
| Objekt- und Löschtrigger, Restricted-View-Generator | `src/main/resources/db/changelog/1-rbac/1058-rbac-generators.sql` |
| Trigger-Funktionen der Basis, `delete_grants_of_role_tg` | `src/main/resources/db/changelog/1-rbac/1050-rbac-base.sql` |
| Audit-Journal | `src/main/resources/db/changelog/0-base/020-audit-log.sql` |
| INSERT-Grants und INSERT-Prüfung | `src/main/java/…/rbac/generator/InsertTriggerGenerator.java` |
| Aufbau und Nachführung der Rollen-/Grant-Struktur | `src/main/java/…/rbac/generator/RolesGrantsAndPermissionsGenerator.java` |
