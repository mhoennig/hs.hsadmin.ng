# Scenario: Ausübung von Rechten aufgrund von Gruppenzugehörigkeit

ACHTUNG: Diese Szenarien sind noch nicht vollständig implementiert und daher auch vom derzeitigen Scenario-Test noch nicht vollständig abgedeckt.

TODO.test: Group-Szenerio-Test vervollständigen.

## Annahmen / Bereitgestellte Testdaten:

1. In **Keycloak** seien bekannt:
   - der **User xyz-adam**, zwar Admin, aber in keiner hier relevanten Gruppe,
   - die **Gruppe xyz-webmasters**,
   - der **User xyz-weber** in der Gruppe *xyz-webmasters*.

2. In **hsadmin-NG** seien bekannt:
   - der User **xyz-weber** als `USER`-Subject,
   - die Gruppe **xyz-webmasters** als `GROUP`-Subject,
   - der User **xyz-adam**<br/>
     mit der Admin-Rolle des Projektes *xyz-website* (*hs_booking.project#xyz-website:ADMIN*) direkt oder indirekt, z.B. via Debitor-Admin,
   - ein Project **xyz-website** mit Booking-Items.

Mit anderen Worten: Nach dem Anlegen des User-Accounts und der Gruppe ist das Sync von Keycloak nach hsadmin-NG bereits gelaufen.

Zu beachten: Alle drei Keycloak-Subjects liegen im selben Realm, symbolisiert durch das identische Prefix "xyz-".


## Technische Testsequenz (muss fachlich nach ausführendem User sortiert werden):

- [ ] User **xyz-adam** macht ein `GET /api/rbac/subjects?type=USER`<br/>
    <!-- Hinweis: der Query-Parameter `type=...` wird erst in diesem PR implementiert werden -->
    <!-- Hinweis: "im selben Realm" wird erst in einem späteren PR implementiert werden -->
    => der User **xyz-weber** ist darin enthalten, weil er als `USER`-Subject im selben Realm bekannt ist

- [x] User **person-FirbySusan@example.com** macht ein `GET /api/rbac/subjects?name=/xyz-Service&type=GROUP`<br/>
    <!-- Hinweis: "im selben Realm" wird erst in einem späteren PR implementiert werden -->
   => die Gruppe */xyz-Service* ist darin enthalten, weil sie als `GROUP`-Subject bekannt ist<br/>
   - <span style="color: blue;">Die UUID der Gruppe merken!</span>

- [ ] User **xyz-adam** macht ein `GET /api/rbac/subjects?type=GROUP`<br/>
    <!-- Hinweis: "im selben Realm" wird erst in einem späteren PR implementiert werden -->
   => die Gruppe *xyz-webmasters* ist darin enthalten, weil sie als `GROUP`-Subject im selben Realm bekannt ist<br/>
   - <span style="color: blue;">Die UUID der Gruppe merken!</span>

- [ ] User **xyz-weber** macht ein `GET /api/rbac/subjects?type=GROUP`<br/>
    <!-- Hinweis: "im selben Realm" wird erst in einem späteren PR implementiert werden -->
   => die Gruppe *xyz-webmasters* ist in der Ausgabe enthalten, weil sie als `GROUP`-Subject im selben Realm bekannt ist

- [ ] User **xyz-weber** macht ein `GET /api/hs/booking/projects`<br/>
   => das Projekt "xyz-website" ist darin NICHT enthalten, da es noch kein passendes *grant* gibt

- [x] User **person-FirbySusan@example.com** macht ein `GET /api/hs/booking/projects`<br/>
   - mit Header `Hostsharing-Assumed-Roles: hs_office.relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT`
   => das Projekt *D-1000111 default project* ist darin enthalten<br/>
   - <span style="color: blue;">Die UUID des Projektes merken!</span>

- [x] User **person-FirbySusan@example.com** macht `GET /api/rbac/roles?name=hs_booking.project#D-1000111-D-1000111defaultproject:ADMIN`<br/>
   - mit Header `Hostsharing-Assumed-Roles: hs_booking.project#uuid-des-projekts:OWNER`
   => die Rolle für *hs_booking.project#D-1000111-D-1000111defaultproject:ADMIN* ist enthalten<br/>
   - <span style="color: blue;">Die UUID der Rolle merken!</span>

- [ ] User **xyz-adam** macht `GET /api/rbac/roles`<br/>
   => darunter die Rollen für *hs_booking.project#xyz-website:ADMIN* und *hs_booking.project#xyz-website:REFERRER*<br/>
   - <span style="color: blue;">Die UUIDs der beiden Rollen merken!</span>

- [x] User **person-FirbySusan@example.com** macht `GET /api/rbac/grants/uuid-der-project-admin-role/uuid-der-gruppe`<br/>
   => das Grant existiert vor dem Test noch nicht

- [ ] User **xyz-adam** macht `POST /api/rbac/grants`
   - mit Header `X-hsadmin-NG-assumed-roles: hs_booking.project#xyz-website:ADMIN` \
     <span style="color: blue;">Dafür die eine gemerkte UUID von oben verwenden!</span>
   - mit Body `{ "assumed": false, "grantedRole.uuid": "uuid-der-project-xyz-website-admin-role", "granteeSubject.uuid": "uuid-der-xyz-webmaster-gruppe" }`<br/>

   => dieses *grant* gibt der Gruppe *xyz-webmasters* die Rolle *hs_booking.project#xyz-website:ADMIN*, ohne auto-assume<br/>
   => damit erhält indirekt auch der User **xyz-weber** diese Rolle, ebenso ohne auto-assume
   - Sichtbar würde das Project aber eben erst mit einem Assume, was aber ohne Sichtbarkeit nicht möglich ist.

- [ ] User **xyz-adam** macht `POST /api/rbac/grants`
    - mit Header `X-hsadmin-NG-assumed-roles: hs_booking.project#xyz-website:REFERRER` \
      <span style="color: blue;">Dafür die andere gemerkte UUID von oben verwenden!</span>
    - mit Body `{ "assumed": true, "grantedRole.uuid": "uuid-der-project-xyz-website-admin-role", "granteeSubject.uuid": "uuid-der-xyz-webmaster-gruppe" }`<br/>

   => dieses *grant* gibt der Gruppe *xyz-webmasters* die Rolle *hs_booking.project#xyz-website:REFERRER*, mit auto-assume<br/>
   => damit erhält indirekt auch der User **xyz-weber** diese Rolle, ebenso mit auto-assume
      und das Project wird somit für diesen auch sichtbar, nicht allerdings was noch darunter hängt,
      also z.B. keine Booking-Items, denn die darf ein Project-REFERRER nicht sehen.

- [x] User **person-FirbySusan@example.com** macht `POST /api/rbac/grants`
   - mit Header `Hostsharing-Assumed-Roles: hs_booking.project#uuid-des-projekts:OWNER`
   - mit Body `{ "assumed": true, "grantedRole.uuid": "uuid-der-project-admin-role", "granteeSubject.uuid": "uuid-der-gruppe" }`<br/>

   => dieses *grant* gibt der Gruppe */xyz-Service* die Rolle *hs_booking.project#D-1000111-D-1000111defaultproject:ADMIN*, derzeit mit auto-assume<br/>
   => damit erhält indirekt auch der User **selfregistered-user-drew@hostsharing.org** diese Rolle, wenn dessen JWT die Gruppe enthält

- [x] User **selfregistered-user-drew@hostsharing.org** macht ein `GET /api/hs/booking/projects`<br/>
   - mit Gruppenzugehörigkeit */xyz-Service* im JWT<br/>
   => das Projekt *D-1000111 default project* ist nun darin enthalten

- [x] User **selfregistered-user-drew@hostsharing.org** macht ein `GET /api/rbac/context`
   - mit Gruppenzugehörigkeit */xyz-Service* im JWT
   - mit Header `Hostsharing-Assumed-Roles: hs_booking.project#uuid-des-projekts:ADMIN`
   => der Context enthält den User als Subject und genau die angenommene Projekt-ADMIN-Rolle

- [x] User **selfregistered-user-drew@hostsharing.org** macht ein `GET /api/hs/booking/projects`
   - mit Gruppenzugehörigkeit */xyz-Service* im JWT
   - mit Header `Hostsharing-Assumed-Roles: hs_booking.project#uuid-des-projekts:ADMIN`
   => das Projekt *D-1000111 default project* ist darin enthalten

- [ ] User **xyz-weber** macht ein `GET /api/hs/booking/projects`<br/>
   => das Projekt *xyz-website* ist nun darin enthalten
   - Damit das funktioniert, muss bei unassumed grants der für einen vorab-PR geplante Fallback auf die SELECT-Permission des Zielobjektes implementiert sein.

- [ ] User **xyz-weber** macht ein `GET /api/hs/booking/items`
   - mit Header `X-hsadmin-NG-assumed-roles: hs_booking.project#xyz-website:ADMIN`
   => etwaige Booking-Items des Projekts sind sichtbar<br/>
      (ohne `assumedRoles` gäbe es nur den SELECT-Fallback aus dem unassumed-grant auf das Projekt selbst; Objekte darunter wären nicht sichtbar)

- [x] User **selfregistered-user-drew@hostsharing.org** macht ein `GET /api/hs/hosting/assets?projectUuid=uuid-des-projekts`
   - mit Gruppenzugehörigkeit */xyz-Service* im JWT
   - mit Header `Hostsharing-Assumed-Roles: hs_booking.project#uuid-des-projekts:ADMIN`
   => die Hosting-Assets mit den Identifiern *fir01* und *vm1011* sind sichtbar

Anmerkung: Usernamen, Gruppennamen sind nur Beispiele. Neue API-Pfade müssen ggf. noch dem Design angepasst werden.
