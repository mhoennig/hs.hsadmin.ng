package net.hostsharing.hsadminng.test.pac;

import net.hostsharing.hsadminng.test.cust.TestCustomer;
import net.hostsharing.hsadminng.test.cust.TestCustomerEntity;

import static java.util.UUID.randomUUID;

public class TestPackage {

    public static final PackageEntity xxx00 = hsPackage(TestCustomer.xxx, "xxx00");
    public static final PackageEntity xxx01 = hsPackage(TestCustomer.xxx, "xxx01");
    public static final PackageEntity xxx02 = hsPackage(TestCustomer.xxx, "xxx02");

    public static PackageEntity hsPackage(final TestCustomerEntity customer, final String name) {
        return new PackageEntity(randomUUID(), 0, customer, name, "initial description of package " + name);
    }
}
