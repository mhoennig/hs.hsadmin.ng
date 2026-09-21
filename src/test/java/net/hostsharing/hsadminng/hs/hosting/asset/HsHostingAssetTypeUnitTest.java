package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.FORBIDDEN;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.OPTIONAL;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.REQUIRED;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.TERMINATORY;
import static org.assertj.core.api.Assertions.assertThat;

class HsHostingAssetTypeUnitTest {

    @Test
    void generatedPlantUML() {
        final var result = HsHostingAssetType.renderAsEmbeddedPlantUml();

        assertThat(result).isEqualTo("""
                ## HostingAsset Type Structure
                
                
                ### Server+Webspace
    
                ```plantuml
                @startuml
                left to right direction
    
                package Booking #feb28c {
                    entity BI_PRIVATE_CLOUD
                    entity BI_CLOUD_SERVER
                    entity BI_MANAGED_SERVER
                    entity BI_MANAGED_WEBSPACE
                    entity BI_DOMAIN_SETUP
                }
    
                package Hosting #feb28c{
                    package Server #99bcdb {
                        entity HA_CLOUD_SERVER
                        entity HA_MANAGED_SERVER
                        entity HA_IPV4_NUMBER
                        entity HA_IPV6_NUMBER
                    }
    
                    package Webspace #99bcdb {
                        entity HA_MANAGED_WEBSPACE
                        entity HA_UNIX_USER
                        entity HA_EMAIL_ALIAS
                    }
    
                }
    
                BI_CLOUD_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_WEBSPACE *--> BI_MANAGED_SERVER
    
                HA_CLOUD_SERVER *==> BI_CLOUD_SERVER
                HA_MANAGED_SERVER *==> BI_MANAGED_SERVER
                HA_MANAGED_WEBSPACE *==> BI_MANAGED_WEBSPACE
                HA_MANAGED_WEBSPACE o..> HA_MANAGED_SERVER
                HA_UNIX_USER *==> HA_MANAGED_WEBSPACE
                HA_EMAIL_ALIAS *==> HA_MANAGED_WEBSPACE
                HA_IPV4_NUMBER o..> HA_CLOUD_SERVER
                HA_IPV4_NUMBER o..> HA_MANAGED_SERVER
                HA_IPV4_NUMBER o..> HA_MANAGED_WEBSPACE
                HA_IPV6_NUMBER o..> HA_CLOUD_SERVER
                HA_IPV6_NUMBER o..> HA_MANAGED_SERVER
                HA_IPV6_NUMBER o..> HA_MANAGED_WEBSPACE
    
                package Legend #white {
                    SUB_ENTITY1 *--> REQUIRED_PARENT_ENTITY
                    SUB_ENTITY2 *..> OPTIONAL_PARENT_ENTITY
                    ASSIGNED_ENTITY1 o--> REQUIRED_ASSIGNED_TO_ENTITY1
                    ASSIGNED_ENTITY2 o..> OPTIONAL_ASSIGNED_TO_ENTITY2
                }
                Booking -down[hidden]->Legend
                ```
    
                ### Domain
    
                ```plantuml
                @startuml
                left to right direction
    
                package Booking #feb28c {
                    entity BI_PRIVATE_CLOUD
                    entity BI_CLOUD_SERVER
                    entity BI_MANAGED_SERVER
                    entity BI_MANAGED_WEBSPACE
                    entity BI_DOMAIN_SETUP
                }
    
                package Hosting #feb28c{
                    package Domain #99bcdb {
                        entity HA_DOMAIN_SETUP
                        entity HA_DOMAIN_DNS_SETUP
                        entity HA_DOMAIN_HTTP_SETUP
                        entity HA_DOMAIN_SMTP_SETUP
                        entity HA_DOMAIN_MBOX_SETUP
                        entity HA_EMAIL_ADDRESS
                    }
    
                    package Webspace #99bcdb {
                        entity HA_MANAGED_WEBSPACE
                        entity HA_UNIX_USER
                        entity HA_EMAIL_ALIAS
                    }
    
                }
    
                BI_CLOUD_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_WEBSPACE *--> BI_MANAGED_SERVER
    
                HA_MANAGED_WEBSPACE *==> BI_MANAGED_WEBSPACE
                HA_UNIX_USER *==> HA_MANAGED_WEBSPACE
                HA_EMAIL_ALIAS *==> HA_MANAGED_WEBSPACE
                HA_DOMAIN_SETUP *..> BI_DOMAIN_SETUP
                HA_DOMAIN_SETUP o..> HA_DOMAIN_SETUP
                HA_DOMAIN_DNS_SETUP *==> HA_DOMAIN_SETUP
                HA_DOMAIN_DNS_SETUP o--> HA_MANAGED_WEBSPACE
                HA_DOMAIN_HTTP_SETUP *==> HA_DOMAIN_SETUP
                HA_DOMAIN_HTTP_SETUP o--> HA_UNIX_USER
                HA_DOMAIN_SMTP_SETUP *==> HA_DOMAIN_SETUP
                HA_DOMAIN_SMTP_SETUP o--> HA_MANAGED_WEBSPACE
                HA_DOMAIN_MBOX_SETUP *==> HA_DOMAIN_SETUP
                HA_DOMAIN_MBOX_SETUP o--> HA_MANAGED_WEBSPACE
                HA_EMAIL_ADDRESS *==> HA_DOMAIN_MBOX_SETUP
    
                package Legend #white {
                    SUB_ENTITY1 *--> REQUIRED_PARENT_ENTITY
                    SUB_ENTITY2 *..> OPTIONAL_PARENT_ENTITY
                    ASSIGNED_ENTITY1 o--> REQUIRED_ASSIGNED_TO_ENTITY1
                    ASSIGNED_ENTITY2 o..> OPTIONAL_ASSIGNED_TO_ENTITY2
                }
                Booking -down[hidden]->Legend
                ```
    
                ### MariaDB
    
                ```plantuml
                @startuml
                left to right direction
    
                package Booking #feb28c {
                    entity BI_PRIVATE_CLOUD
                    entity BI_CLOUD_SERVER
                    entity BI_MANAGED_SERVER
                    entity BI_MANAGED_WEBSPACE
                    entity BI_DOMAIN_SETUP
                }
    
                package Hosting #feb28c{
                    package MariaDB #99bcdb {
                        entity HA_MARIADB_INSTANCE
                        entity HA_MARIADB_USER
                        entity HA_MARIADB_DATABASE
                    }
    
                    package Webspace #99bcdb {
                        entity HA_MANAGED_WEBSPACE
                        entity HA_UNIX_USER
                        entity HA_EMAIL_ALIAS
                    }
    
                }
    
                BI_CLOUD_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_WEBSPACE *--> BI_MANAGED_SERVER
    
                HA_MANAGED_WEBSPACE *==> BI_MANAGED_WEBSPACE
                HA_UNIX_USER *==> HA_MANAGED_WEBSPACE
                HA_EMAIL_ALIAS *==> HA_MANAGED_WEBSPACE
                HA_MARIADB_USER *==> HA_MANAGED_WEBSPACE
                HA_MARIADB_USER o--> HA_MARIADB_INSTANCE
                HA_MARIADB_DATABASE *==> HA_MARIADB_USER
    
                package Legend #white {
                    SUB_ENTITY1 *--> REQUIRED_PARENT_ENTITY
                    SUB_ENTITY2 *..> OPTIONAL_PARENT_ENTITY
                    ASSIGNED_ENTITY1 o--> REQUIRED_ASSIGNED_TO_ENTITY1
                    ASSIGNED_ENTITY2 o..> OPTIONAL_ASSIGNED_TO_ENTITY2
                }
                Booking -down[hidden]->Legend
                ```
    
                ### PostgreSQL
    
                ```plantuml
                @startuml
                left to right direction
    
                package Booking #feb28c {
                    entity BI_PRIVATE_CLOUD
                    entity BI_CLOUD_SERVER
                    entity BI_MANAGED_SERVER
                    entity BI_MANAGED_WEBSPACE
                    entity BI_DOMAIN_SETUP
                }
    
                package Hosting #feb28c{
                    package PostgreSQL #99bcdb {
                        entity HA_PGSQL_INSTANCE
                        entity HA_PGSQL_USER
                        entity HA_PGSQL_DATABASE
                    }
    
                    package Webspace #99bcdb {
                        entity HA_MANAGED_WEBSPACE
                        entity HA_UNIX_USER
                        entity HA_EMAIL_ALIAS
                    }
    
                }
    
                BI_CLOUD_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_SERVER *--> BI_PRIVATE_CLOUD
                BI_MANAGED_WEBSPACE *--> BI_MANAGED_SERVER
    
                HA_MANAGED_WEBSPACE *==> BI_MANAGED_WEBSPACE
                HA_UNIX_USER *==> HA_MANAGED_WEBSPACE
                HA_EMAIL_ALIAS *==> HA_MANAGED_WEBSPACE
                HA_PGSQL_USER *==> HA_MANAGED_WEBSPACE
                HA_PGSQL_USER o--> HA_PGSQL_INSTANCE
                HA_PGSQL_DATABASE *==> HA_PGSQL_USER
    
                package Legend #white {
                    SUB_ENTITY1 *--> REQUIRED_PARENT_ENTITY
                    SUB_ENTITY2 *..> OPTIONAL_PARENT_ENTITY
                    ASSIGNED_ENTITY1 o--> REQUIRED_ASSIGNED_TO_ENTITY1
                    ASSIGNED_ENTITY2 o..> OPTIONAL_ASSIGNED_TO_ENTITY2
                }
                Booking -down[hidden]->Legend
                ```
    
                This code generated was by HsHostingAssetType.main, do not amend manually.
                """);
    }

