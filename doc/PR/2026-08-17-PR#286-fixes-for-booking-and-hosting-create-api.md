# Fixes für das Anlegen von Booking-Items und Hosting-Assets per API

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/286

## Das Problem

Beim Anlegen von Booking-Items und Hosting-Assets über den Frontend-Prototypen zeigten sich mehrere Fehler in der REST-API:

- Das `HsHostingAssetInsert`-Schema hatte kein Feld `assignedToAsset.uuid`.
  Manche Asset-Typen brauchen aber immer eine Zuweisung:
  `DOMAIN_DNS_SETUP`, `DOMAIN_HTTP_SETUP`, `DOMAIN_SMTP_SETUP`,
  `DOMAIN_MBOX_SETUP`, `PGSQL_USER`, `MARIADB_USER`.
  Diese Typen ließen sich deshalb per API gar nicht anlegen.
- `POST /api/hs/booking/items` ohne `hostingAsset` schickte den Text `"null"` statt des echten Werts `null`.
  Das startete ungewollt die Hosting-Asset-Factories.
  Und bei `MANAGED_WEBSPACE` führte das zu einem Fehler 500.
- `POST /api/hs/booking/items` für `DOMAIN_SETUP` mit `hostingAsset`, aber ohne `subHostingAssets`,
  endete in einem Fehler 500 (NullPointerException) ohne verständliche Fehlermeldung.
- Die `ManagedWebspaceHostingAssetFactory` warf eine NullPointerException,
  wenn das Webspace-Booking-Item kein Parent-Booking-Item hatte.
- `PatchableMapWrapper.assign(...)` (POST-Pfad) wandelte JSON-Arrays nicht in `String[]` um.
  Der PATCH-Pfad konnte das schon.
  Deshalb ließen sich `EMAIL_ADDRESS` und `EMAIL_ALIAS` mit `target`-Array per POST nicht anlegen.
- `computeHomedir` im `UNIX_USER`-Validator schlug fehl beim Default-User eines Webspace.
  Bei diesem User ist der Identifier gleich dem Webspace-Namen.

## Nicht-Ziele

- Keine neuen API-Endpunkte und keine Typ-Metadaten; dieses folgt in einem eigenen PR.
- Keine Änderung am Legacy-Kompatibilitätsmodus der `DomainSetupHostingAssetFactory`,
  der weiterhin alle vier technischen Sub-Setups verlangt
  (siehe `TODO.legacy`-Kommentar in `create()`).
- Kein optionaler `identifier` beim Anlegen; auch das folgt im Typ-Metadaten-PR.

## Die Lösung

Das `HsHostingAssetInsert`-Schema bekommt ein neues Feld: `assignedToAsset.uuid`.
Es ist optional (nullable), genau wie `parentAsset.uuid`.
Der Postmapper im `HsHostingAssetController` löst diese UUID auf, genau wie die anderen Referenzen.
Er nutzt dafür die eingeschränkte Sicht aus PR#265, sodass ein unsichtbares Asset
sich wie ein unbekanntes Asset verhält und HTTP 404 ergibt.

Fehler in der Anfrage selbst prüft jetzt die neue Methode `validateRequest()` der `HostingAssetFactory`.
Der `HsBookingItemCreatedListener` ruft sie außerhalb von `createAndPersist(...)` auf.
Für `DOMAIN_SETUP` meldet sie fehlende oder unvollständige `subHostingAssets`,
ein `DOMAIN_HTTP_SETUP` ohne `assignedToAsset.uuid` und über-spezifizierte Sub-Assets;
für `MANAGED_WEBSPACE` einen falschen Asset-Typ.
Der Client bekommt dafür HTTP 400, und das Booking-Item wird zurückgerollt.
Fehler aus `create()` werden weiterhin nur zu einer `statusMessage` am Event, also HTTP 201.
Der Unterschied: Einen Fehler in der Anfrage kann der Client sofort selbst beheben.
Die `statusMessage` bleibt dem vorbehalten, was ohnehin erst später von Hand entsteht.
Vorher entschied allein die Stelle des Wurfs über 400 oder 201,
weshalb gleichartige Fehler unterschiedliche Antworten bekamen.

Die übrigen Fehler sind kleine, lokale Korrekturen:

- `HsBookingItemController`: kein Event-JSON `"null"` mehr.
- `HsBookingItemCreatedListener`: Ob ein Booking-Item auf die manuelle Einrichtung wartet,
  hängt jetzt an seinem Typ und nicht mehr daran, ob ein `hostingAsset` mitgeschickt wurde.
  Ein gebuchter Managed Server bekommt seine Zeile in `hs_booking.item_created_event` also weiterhin.
  Event-Zeilen mit dem alten JSON-Text `"null"` führen außerdem nicht mehr zu einer NullPointerException.
- `ManagedWebspaceHostingAssetFactory`: keine NullPointerException mehr ohne Parent-Booking-Item.
- `PatchableMapWrapper`: wandelt JSON-Arrays jetzt auch im POST-Pfad in `String[]` um.
- `HsUnixUserHostingAssetValidator`: Homedir des Default-Users ist jetzt `/home/pacs/<webspace>`.

Verifiziert durch neue bzw. erweiterte Tests:

