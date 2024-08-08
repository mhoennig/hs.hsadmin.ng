package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_HTTP_SETUP;
import static net.hostsharing.hsadminng.hs.validation.ArrayProperty.arrayOf;
import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsDomainHttpSetupHostingAssetValidator extends HostingAssetEntityValidator {

    public static final String IDENTIFIER_SUFFIX = "|HTTP";
    public static final String FILESYSTEM_PATH = "^/.*";
    public static final String SUBDOMAIN_NAME_REGEX = "(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))";

    HsDomainHttpSetupHostingAssetValidator() {
        super(
                DOMAIN_HTTP_SETUP,
                AlarmContact.isOptional(),

                booleanProperty("htdocsfallback").withDefault(true),
                booleanProperty("indexes").withDefault(true),
                booleanProperty("cgi").withDefault(true),
                booleanProperty("passenger").withDefault(true),
                booleanProperty("passenger-errorpage").withDefault(false),
                booleanProperty("fastcgi").withDefault(true),
                booleanProperty("autoconfig").withDefault(true),
                booleanProperty("greylisting").withDefault(true),
                booleanProperty("includes").withDefault(true),
                booleanProperty("letsencrypt").withDefault(true),
                booleanProperty("multiviews").withDefault(true),
                stringProperty("fcgi-php-bin").matchesRegEx(FILESYSTEM_PATH).provided("/usr/lib/cgi-bin/php").withDefault("/usr/lib/cgi-bin/php"),
                stringProperty("passenger-nodejs").matchesRegEx(FILESYSTEM_PATH).provided("/usr/bin/node").withDefault("/usr/bin/node"),
                stringProperty("passenger-python").matchesRegEx(FILESYSTEM_PATH).provided("/usr/bin/python3").withDefault("/usr/bin/python3"),
                stringProperty("passenger-ruby").matchesRegEx(FILESYSTEM_PATH).provided("/usr/bin/ruby").withDefault("/usr/bin/ruby"),
                arrayOf(
                        stringProperty("subdomains").matchesRegEx(SUBDOMAIN_NAME_REGEX).required()
                ).optional());
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return  Pattern.compile("^" + Pattern.quote(assetEntity.getParentAsset().getIdentifier() + IDENTIFIER_SUFFIX) + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAsset entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(pa.getIdentifier() + IDENTIFIER_SUFFIX));
        }
    }
}
