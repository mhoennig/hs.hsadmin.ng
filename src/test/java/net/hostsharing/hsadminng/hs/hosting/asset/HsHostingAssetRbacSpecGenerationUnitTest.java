package net.hostsharing.hsadminng.hs.hosting.asset;

import lombok.val;
import net.hostsharing.hsadminng.rbac.generator.RbacViewPostgresGenerator;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class HsHostingAssetRbacSpecGenerationUnitTest {

    /**
     * Under the Turkish locale, 'ADMIN'.toLowerCase() becomes 'admın' (dotless ı),
     * which would make the generated identifiers deviate from the committed changelogs.
     */
    @Test
    void generatesLocaleIndependentIdentifiers() {
        val defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr", "TR"));

            val generated = new RbacViewPostgresGenerator(HsHostingAssetRbacEntity.rbac()).toString();

            assertThat(generated).contains("hs_hosting.asset_grants_insert_to_global_admin_tf()");
            assertThat(generated).doesNotContain("admın");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