- [HsHostingAssetControllerRestTest](../../src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/HsHostingAssetControllerRestTest.java)
  (`PGSQL_USER`-POST mit `assignedToAsset.uuid`, vor der Schema-Erweiterung nachweislich rot)
- [HsBookingItemCreatedListenerUnitTest](../../src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/factories/HsBookingItemCreatedListenerUnitTest.java)
  und [HsBookingItemControllerAcceptanceTest](../../src/test/java/net/hostsharing/hsadminng/hs/booking/item/HsBookingItemControllerAcceptanceTest.java)
  (400 bei fehlenden `subHostingAssets`; Wartezeile für gebuchte Server ohne `hostingAsset`)
- [DomainSetupHostingAssetFactoryUnitTest](../../src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/factories/DomainSetupHostingAssetFactoryUnitTest.java)
  (400 bei unvollständigen, über-spezifizierten und unzugewiesenen Sub-Assets)
- [ManagedWebspaceHostingAssetFactoryUnitTest](../../src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/factories/ManagedWebspaceHostingAssetFactoryUnitTest.java),
  [PatchableMapWrapperUnitTest](../../src/test/java/net/hostsharing/hsadminng/mapper/PatchableMapWrapperUnitTest.java)
  und [HsUnixUserHostingAssetValidatorUnitTest](../../src/test/java/net/hostsharing/hsadminng/hs/hosting/asset/validators/HsUnixUserHostingAssetValidatorUnitTest.java)

## Zusätzliche Änderungen

Alle Befunde des Code-Reviews sind hier behoben, jeder davon mit vorab rotem Test:

- `assignedToAsset.uuid` steht jetzt auch im Antwort- und im Patch-Schema.
  Bisher ließ sich die Zuweisung setzen, aber weder lesen noch korrigieren.
- Ein `null`-Element in einem Konfigurations-Array ergibt jetzt HTTP 400 statt HTTP 500.
- Ein `null`-Wert im POST-Body wird jetzt wie beim PATCH behandelt:
  die Property fehlt danach, statt dass ein wörtliches `null` im jsonb landet.
  Diese `assign(...)`-Semantik gilt für alle Nutzer des `PatchableMapWrapper`,
  also auch für Booking-Resources und die Kontaktdaten im Office-Modul.
- Aus jsonb geladene Arrays werden jetzt auch beim Lesen und in `putAll(...)` gewandelt;
  vorher scheiterte „laden, dann PATCH" weiterhin an `expected String[] but is ArrayList`.
- Die Abfragen nach genau einem Asset sind mit `setMaxResults(1)` begrenzt,
  statt über `getResultStream()` einen Cursor offen zu lassen; das betrifft auch
  `HsManagedWebspaceBookingItemValidator` außerhalb des eigentlichen Diffs.
- `computeHomedir` behält Alt-Bezeichner ohne Webspace-Präfix vollständig bei,
  statt sie blind abzuschneiden; das ergab bisher ein falsches Verzeichnis oder HTTP 500 beim Lesen.

- Ideen-Eintrag in `doc/PR/IDEAS.md`: Scenario-Test-Abdeckung der Booking- und Hosting-Endpunkte.
- Die Idee zum Legacy-Kompatibilitätsmodus der `DomainSetupHostingAssetFactory`
  ist jetzt als `TODO.legacy`-Kommentar direkt an der betroffenen Stelle im Code.
- `assertEventStatus(...)` in den Factory-Unit-Tests prüft jetzt wirklich;
  die bisherige `Optional.map(...)`-Kette ohne Terminaloperation lief ohne eine einzige Zusicherung durch.
- Die Meldung für ein nicht auflösbares `assignedToAsset.uuid` im `DOMAIN_SETUP`-Pfad
  nennt jetzt den wirklichen Grund, weil die fehlende Angabe schon `validateRequest()` abfängt.
- Zwei damit aufgedeckte tote Tests in `DomainSetupHostingAssetFactoryUnitTest` sind entfernt:
  Sie erwarteten eine Meldung, die es im Code nicht gibt, und ihre Rümpfe waren Duplikate.

## Folge-PRs

- Typ-Metadaten für Hosting-Assets und Booking-Items im API samt Scenario-Tests
  (Branch `mihoe/improved-hosting-type-descriptions-for-frontend`).
- Demodaten-Generator für Booking und Hosting.
- Einzeln zuordenbare technische Domain-Sub-Setups über die Hosting-API statt alle vier zusammen
  im Buchungspfad (siehe `TODO.legacy` in `DomainSetupHostingAssetFactory.validateRequest()`).
  Voraussetzung: Das Backend verwaltet das DNS selbst, statt es dem Dateisystem des Legacy-Systems zu überlassen.

---

## Release Note

released: 2026-08-20

### Hosting assets with a required assignment can now be created via the API

Since 2026-08-17, `POST /api/hs/hosting/assets` accepts `assignedToAsset.uuid`,
so asset types with a required assignment
(domain DNS/HTTP/SMTP/MBOX setups, PostgreSQL and MariaDB users) can be created directly.
The assignment also shows up in the asset response and can be changed with `PATCH`.
A `DOMAIN_SETUP` booking with missing or incomplete technical sub-assets
now gets status 400 and an explanatory message, instead of status 500 or a silent status 201.
