# PR#274: Öffentlicher Auth-Konfigurations-Endpunkt für das Frontend

## Verwandte Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/274

## Das Problem

Das Frontend muss die Authentifizierungs-Konfiguration heute pro Umgebung in git-ignorierten,
auf separatem Weg verteilten Env-Files pflegen und zur Build-Zeit einbacken
(`REACT_APP_OIDC_AUTHORITY`, `REACT_APP_OIDC_CLIENT_ID`, `REACT_APP_FAKE_JWT_TOKEN_URL`).
Das Backend kennt dieselbe Information bereits — es validiert jeden Request gegen den konfigurierten Issuer.
Laufen beide auseinander, klappt der Login scheinbar, aber jeder API-Aufruf antwortet 401.

Welche Login-Methode (Keycloak-OIDC oder fake-jwt) passt, entscheidet allein das aktive Profil des Backends;
im Frontend muss sie dennoch redundant per Env-File konfiguriert werden.

## Nicht-Ziele

- Kein OIDC-Discovery für das `fake-jwt`-Profil; die `/.well-known`-Pfade bleiben unverändert.
- Keine `redirect_uri` in der Antwort; die leitet das Frontend aus `window.location.origin` ab —
  das Backend kennt den Origin der Frontend-Installation nicht, die verbindliche Redirect-URI-Liste liegt im Keycloak-Client.
- Keine `client_id` in der Antwort; sie gehört dem jeweiligen Client:
  Das Frontend definiert seine eigene, andere Clients (CLI etc.) haben ihre eigene.
- Keine Geheimnisse in der Antwort; Authority und Client-Id eines öffentlichen SPA-Clients stehen ohnehin im JS-Bundle.
- Der Mock-Modus des Frontends (`REACT_APP_MOCK_API=true`) bleibt env-gesteuert; ohne Backend gibt es nichts abzufragen.

## Die Szenarien

### Feature: Auth-Konfigurations-Endpunkt

#### Hintergrund

- Der Endpunkt `GET /api/auth-config` ist ein öffentliches Geschwister von `/api/version`:
  keine Authentifizierung und CORS-offen, denn der Aufruf passiert vor dem Login (Henne-Ei).
  Der Name lautet bewusst `auth-config`, nicht `auth`: Er meldet nur die Konfiguration.
- Die Antwort verzweigt auf `method` (Pflichtfeld), die übrigen Felder sind methodenspezifisch;
  unbekannte Zusatzfelder ignoriert das Frontend, damit ein drittes `method` Clients nicht bricht.
- Die gemeldeten Werte sind aus der Backend-Konfiguration abgeleitet, nicht dupliziert:
  Die gemeldete `authority` ist derselbe Issuer, gegen den das Backend Tokens validiert.
- Im `fake-jwt`-Fall enthält die Antwort mit `tokenUrl` alles Nötige — der Token-Endpunkt akzeptiert
  beliebige Zugangsdaten ohne Client-Registrierung; lokale Entwicklung braucht dann keinerlei Auth-Konfiguration.
- Kompromiss: Der Login setzt dann ein erreichbares Backend voraus;
  heute funktioniert der Keycloak-Login im Frontend auch bei totem API-Backend.

#### Szenario#274.01: Das Frontend erfragt die Auth-Konfiguration eines OIDC-Backends

Damit sich der `AuthProvider` des Frontends zur Laufzeit aus der Antwort konfiguriert, statt aus `REACT_APP_OIDC_*`.

- **Given** ein Backend mit konfiguriertem OIDC-Issuer (`HSADMINNG_JWT_ISSUER`)
- **When** das Frontend `GET /api/auth-config` ohne Token aufruft
- **Then** antwortet das Backend `200` mit `method: "oidc"`
    - **and** `authority` ist der Issuer, gegen den das Backend Tokens validiert

Verifiziert durch: [AuthConfigIntegrationTest](../../src/test/java/net/hostsharing/hsadminng/config/AuthConfigIntegrationTest.java) — als Integrationstest, denn das Szenario-Framework läuft fest im `fake-jwt`-Profil

#### Szenario#274.02: Das Frontend erfragt die Auth-Konfiguration eines fake-jwt-Backends

Damit das Frontend die fake-jwt-Login-Methode automatisch richtig wählt, statt sie per Env-File mitgeteilt zu bekommen.

