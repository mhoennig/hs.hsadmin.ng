package net.hostsharing.hsadminng.hs.hosting.asset;

public enum HsHostingAssetType {
    CLOUD_SERVER, // named e.g. vm1234
    MANAGED_SERVER, // named e.g. vm1234
    MANAGED_WEBSPACE(MANAGED_SERVER), // named eg. xyz00
    UNIX_USER(MANAGED_WEBSPACE), // named e.g. xyz00-abc
    DOMAIN_SETUP(UNIX_USER), // named e.g. example.org

    // TODO.spec: SECURE_MX
    EMAIL_ALIAS(MANAGED_WEBSPACE), // named e.g. xyz00-abc
    EMAIL_ADDRESS(DOMAIN_SETUP), // named e.g. sample@example.org
    PGSQL_USER(MANAGED_WEBSPACE), // named e.g. xyz00_abc
    PGSQL_DATABASE(MANAGED_WEBSPACE), // named e.g. xyz00_abc, TODO.spec: or PGSQL_USER?
    MARIADB_USER(MANAGED_WEBSPACE), // named e.g. xyz00_abc
    MARIADB_DATABASE(MANAGED_WEBSPACE); // named e.g. xyz00_abc, TODO.spec: or MARIADB_USER?


    public final HsHostingAssetType parentAssetType;

    HsHostingAssetType(final HsHostingAssetType parentAssetType) {
        this.parentAssetType = parentAssetType;
    }

    HsHostingAssetType() {
        this(null);
    }
}
