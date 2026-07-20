package net.hostsharing.hsadminng.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Provisions the global-admin API_KEY subject `hsadminng.provisioning.key` from the configuration.
 *
 * When starting with a legacy database, there is neither a Keycloak-matching USER subject nor
 * any API_KEY subject, thus nobody could create the first API-key via the API. If
 * `hsadminng.security.provisioning-api-key-sha256` (HSADMINNG_PROVISIONING_API_KEY_SHA256) is configured,
 * this bootstrap creates the API_KEY subject "hsadminng.provisioning.key" with the global ADMIN role and
 * stores the configured SHA-256 hash - but only if no API-key is stored for that subject yet:
 * an already stored API-key always takes precedence, thus restarting the application never
 * changes it, not even if the configured hash differs.
 *
 * Only the SHA-256 hash of the API-key is configured, thus the clear-text API-key exists
 * neither on the application server nor in the database, but only at the client. It is
 * validated on each request by the ApiKeyAuthenticationFilter, just like any other API-key.
 */
@Slf4j
@Component
public class ProvisioningApiKeyBootstrap implements ApplicationRunner {

    public static final String HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME = "hsadminng.provisioning.key";
    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private final JdbcTemplate jdbcTemplate;
    private final String configuredApiKeyHash;

    public ProvisioningApiKeyBootstrap(
            final JdbcTemplate jdbcTemplate,
            @Value("${hsadminng.security.provisioning-api-key-sha256:}") final String configuredApiKeyHash) {
        this.jdbcTemplate = jdbcTemplate;
        this.configuredApiKeyHash = configuredApiKeyHash;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (!StringUtils.hasText(configuredApiKeyHash)) {
            return;
        }
        final var configuredKeyHash = configuredApiKeyHash.trim().toLowerCase();
        if (!SHA256_HEX_PATTERN.matcher(configuredKeyHash).matches()) {
            throw new IllegalStateException(
                    "hsadminng.security.provisioning-api-key-sha256 (HSADMINNG_PROVISIONING_API_KEY_SHA256)"
                            + " must be a hex-encoded SHA-256 hash (64 hex digits)");
        }

        final var storedKeyHashes = jdbcTemplate.queryForList("""
                select ak.keyHash
                    from rbac.api_key ak
                    join rbac.subject s on s.uuid = ak.uuid
                    where s.name = '%s'
                """.formatted(HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME), String.class);
        if (!storedKeyHashes.isEmpty()) {
            if (storedKeyHashes.getFirst().equals(configuredKeyHash)) {
                log.info("the provisioning API-key subject '{}' is already provisioned", HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME);
            } else {
                log.warn("the configured provisioning API-key hash differs from the API-key stored for subject '{}';"
                        + " the stored API-key takes precedence and remains valid", HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME);
            }
            return;
        }

        // Privileged bootstrap seed, run before any global-admin subject exists, so it must use the
        // unchecked RBAC primitives: the checked repository path (rbac.create_api_key) rejects callers
        // without an authenticated global admin -- the very subject this seed creates. Like the Liquibase
        // seed changesets it inlines a system-context do-block; hash validated as hex, name is a constant.
        jdbcTemplate.execute("""
                do language plpgsql $$
                declare
                    subjectUuid uuid;
                    admins      uuid;
                begin
                    call base.defineContext('provisioning the API-key from configuration', null, null, null);

                    subjectUuid := rbac.create_subject_if_not_exist('%s', 'API_KEY'::rbac.SubjectType);

                    admins := rbac.findRoleId(rbac.global_ADMIN());
                    if not exists (
                        select 1 from rbac.grant
                            where ascendantUuid = subjectUuid and descendantUuid = admins) then
                        call rbac.grantRoleToSubjectUnchecked(admins, admins, subjectUuid);
                    end if;

                    insert into rbac.api_key (uuid, keyHash) values (subjectUuid, '%s');
                end $$;
                """.formatted(HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME, configuredKeyHash));
        log.info("provisioned the provisioning API-key subject '{}' with the global ADMIN role", HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME);
    }
}