    @Test
    void bookingItemPolicies() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.bookingItemPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.MANAGED_SERVER.bookingItemPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.bookingItemPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.UNIX_USER.bookingItemPolicy()).isEqualTo(FORBIDDEN);
        assertThat(HsHostingAssetType.DOMAIN_SETUP.bookingItemPolicy()).isEqualTo(TERMINATORY);
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.bookingItemPolicy()).isEqualTo(FORBIDDEN);
        assertThat(HsHostingAssetType.IPV4_NUMBER.bookingItemPolicy()).isEqualTo(FORBIDDEN);
    }

    @Test
    void parentAssetPolicies() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.parentAssetPolicy()).isEqualTo(FORBIDDEN);
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.parentAssetPolicy()).isEqualTo(OPTIONAL);
        assertThat(HsHostingAssetType.UNIX_USER.parentAssetPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.DOMAIN_SETUP.parentAssetPolicy()).isEqualTo(OPTIONAL);
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.parentAssetPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.IPV4_NUMBER.parentAssetPolicy()).isEqualTo(FORBIDDEN);
    }

    @Test
    void assignedToAssetPolicies() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.assignedToAssetPolicy()).isEqualTo(FORBIDDEN);
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.assignedToAssetPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.DOMAIN_HTTP_SETUP.assignedToAssetPolicy()).isEqualTo(REQUIRED);
        assertThat(HsHostingAssetType.IPV4_NUMBER.assignedToAssetPolicy()).isEqualTo(OPTIONAL);
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.assignedToAssetPolicy()).isEqualTo(FORBIDDEN);
    }

    @Test
    void bookingItemTypes() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.bookingItemTypes()).containsExactly(HsBookingItemType.CLOUD_SERVER);
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.bookingItemTypes()).containsExactly(HsBookingItemType.MANAGED_WEBSPACE);
        assertThat(HsHostingAssetType.DOMAIN_SETUP.bookingItemTypes()).containsExactly(HsBookingItemType.DOMAIN_SETUP);
        assertThat(HsHostingAssetType.UNIX_USER.bookingItemTypes()).isEmpty();
        assertThat(HsHostingAssetType.IPV4_NUMBER.bookingItemTypes()).isEmpty();
    }

    @Test
    void parentAssetTypes() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.parentAssetTypes()).isEmpty();
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.parentAssetTypes()).containsExactly(HsHostingAssetType.MANAGED_SERVER);
        assertThat(HsHostingAssetType.UNIX_USER.parentAssetTypes()).containsExactly(HsHostingAssetType.MANAGED_WEBSPACE);
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.parentAssetTypes()).containsExactly(HsHostingAssetType.DOMAIN_SETUP);
        assertThat(HsHostingAssetType.DOMAIN_SETUP.parentAssetTypes()).containsExactly(HsHostingAssetType.DOMAIN_SETUP);
    }

    @Test
    void assignedToAssetTypes() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.assignedToAssetTypes()).isEmpty();
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.assignedToAssetTypes()).containsExactly(HsHostingAssetType.MANAGED_WEBSPACE);
        assertThat(HsHostingAssetType.IPV4_NUMBER.assignedToAssetTypes()).containsExactlyInAnyOrder(
                HsHostingAssetType.CLOUD_SERVER, HsHostingAssetType.MANAGED_SERVER, HsHostingAssetType.MANAGED_WEBSPACE);
        assertThat(HsHostingAssetType.PGSQL_USER.assignedToAssetTypes()).containsExactly(HsHostingAssetType.PGSQL_INSTANCE);
    }

    @Test
    void belongsToAnyAndNodeName() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.belongsToAny(Set.of("Server"))).isTrue();
        assertThat(HsHostingAssetType.CLOUD_SERVER.belongsToAny(Set.of("Webspace"))).isFalse();
        assertThat(HsHostingAssetType.UNIX_USER.belongsToAny(Set.of("Webspace", "Server"))).isTrue();
        assertThat(HsHostingAssetType.CLOUD_SERVER.nodeName()).isEqualTo("HA_CLOUD_SERVER");
        assertThat(HsHostingAssetType.of(HsHostingAssetType.CLOUD_SERVER)).isEqualTo(HsHostingAssetType.CLOUD_SERVER);
        assertThat(HsHostingAssetType.of(null)).isNull();
    }

    @Test
    void edges() {
        assertThat(HsHostingAssetType.CLOUD_SERVER.edges(Set.of("Server"))).contains("HA_CLOUD_SERVER *==> BI_CLOUD_SERVER");
        assertThat(HsHostingAssetType.MANAGED_WEBSPACE.edges(Set.of("Server")))
                .contains("HA_MANAGED_WEBSPACE *==> BI_MANAGED_WEBSPACE", "HA_MANAGED_WEBSPACE o..> HA_MANAGED_SERVER");
        assertThat(HsHostingAssetType.DOMAIN_DNS_SETUP.edges(Set.of("Domain", "Webspace")))
                .contains("HA_DOMAIN_DNS_SETUP o--> HA_MANAGED_WEBSPACE");
    }
}
