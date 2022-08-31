package net.hostsharing.hsadminng.test.pac;

import net.hostsharing.hsadminng.test.cust.TestCustomer;
import net.hostsharing.hsadminng.test.cust.TestCustomerEntity;

import static java.util.UUID.randomUUID;

public class TestPackage {

    public static final TestPackageEntity xxx00 = hsPackage(TestCustomer.xxx, "xxx00");
    public static final TestPackageEntity xxx01 = hsPackage(TestCustomer.xxx, "xxx01");
    public static final TestPackageEntity xxx02 = hsPackage(TestCustomer.xxx, "xxx02");

    public static TestPackageEntity hsPackage(final TestCustomerEntity customer, final String name) {
        return new TestPackageEntity(randomUUID(), 0, customer, name, "initial description of package " + name);
    }
}
