package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedServerHostingAssetValidator extends HsHostingAssetEntityValidator {

    public HsManagedServerHostingAssetValidator() {
        super(
                // monitoring
                integerProperty("monit_max_cpu_usage").unit("%").min(10).max(100).withDefault(92),
                integerProperty("monit_max_ram_usage").unit("%").min(10).max(100).withDefault(92),
                integerProperty("monit_max_ssd_usage").unit("%").min(10).max(100).withDefault(98),
                integerProperty("monit_min_free_ssd").min(1).max(1000).withDefault(5),
                integerProperty("monit_max_hdd_usage").unit("%").min(10).max(100).withDefault(95),
                integerProperty("monit_min_free_hdd").min(1).max(4000).withDefault(10),
                // stringProperty("monit_alarm_email").unit("GB").optional() TODO.impl: via Contact?

                // other settings
                // booleanProperty("fastcgi_small").withDefault(false), TODO.spec: clarify Salt-Grains

                // database software
                booleanProperty("software-pgsql").withDefault(true),
                booleanProperty("software-mariadb").withDefault(true),

                // PHP
                enumerationProperty("php-default").valuesFromProperties("software-php-").withDefault("8.2"),
                booleanProperty("software-php-5.6").withDefault(false),
                booleanProperty("software-php-7.0").withDefault(false),
                booleanProperty("software-php-7.1").withDefault(false),
                booleanProperty("software-php-7.2").withDefault(false),
                booleanProperty("software-php-7.3").withDefault(false),
                booleanProperty("software-php-7.4").withDefault(true),
                booleanProperty("software-php-8.0").withDefault(false),
                booleanProperty("software-php-8.1").withDefault(false),
                booleanProperty("software-php-8.2").withDefault(true),

                // other software
                booleanProperty("software-postfix-tls-1.0").withDefault(false),
                booleanProperty("software-dovecot-tls-1.0").withDefault(false),
                booleanProperty("software-clamav").withDefault(true),
                booleanProperty("software-collabora").withDefault(false),
                booleanProperty("software-libreoffice").withDefault(false),
                booleanProperty("software-imagemagick-ghostscript").withDefault(false)
        );
    }
}
