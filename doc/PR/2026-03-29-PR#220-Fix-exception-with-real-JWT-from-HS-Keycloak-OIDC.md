# PR#220: Fix exception with real JWT from HS Keycloak OIDC

## The Problems

### Hsadmin-NG Throws an Exception at Startup

A locally running hsadmin-NG app does not work with the real JWT from an HS Keycloak OIDC:

```sh
source .unset-environment

export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
export HSADMINNG_POSTGRES_ADMIN_USERNAME=postgres
export HSADMINNG_POSTGRES_ADMIN_PASSWORD=password
export HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted
export HSADMINNG_SUPERUSER=superuser-alex@hostsharing.net
export HSADMINNG_MIGRATION_DATA_PATH=migration
export HSADMINNG_OFFICE_DATA_SQL_FILE=

export HSADMINNG_ACCOUNT_PASSWORD_HASH_ALGORITHM='{SSHA}'
export HSADMINNG_JWT_ISSUER=https://login.dev.hsadmin.de/realms/testui
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://login.dev.hsadmin.de/realms/testui

export LANG=en_US.UTF-8

export ALLOWED_ORIGINS=http://127.0.0.1:8082

gw bootRun --args='--spring.profiles.active=dev,complete,test-data'
```

This fails with the following error:

```
Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-03-27T15:12:48.630+01:00 ERROR 11995 --- [    restartedMain] o.s.boot.SpringApplication                            &nbsp;: Application run failed

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration': Unsatisfied dependency expressed through method 'setFilterChains' parameter 0: Error creating bean with name 'securityFilterChain' defined in class path resource [net/hostsharing/hsadminng/config/WebSecurityConfig.class]: Failed to instantiate [org.springframework.security.web.SecurityFilterChain]: Factory method 'securityFilterChain' threw exception with message: Error creating bean with name 'jwtDecoder' defined in class path resource [net/hostsharing/hsadminng/config/WebSecurityConfig.class]: Failed to instantiate [org.springframework.security.oauth2.jwt.JwtDecoder]: Factory method 'jwtDecoder' threw exception with message: jwkSetUri cannot be empty
                at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredMethodElement.resolveMethodArguments(AutowiredAnnotationBeanPostProcessor.java:896) ~[spring-beans-6.2.10.jar:6.2.10]
                at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredMethodElement.inject(AutowiredAnnotationBeanPostProcessor.java:849) ~[spring-beans-6.2.10.jar:6.2.10]
                at org.springframework.beans.factory.annotation.InjectionMetadata.inject(InjectionMetadata.java:146) ~[spring-beans-6.2.10.jar:6.2.10]
                at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessProperties(AutowiredAnnotationBeanPostProcessor.java:509) ~[spring-beans-6.2.10.jar:6.2.10]
```

### jwt-curl login does not work

```
jwt-curl login
Username: superuser-alex@hostsharing.net
Password: password
ERROR: could not get JWT access token: curl: (22) The requested URL returned error: 401
{"error":"invalid_client","error_description":"Invalid client or Invalid client credentials"}
```

## The Cause

### Cause of the App-Start Problem

The environment sets `HSADMINNG_JWT_ISSUER`and `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`, which is redundant.
But it does neither set `HSADMINNG_JWT_JWKS_URL` nor `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKS_URI`.

See also the Spring config in the `main/.../application.yml`: 

```
    security:
        oauth2:
            resourceserver:
                jwt:
                    issuer-uri: ${HSADMINNG_JWT_ISSUER:}
                    jwk-set-uri: ${HSADMINNG_JWT_JWKS_URL:}
```

When `issuer-uri` is in the Spring config at all and `HSADMINNG_JWT_JWKS_URL` is unset, it becomes an empty string.
And an empty value is invalid for `NimbusJwtDecoder.withJwkSetUri(...)`, which is used by `BaseWebSecurityConfig`,
as well as for the default-bean if we make it conditional.


### Cause of the jwt-curl Login Problem

jwt-curl needs the envionment variables `HSADMINNG_JWT_TOKEN_URL` and `HSADMINNG_JWT_CLIENT_ID` to be set properly set.

## The Solution

### Improving the README.md to Run the App with a Real JWT

I added a "HOWTO" title above the part in the `README.md` so it can be found easier by running:

```sh
. .aliases
howto real keycloak
# or e.g.
howot real jwt
```

### Fixing the Environment Problem to Run the App with a Real JWT

Remove the redundant environment variable:

```
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://login.dev.hsadmin.de/realms/testui
```

Set the issuer (required):

```
export HSADMINNG_JWT_ISSUER=https://login.dev.hsadmin.de/realms/testui
```

Set the JWKS URL only if you want to override autodetection (optional):

```
export HSADMINNG_JWT_JWKS_URL=https://login.dev.hsadmin.de/realms/testui/protocol/openid-connect/certs
```

Otherwise, make sure it's unset, e.g. via `unset HSADMINNG_JWT_JWKS_URL`.

Now the app starts. Let's also test auth:

```sh
CLIENT_ID=... # to be replaced
USERNAME=...  # to be replaced
PASSWORD=...  # to be replaced
BEARER="$(curl -X POST https://login.dev.hsadmin.de/realms/testui/protocol/openid-connect/token \
               -H "Content-Type: application/x-www-form-urlencoded" \
               -d "client_id=$CLIENT_ID" \
               -d "scope=openid \
               -d "grant_type=password" \
               -d "username=$USERNAME" \
               -d "password=$PASSWORD" \
          | jq -r '.refresh_token')"
```

Now try to fetch the accessible accounts:

```sh
curl --no-progress-meter -X GET 'http://127.0.0.1:8080/api/hs/accounts/current' \
     -H "Origin: https://testui.hsngdev.hs-example.de" \
     -H 'Accept: application/json' \
     -H "Authorization: Bearer $BEARER" \
     | jq
```

### Autodetection of jwk-set-uri

The environment variable `HSADMINNG_JWT_JWKS_URL` is now optional.

`BaseWebSecurityConfig` now creates the decoder explicitly:

- if `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` has text, it uses `NimbusJwtDecoder.withJwkSetUri(...)`
- if it is empty but `spring.security.oauth2.resourceserver.jwt.issuer-uri` has text, it uses `JwtDecoders.fromIssuerLocation(...)`
- if both are empty, startup fails fast with a clear `IllegalStateException`

This prevents Spring Boot auto-configuration from trying to use an empty `jwk-set-uri`.


### Fixing the jwt-curl Login Problem

Properly set the environment variables required by `jwt-curl`:

<!-- disable:fixmes -->
```
export HSADMINNG_JWT_CLIENT_ID=FIXME
export HSADMINNG_JWT_CLIENT_SECRET=FIXME
export HSADMINNG_JWT_USERNAME=superuser-alex@hostsharing.net
export HSADMINNG_JWT_PASSWORD=password
export HSADMINNG_JWT_TOKEN_URL=FIXME
```
<!-- enable:fixmes -->

TODO.doc: Find out the proper values. This is subject for PR#235.
