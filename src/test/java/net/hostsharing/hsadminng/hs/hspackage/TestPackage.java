package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.hs.hscustomer.CustomerEntity;
import net.hostsharing.hsadminng.hs.hscustomer.TestCustomer;

import static java.util.UUID.randomUUID;

public class TestPackage {

    public static final PackageEntity xxx00 = hsPackage(TestCustomer.xxx, "xxx00");
    public static final PackageEntity xxx01 = hsPackage(TestCustomer.xxx, "xxx01");
    public static final PackageEntity xxx02 = hsPackage(TestCustomer.xxx, "xxx02");

    public static PackageEntity hsPackage(final CustomerEntity customer, final String name) {
        return new PackageEntity(randomUUID(), customer, name, "initial description of package " + name);
    }
}
