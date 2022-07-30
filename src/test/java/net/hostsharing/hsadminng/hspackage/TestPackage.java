package net.hostsharing.hsadminng.hspackage;

import net.hostsharing.hsadminng.hscustomer.CustomerEntity;
import net.hostsharing.hsadminng.hscustomer.TestCustomer;

import static java.util.UUID.randomUUID;

public class TestPackage {

    public static final PackageEntity xxx00 = hsPackage(TestCustomer.xxx, "xxx00");
    public static final PackageEntity xxx01 = hsPackage(TestCustomer.xxx, "xxx01");
    public static final PackageEntity xxx02 = hsPackage(TestCustomer.xxx, "xxx02");

    public static PackageEntity hsPackage(final CustomerEntity customer, final String name) {
        return new PackageEntity(randomUUID(), name, customer);
    }
}
