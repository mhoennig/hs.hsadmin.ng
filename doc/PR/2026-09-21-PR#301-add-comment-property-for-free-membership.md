# PR#301: Kommentar zur Mitgliedsbeitragsfreiheit

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/301
- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/590

## Das Problem

Eine Mitgliedschaft mit `membershipFeeBillable: false` ist beitragsfrei.
Die Mitgliedschaft nennt die Begründung dafür derzeit nicht.
Wer die Beitragsfreiheit später prüft, muss die Begründung außerhalb von hsadmin-ng suchen.

## Nicht-Ziele

- Keine Änderung an `membershipFeeBillable`.
- Keine Historie der Kommentare.
- Keine Pflicht in der Datenbank, denn die Migration kann für bestehende Mitgliedschaften keine sinnvollen Kommentare liefern.

## Die Szenarien

### Feature: Beitragsfreiheit einer Mitgliedschaft begründen

Anmerkung zu den Links zu den Scenario-Reports:
Die Links zu den `master`-Reports funktionieren erst nach dem Merge auf master mit dem nächsten `release`-Build.
Die Links zu den branch-spezifischen Reports funktionieren nur, bis der Branch nach dem merge gelöscht wird.

#### Scenario#301.01: Die Mitgliederverwaltung legt eine beitragsfreie Mitgliedschaft mit Kommentar an

&hellip; damit die Begründung der Beitragsfreiheit an der Mitgliedschaft steht.

- **Given** ein Partner ohne Mitgliedschaft
- **When** eine Mitgliedschaft mit `membershipFeeBillable: false` und einem Kommentar angelegt wird
- **Then** liefert `GET /api/hs/office/memberships/{uuid}` den Kommentar

##### Verified by

- [HsOfficeScenarioTests.shouldCreateFeeFreeMembershipWithComment](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [CreateMembership](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/CreateMembership.java)
- Scenario-Reports “4020: Create Fee Free Membership with Comment”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4020%3A%20Create%20Fee%20Free%20Membership%20with%20Comment.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4020%3A%20Create%20Fee%20Free%20Membership%20with%20Comment.html)

#### Scenario#301.02: Die Mitgliederverwaltung stellt eine Mitgliedschaft nachträglich beitragsfrei

&hellip; damit eine spätere Entscheidung über die Beitragsfreiheit nachvollziehbar bleibt.

- **Given** eine beitragspflichtige Mitgliedschaft
- **When** die Mitgliedschaft per `PATCH` auf `membershipFeeBillable: false` mit einem Kommentar gesetzt wird
- **Then** liefert `GET /api/hs/office/memberships/{uuid}` den Kommentar

##### Verified by

- [HsOfficeScenarioTests.shouldExemptMembershipFromFee](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ChangeMembershipFeeExemption](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/ChangeMembershipFeeExemption.java)
- Scenario-Reports “4030: Exempt Membership From Fee”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4030%3A%20Exempt%20Membership%20From%20Fee.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4030%3A%20Exempt%20Membership%20From%20Fee.html)

#### Scenario#301.03: Die API lehnt das Anlegen einer beitragsfreien Mitgliedschaft ohne Kommentar ab

&hellip; damit keine beitragsfreie Mitgliedschaft ohne Begründung entsteht.

- **Given** ein Partner ohne Mitgliedschaft
- **When** ein `POST` eine Mitgliedschaft mit `membershipFeeBillable: false` aber ohne `membershipFeeComment` anlegt
- **Then** antwortet die API mit `400 Bad Request`

##### Verified by

- [HsOfficeScenarioTests.shouldRejectFeeFreeMembershipWithoutComment](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [CreateMembership](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/CreateMembership.java)
- Scenario-Reports “4010: Reject Fee Free Membership Without Comment”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4010%3A%20Reject%20Fee%20Free%20Membership%20Without%20Comment.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4010%3A%20Reject%20Fee%20Free%20Membership%20Without%20Comment.html)

#### Scenario#301.04: Die API lehnt die Beitragsbefreiung einer Mitgliedschaft ohne Kommentar ab

&hellip; damit keine Mitgliedschaft ohne Begründung beitragsfrei wird.

- **Given** eine beitragspflichtige Mitgliedschaft
- **When** ein `PATCH` die Mitgliedschaft auf `membershipFeeBillable: false` aber ohne `membershipFeeComment` setzt
- **Then** antwortet die API mit `400 Bad Request`