- **Given** ein Backend im Profil `fake-jwt`
- **When** das Frontend `GET /api/auth-config` ohne Token aufruft
- **Then** antwortet das Backend `200` mit `method: "fake-jwt"`
    - **and** `tokenUrl: "/fake-jwt/token"`

Verifiziert durch: [AuthConfigScenarioTests.theFrontendFetchesTheAuthConfigurationOfAFakeJwtBackend](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/AuthConfigScenarioTests.java) mit dem Use-Case [FetchAuthConfiguration](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/FetchAuthConfiguration.java)

#### Szenario#274.03: Der Aufruf gelingt vor dem Login von einem fremden Origin

Damit ein lokal laufendes SPA den Endpunkt ohne Umweg über einen Proxy direkt aufrufen kann.

- **Given** ein Browser-Frontend auf einem anderen Origin, noch ohne Token
- **When** es den CORS-Preflight und `GET /api/auth-config` sendet
- **Then** beantwortet das Backend beide wie für die übrigen `/api`-Endpunkte erlaubt

Verifiziert durch: [AuthConfigScenarioTests.theAuthConfigurationCanBeFetchedFromAForeignOriginBeforeAnyLogin](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/AuthConfigScenarioTests.java) mit dem Use-Case [FetchAuthConfigurationFromForeignOrigin](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/FetchAuthConfigurationFromForeignOrigin.java)

#### Szenario#274.04: Das Frontend ruft den fake-jwt-Token-Endpunkt direkt auf

Damit der Webpack-Dev-Server-Proxy-Umweg entfällt und die gemeldete `tokenUrl` vom Browser direkt aufrufbar ist.

- **Given** ein Backend im Profil `fake-jwt`
- **When** ein Frontend auf einem anderen Origin den CORS-Preflight und `POST /fake-jwt/token` sendet
- **Then** beantwortet das Backend beide wie für die `/api`-Endpunkte erlaubt

Verifiziert durch: [AuthConfigScenarioTests.theFrontendLogsInDirectlyAtTheReportedFakeJwtTokenUrl](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/AuthConfigScenarioTests.java) mit dem Use-Case [LoginAtReportedFakeJwtTokenUrl](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/LoginAtReportedFakeJwtTokenUrl.java)

## Die Lösung

- OpenAPI-first: `GET /api/auth-config` mit Schema `AuthConfig` in `api-definition.yaml`
  neben `/api/version`; Pflichtfeld nur `method`, damit die Antwort erweiterbar bleibt.
- Der `AuthConfigController` implementiert den generierten `AuthConfigApi` und leitet die Antwort
  aus der vorhandenen Spring-Konfiguration ab: aktives Profil `fake-jwt` ergibt `method: "fake-jwt"` mit `tokenUrl`,
  sonst `method: "oidc"` mit `spring.security.oauth2.resourceserver.jwt.issuer-uri` als `authority`.
- Öffentlich wie alle `/api`-Endpunkte (permit-all auf HTTP-Ebene, keine `@PreAuthorize` auf Methodenebene).
- CORS ist auch für `/fake-jwt/**` freigegeben (`corsConfigurer` in `BaseWebSecurityConfig`, nur `POST`);
  sonst wäre die gemeldete `tokenUrl` vom Browser aus nur über einen Proxy-Umweg aufrufbar.

## Zusätzliche Änderungen

- Szenario-Framework: `FakeLoginUser.asAnonymous()` für Requests ganz ohne Auth-Header,
  `UseCase.httpOptions(...)` für CORS-Preflights;
  die Report-Erzeugung rendert Nicht-JSON-Request-Bodies (form-urlencoded) unverändert.

## Folge-PRs

- Denkbar: das `fake-jwt`-Profil zu einem minimalen OIDC-Provider ausbauen
  (Discovery, JWKS, Authorization-Endpunkt mit Code-Flow und PKCE —
  Discovery allein genügt nicht, Standard-SPA-Clients beherrschen keinen Password-Grant);
  dann entfiele die fake-jwt-Login-Methode im Frontend, und `/api/auth-config` meldete auch lokal `method: "oidc"`, ohne API-Bruch.

---

## Release Note

released: 2026-08-06

### The backend reports its authentication configuration

Since 2026-08-06, the public endpoint `GET /api/auth-config` reports which authentication method
the backend expects: `oidc` with the issuer to log in at as `authority`, or `fake-jwt` with the `tokenUrl`;
clients branch on `method` and have to ignore unknown additional fields.
Also, since then, CORS is open for `/fake-jwt/**`, so browser-frontends can call the local token endpoint directly.
