# PR#260: Autorisierungslücken der zweiten Runde des Backend-Security-Audits in den Modulen Office und Accounts schließen

## Verwandte Links (Hostsharing-intern)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/545
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/260

## Aufteilung der zweiten Audit-Runde

Der ursprüngliche Branch wurde zu groß und ist in drei PRs aufgeteilt:

- PR#260 (dieser PR): Fixes in den Modulen Office und Accounts, dazu ein RBAC-Regressionstest (Java und SQL).
- [PR#264](2026-08-03-PR%23264-security-hardening-tooling-and-dependencies.md):
  Fixes im Tooling, den Dependencies und dem Request-Body-Filter.
- [PR#265](2026-08-03-PR%23265-security-hardening-booking-hosting.md):
  Fixes im Booking/Hosting-Bereich (Java und SQL).

## Das Problem

Das Backend-Security-Audit (Hostsharing-intern) meldete weitere hoch- und mittelhoch priorisierte Befunde,
dazu ein niedrig priorisierter.
Die Befund-Nummern folgen dem Audit-Bericht vom 2026-07-27, wie schon in PR#254;
die zweite Audit-Runde (Bericht vom 2026-07-28) bestätigte die Reste der dort schon behandelten Befunde.

Dieser PR behandelt die Befunde in den Modulen Office (`hs/office`) und Accounts (`hs/accounts`),
dazu den Regressionstest zum RBAC-Befund B-01:

- A-02 (Rest): der Debitor-Schreibpfad akzeptierte eine Relation-UUID vom Client ohne Sichtbarkeitsprüfung.
  Der Hosting-Teil dieses Befunds ist in PR#265.
- A-03 (Rest): der Person-Filter der Account-Liste verriet, ob eine Person existiert.
- A-05: die Repräsentanten-Query der Personen las die Basistabellen.
- B-01: der Subject-Sync-Report akzeptierte jedes authentifizierte Subject.
- C-09: die Coopassets-Antwort verriet UUIDs fremder Transaktionen.

Dieser PR schließt alle diese Befunde außer B-01, der inzwischen über master behoben ist (siehe Lösung).

## Nicht-Ziele

- Die Rollen-Graphen ändern sich nicht, nur die Permission-*Prüfungen* ändern sich.
- Keine neue Autorisierungsschicht:
  jeder Fix nutzt die `_rv`-Restricted-Views oder eine Prüfung, die vergleichbare Endpoints schon haben.
- Die organisationsweite Sichtbarkeit von *Personen* (der andere Teil von A-03) bleibt,
  bis die Sichtbarkeitsregeln spezifiziert sind (Checkboxen im PR#254-Doc).
  Für *Accounts* ist sie mit diesem PR entfallen, siehe A-03 (Rest).

## Hintergrund

Der Hintergrund-Abschnitt des [PR#254-Docs](2026-07-28-PR%23254-security-hardening-authorization-gaps.md)
erklärt die beiden Regeln der `_rv`-Restricted-Views und die beiden Läufe durch den Grant-Graphen.
Die Befunde A-02, A-03 und A-05 folgen demselben Muster wie dort:
ein Code-Pfad erreicht eine Basistabelle direkt und hat damit keine Sichtbarkeitsprüfung.

## Die Lösung

### A-02 (Rest) — ein weiterer Schreibpfad ohne Sichtbarkeitsprüfung

PR#254 schloss diesen Defekt in den Relation- und Partner-Schreibpfaden;
er blieb in `HsOfficeDebitorEntityPatcher.debitorRel`, den erst die zweite Audit-Runde meldete.

`em.getReference` auf `HsOfficeRelationRealEntity` trifft die Basistabelle, der Lazy-Proxy prüft also keine Sichtbarkeit.
Ein Aufrufer mit `debitorRel:ADMIN` konnte einen Debitor auf eine fremde Relation umhängen.

Der Patcher löst die Client-UUID jetzt über `findVisibleByUuid` auf; das Relation-RBAC-Repository bekam die Methode dafür.
Eine Referenz auf eine unsichtbare Zeile ergibt jetzt `404`.

`HsOfficeDebitorEntityPatcher.refundBankAccount` braucht keine Änderung:
`HsOfficeBankAccountEntity` mappt auf `bankaccount_rv`, der Proxy scheitert also schon an unsichtbaren Zeilen.

Der gleichartige Defekt im `alarmContact`-Schreibpfad der Hosting-Assets ist in PR#265 behoben.

### A-03 (Rest) — der Person-Filter der Accounts war ein Existenz-Orakel

PR#254 zog die Sichtbarkeitsbedingung für die Account-Endpoints ein; ein Orakel blieb:
`HsAccountController.findByPersonUuid` prüfte die Person mit `findByUuid`,
und diese Methode liest `hs_office.person` und sieht jede Zeile; nur die Account-Liste selbst war beschränkt.
Für eine existierende, aber unsichtbare Person kam `200` mit leerer Liste,
für eine nicht existierende Person eine Ablehnung —
aus diesem Unterschied konnte ein Aufrufer existierende Person-UUIDs herausfinden.

Die Prüfung nutzt jetzt `findVisibleByUuid`, beide Fälle werden also gleich abgelehnt.
Sie greift erst, wenn kein sichtbarer Account gefunden wurde:
ein Treffer beweist die Berechtigung schon, und der Normalfall spart so die teure `person_rv`-Abfrage.

Der Orakel-Fix legte offen, dass die drei Zugriffswege auf Accounts drei verschiedene
Sichtbarkeitsregeln hatten: die ungefilterte Liste eine personenbasierte,
der Einzel-Lookup und der Person-Filter die organisationsweite `VISIBLE_SUBJECT_CONDITION`,
und der neue Guard die ReBAC-Sichtbarkeit aus `person_rv`.
Daraus folgten zwei Defekte:
ein selbstregistrierter Nutzer konnte nicht nach seiner *eigenen* Person filtern,
und er sah umgekehrt den Account einer *fremden* Person, sobald deren Subject nur dasselbe Namenspräfix trug.

Alle Account-Abfragen nutzen jetzt dieselbe Bedingung `HsAccountRepository.VISIBLE_ACCOUNT_CONDITION`:
Global-Admins sehen alle Accounts, jeder sieht den eigenen und alle Accounts derselben Person
sowie die der von ihm vertretenen Personen,
und darüber hinaus die Accounts, deren Subject *und* deren Person er über die `_rv`-Views sieht.
Der Organisations-Zweig ist dabei entfallen:
ein Account ist kein RBAC-Objekt, und die organisationsweite *Subject*-Sichtbarkeit existiert,
um Kollegen Rollen zu granten — dafür braucht es das Subject, nicht seinen Account
mit Personen-Verknüpfung und globaler uid/gid.

### A-05 — die Repräsentanten-Query las die Basistabellen

`HsOfficePersonRbacRepository.findPersonsRepresentedByPersonWithUuid` las `hs_office.relation` und
`hs_office.person`, und sie prüfte auch nicht, ob die angegebene Person für den Aufrufer sichtbar ist.
`GET /api/hs/office/persons?representedByPersonUuid=…` lieferte so jedem authentifizierten Subject
fremde Personen und ihre Repräsentanten.

Die Query liest jetzt `relation_rv` und `person_rv`.

### B-01 — der Sync-Report akzeptierte jedes Subject (inzwischen über master behoben)

`POST /api/rbac/subjects/sync-report` hatte nur die `isAuthenticated()`-Prüfung der Controller-Klasse.
Jeder Nutzer konnte so einen falschen Monitoring-Heartbeat senden,
und weil jeder Insert die Reports außerhalb des Retention-Fensters löscht,
auch die Historie aller anderen Reporter löschen.

Der Fix — `context.requireGlobalAdmin` — kam zwischenzeitlich mit PR#258 über master herein.
Dieser PR steuert nur noch den Acceptance-Regressionstest bei.

### C-09 — die Adoptions-Antwort gab fremde UUIDs preis

Die `adoptionAssetTx`-Response einer Asset-Übertragung enthielt die UUIDs
ihrer notwendigerweise fremden Membership und Transaktion.
Eine neue, eigene OpenAPI-Resource lässt diese internen UUIDs weg
und behält die Mitgliedsnummer der Gegenseite und den vollständigen Gegenbuchungssatz.
Die Commits dieses Fixes nennen den Befund noch SEC-01.

### Test-Nachweis

Jeder Test, der einen Befund zeigt, wurde *vor* seinem Fix committet.
Die Branch-Historie zeigt den Test also rot ohne den Fix und grün mit ihm.
Ausnahme ist B-01: sein Fix kam über master herein, sein Test war in dieser Historie nie rot.
Jeder Test trägt einen Marker `verifies Fix for Security-Finding <id>`;
die Commit-Messages tragen noch die Befund-Nummern der zweiten Audit-Runde beziehungsweise das SEC-Kürzel.

- A-02: `HsOfficeDebitorControllerAcceptanceTest` — PATCH von `debitorRel` auf eine unsichtbare Relation.
- A-03: `HsAccountControllerAcceptanceTest` — der Person-Filter antwortet für eine unsichtbare
  und für eine unbekannte Person gleich, er akzeptiert aber die eigene Person;
  der Account einer fremden Person derselben Organisation bleibt unsichtbar, auch im `HsAccountRepositoryIntegrationTest`.
- A-05: `HsOfficePersonRbacRepositoryIntegrationTest` — Abfrage der Repräsentanten einer unsichtbaren Person.
- B-01: `RbacSubjectControllerAcceptanceTest` — ein normaler Nutzer sendet einen Sync-Report und scheitert.
- C-09: `HsOfficeCoopAssetsTransactionControllerAcceptanceTest` — die Gegenseite bleibt mit allen
  Buchungsdaten sichtbar, aber ohne Transaktions- und Membership-UUID,
  in beiden Richtungen und auch ohne die UUID ihrer Stornierung.

## Offene Punkte

Aus einem Code-Review dieses Branches, noch offen (Korrektheit vor Aufräumen):

- [x] `HsAccountRepository.VISIBLE_ACCOUNT_CONDITION` verlor die frühere
  `deactivated_at is null`-Bedingung (auf master über den `join rbac.subject` und `VISIBLE_SUBJECT_CONDITION`).
  Der Account eines von Keycloak deaktivierten (soft-deleted) Subjects wurde dadurch über `findVisibleByUuid`
  und den Person-Filter wieder sichtbar — für dieselbe Person, Vertreter und Global-Admins —,
  obwohl die Subject-Seite deaktivierte Subjects „für niemanden sichtbar" hält.
  Behoben: der Guard schließt deaktivierte Subjects wieder aus (Nachweis-Test in `HsAccountRepositoryIntegrationTest`).
- [x] `PATCH /api/hs/office/debitors/{uuid}` liefert jetzt `404` bei einer unsichtbaren
  `debitorRel.uuid`, aber die OpenAPI-Operation dokumentierte nur 200/401/403.
  Behoben: die `404`-Antwort (`NotFound`) in `hs-office-debitors-with-uuid.yaml` ergänzt, analog zur `delete`-Operation.
- [ ] TODO.perf: `VISIBLE_ACCOUNT_CONDITION` materialisiert für jede Nicht-Admin-Listenabfrage zwei
  `_rv`-Views (`subject_rv` und `person_rv`) — das je-Zeile-Muster, das Listenabfragen sonst vermeiden — und
  wertet das anfrage-konstante `rbac.hasGlobalAdminRole()` pro Zeile aus (auf dem `findAllVisible`-Pfad ohnehin tot,
  auf dem Einzel- und Person-Pfad aber live, daher nicht entfernbar).
  Erwägung: eine abwärts gerichtete Rollen-Closure als CTE und `(select rbac.hasGlobalAdminRole())` als InitPlan.
  Nach später verschoben: der werthaltige CTE-Umbau greift in die zentrale Restricted-View-Query-Mechanik ein
  und gehört in den separaten Restricted-View-Query-Refactoring-PR, nicht auf diesen Security-Branch.
- [x] der dreizeilige `findVisibleByUuid`-Kommentar in `HsOfficeRelationRbacRepository` auf eine Zeile gekürzt.
- [x] die drei C-09-Coopassets-Tests via `saveTx`-Helper entdoppelt.
- [x] `final var` → Lombok `val` in den neuen Debitor-, RbacSubject- und Account-Tests.
- [x] die zwei neuen Coopassets-Schemas via `allOf` zusammengeführt (statt sechs wiederholte Felder je Schema).
  Basis `HsOfficeRelatedForeignCoopAssetsTransaction`, beide Schemas via `allOf` plus ihren einen `*.uuid`-Rückverweis
  (Muster wie `IntegerPropertyDescriptor`). Der Generator flacht `allOf` ab, der generierte Resource-Code und die
  Mapper bleiben unverändert; Coopassets-Tests grün.

## Zusätzliche Änderungen

- `findVisibleByUuid` der Relation zog vom Real- ins RBAC-Repository
  (Person und Contact sind schon mit PR#254 umgezogen), weil eine Query auf eine `_rv`-View in die RBAC-Repositories gehört.
- `HsOfficeMembershipController` trägt einen `TODO.impl`-Hinweis:
  seine Partner-Lookups lesen die Basistabelle, was nur harmlos bleibt, solange INSERT allein an `GLOBAL:ADMIN` granted ist.

## Voraussetzungs-PRs

- PR#254 (`mihoe/Taiga545-security-hardening`): inzwischen in master gemerged und in diesen Branch integriert.
- PR#264 und PR#265 sind unabhängig von diesem PR und untereinander; die Merge-Reihenfolge ist frei.