##### Verified by

- [HsOfficeScenarioTests.shouldRejectFeeExemptionWithoutComment](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ChangeMembershipFeeExemption](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/ChangeMembershipFeeExemption.java)
- Scenario-Reports “4025: Reject Fee Exemption Without Comment”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4025%3A%20Reject%20Fee%20Exemption%20Without%20Comment.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4025%3A%20Reject%20Fee%20Exemption%20Without%20Comment.html)

#### Scenario#301.05: Die API lehnt das Anlegen einer beitragspflichtigen Mitgliedschaft mit Kommentar ab

&hellip; damit keine Begründung für eine beitragspflichtige Mitgliedschaft entsteht.

- **Given** ein Partner ohne Mitgliedschaft
- **When** ein `POST` eine Mitgliedschaft mit `membershipFeeBillable: true` und `membershipFeeComment` anlegt
- **Then** antwortet die API mit `400 Bad Request`

##### Verified by

- [HsOfficeScenarioTests.shouldRejectFeeBillableMembershipWithComment](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [CreateMembership](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/CreateMembership.java)
- Scenario-Reports “4015: Reject Fee Billable Membership with Comment”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4015%3A%20Reject%20Fee%20Billable%20Membership%20with%20Comment.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4015%3A%20Reject%20Fee%20Billable%20Membership%20with%20Comment.html)

#### Scenario#301.06: Die API lehnt einen Kommentar beim Ändern einer beitragspflichtigen Mitgliedschaft ab

&hellip; damit keine veraltete Begründung für eine beitragspflichtige Mitgliedschaft steht.

- **Given** eine beitragsfreie Mitgliedschaft mit Kommentar
- **When** ein `PATCH` zu `membershipFeeBillable: true` mit `membershipFeeComment` führt
- **Then** antwortet die API mit `400 Bad Request`

##### Verified by

- [HsOfficeScenarioTests.shouldRejectCommentOnFeeBillableMembership](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/HsOfficeScenarioTests.java)
  using the use-case [ChangeMembershipFeeExemption](../../src/test/java/net/hostsharing/hsadminng/hs/office/scenarios/membership/ChangeMembershipFeeExemption.java)
- Scenario-Reports “4040: Reject Comment on Fee-Billable Membership”: [Branch](https://vm4006.hostsharing.net/branches/mihoe_add-comment-property-for-free-membership/reports/scenarios/4040%3A%20Reject%20Comment%20on%20Fee%20Billable%20Membership.html), [`master`](https://vm4006.hostsharing.net/branches/master_release/reports/scenarios/4040%3A%20Reject%20Comment%20on%20Fee%20Billable%20Membership.html)

## Die Lösung

- `hs_office.membership` erhält die optionale Textspalte `membershipFeeComment`, änderbar laut RBAC-Definition.
- `HsOfficeMembership`, `HsOfficeMembershipInsert` und `HsOfficeMembershipPatch` erhalten
  das optionale Property `membershipFeeComment`.
- `HsOfficeMembershipEntity` und `HsOfficeMembershipEntityPatcher` übernehmen das Property.
- Nur `HsOfficeMembershipController` validiert, ein leerer Kommentar zählt als fehlend.
- `PATCH` validiert nur, wenn der Request `membershipFeeBillable` oder `membershipFeeComment` enthält.
  Damit bleibt eine bestehende beitragsfreie Mitgliedschaft ohne Kommentar änderbar, zum Beispiel für die Kündigung.
- Ein `PATCH` auf `membershipFeeBillable: true` muss `membershipFeeComment: null` mitgeben.
- `CreateMembership` nimmt den erwarteten HTTP-Status entgegen und gibt dem `POST` den Titel „Create the Membership“.
- `GenerateDemoDataset` gibt beitragsfreien Mitgliedschaften einen Kommentar.

---

## Release Note

released: 2026-09-23

### Memberships carry a comment for the fee exemption

The memberships API now has the property `membershipFeeComment`.
`POST` and `PATCH` of `/api/hs/office/memberships` answer `400 Bad Request`,
if `membershipFeeBillable` is `false` without a `membershipFeeComment`, or `true` with one.
To make a membership fee-billable again, send `"membershipFeeComment": null` in the same `PATCH`.
