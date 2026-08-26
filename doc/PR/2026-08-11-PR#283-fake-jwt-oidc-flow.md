# PR#283: Vollständiger OIDC-Flow im fake-jwt-Profil

Das `fake-jwt`-Profil stellt jetzt einen minimalen, aber vollständigen OIDC-Provider bereit,
sodass Frontends lokal denselben Standard-Login-Flow durchlaufen wie gegen das Produktions-Keycloak.

## Relevante Links (Hostsharing-intern)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/283

## Das Problem

Das `fake-jwt`-Profil bot bisher nur einen Token-Endpoint mit Password-Grant.
`GET /api/auth-config` meldete dafür die eigene Methode `fake-jwt` mit einer `tokenUrl`,
und jedes Frontend brauchte neben seinem OIDC-Login einen zweiten, nur lokal genutzten Login-Pfad.
Dieser Sonderpfad wich auch im Verhalten ab:
kein Login-Formular, kein Redirect-Tanz, kein ID-Token —
Fehler im echten Login-Flow fielen lokal also nicht auf.

## Nicht-Ziele

- Kein echter Identity-Provider: beliebige Zugangsdaten werden akzeptiert,
  es gibt weder Client-Registrierung noch Secrets, und Autorisierungs-Codes leben nur im Speicher.
- Der Password-Grant bleibt erhalten; CLI-Werkzeuge wie `tools/jwt-login` und Tests nutzen ihn weiter.
- Keine Änderung an der Token-Validierung des Resource-Servers; die Access-Tokens bleiben unverändert.

## Die Lösung

Der `FakeJwtController` wird zum OIDC-Provider unter dem konfigurierten Issuer (`…/fake-jwt`):

- `GET …/.well-known/openid-configuration` — das Discovery-Dokument, alle Endpoints vom Issuer abgeleitet.
- `GET …/.well-known/jwks.json` — der öffentliche Schlüssel; die URL war schon konfiguriert, existierte aber nicht.
- `GET …/auth` — ein minimales Login-Formular; `POST …/auth` beantwortet es mit dem Redirect samt Autorisierungs-Code.
  Das Formular ist als zentrierte Karte im Stil der Frontend-Login-Seite gehalten (Hostsharing-Blau),
  ohne externe Ressourcen, damit es keine Abhängigkeiten in den Provider zieht.
  Statt Textfeldern bietet es die vorhandenen Benutzer aus `rbac.subject` als Auswahlliste an
  (über eine bewusst sichtbarkeitsfreie Query, denn beim Login gibt es noch keinen RBAC-Kontext);
  ein Passwort-Feld gibt es nicht mehr, da ohnehin jedes Passwort akzeptiert wurde.
  Ist die Datenbank noch leer, fällt das Formular auf ein freies Textfeld zurück.
  Die Namensliste erreicht den Provider über das config-seitige Interface `FakeJwtSelectableUsers`,
  das vom `RealSubjectRepository` implementiert wird;
  eine direkte Abhängigkeit vom rbac-Paket würde den Paketzyklus-Test der Architektur verletzen.
- `POST …/token` — zusätzlich zum Password- und Refresh-Grant jetzt `authorization_code` mit PKCE (S256 und plain);
  die Antwort enthält ein ID-Token mit `aud`, `azp` und durchgereichter `nonce`.
- `GET …/userinfo` und `GET …/logout` runden den Flow ab.
  Der Userinfo-Endpoint akzeptiert nur Access-Tokens:
  Refresh-, ID- und API-Key-Tokens sind mit demselben Schlüssel signiert
  und werden daher zusätzlich über ihre Claims abgewiesen (Review-Befund RF-2).
  Ein Logout ohne `post_logout_redirect_uri` leitet auf die beim Login gemerkte Client-Origin zurück;
  das Frontend startet dort eine frische Authorization-Anfrage und zeigt damit wieder die Login-Seite,
  statt dass der Browser auf einer „Logged out“-Seite des Backends strandet.

Autorisierungs-Codes sind einmalig, verfallen nach 60 Sekunden
und sind an `client_id`, `redirect_uri` und den PKCE-Verifier gebunden.
Redirect-URIs müssen absolute, fragment-freie http(s)-URIs auf einem Host aus
`hsadminng.fake-jwt.client-hosts` sein (Review-Befund RF-1);
der Default ist Loopback (`localhost`, `127.0.0.1`, `[::1]`).
Eine deployte Umgebung überschreibt die gesamte Liste per `HSADMINNG_FAKE_JWT_CLIENT_HOSTS`
und muss Loopback mitschreiben, wenn lokale Frontends weiter gegen dieses Backend laufen sollen.

`GET /api/auth-config` meldet jetzt in allen Umgebungen `method: oidc` mit dem Issuer als `authority`;
die Methode `fake-jwt` und das Feld `tokenUrl` entfallen aus der API.
Ein Frontend konfiguriert seinen OIDC-Client also nur noch mit der gemeldeten `authority` —
lokal wie in Produktion, ohne jede Sonderbehandlung.
Die CORS-Freigabe für `/fake-jwt/**` umfasst dafür jetzt auch GET (Discovery, JWKS, Userinfo).

Der neue Szenario-Test `LoginViaOidcAuthorizationCodeFlow` führt den kompletten Flow aus:
Discovery von fremder Origin, Login-Formular-POST mit PKCE-Challenge,
Code aus dem Redirect, Einlösen am Token-Endpoint und Prüfung der ID-Token-Claims.

## Zusätzliche Änderungen

- Die Test-`application.yml` definiert den Issuer für das `fake-jwt`-Profil,
  den sie bisher aus der Haupt-Konfiguration überschattete;
  der `@Value`-Platzhalter blieb in Tests sonst unaufgelöst.
- `tools/smoke-test` setzt `HSADMINNG_DEPLOY_HOST` jetzt als nackten Host (`stubbed`);
  PR#281 hatte `tools/remote` auf abgeleitete Logins umgestellt und das alte `user@host`-Format abgelehnt,
  wodurch der Schritt `tools/remote backend sql` im Smoke-Test seither scheiterte.
- `tools/pr-quick-check` löst eine im PR-Dokument deklarierte Basis jetzt wie die Default-Basis origin-bevorzugt auf;
  in einer Arbeitskopie ohne gleichnamigen lokalen Branch (CI) scheiterte der Check sonst
  mit `fatal: Not a valid object name`.

## Voraussetzungs-PRs

Dieser Branch baut auf PR#289 auf,
siehe [Grant-Erreichbarkeit als Fixpunkt statt Tiefen- und Mengen-Heuristik](2026-08-22-PR%23289-rbac-grant-reachability-closure.md);
die Umstellung entstand ursprünglich als vorgezogener Commit auf diesem Branch.

- `pr-quick-check:base=mihoe/rbac-grant-reachability-closure`
