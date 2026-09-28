package net.hostsharing.hsadminng.rbac.generator;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RbacSpecUnitTest {

    @Test
    void findsTheEntitiesWithTheirOwnRbacSpecificationAcrossAllModules() {
        // the scanned package is the one RbacSpec.main generates the RBAC views from;
        // the entities are named, not imported, because this package must not depend on the business modules
        val rbacEntityNames = RbacSpec.findRbacEntityClasses("net.hostsharing.hsadminng").stream()
                .map(Class::getSimpleName)
                .toList();

        assertThat(rbacEntityNames)
                .contains("HsOfficeContactRbacEntity", "HsBookingItemRbacEntity", "HsHostingAssetRbacEntity")
                .doesNotContain("HsOfficeContactRealEntity");
    }
}
