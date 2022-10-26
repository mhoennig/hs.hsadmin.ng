package net.hostsharing.hsadminng.test.cust;

import static java.util.UUID.randomUUID;

public class TestCustomer {

    public static final TestCustomerEntity xxx = hsCustomer("xxx", 10001, "xxx@example.com");
    static final TestCustomerEntity yyy = hsCustomer("yyy", 10002, "yyy@example.com");

    static public TestCustomerEntity hsCustomer(final String prefix, final int reference, final String adminName) {
        return new TestCustomerEntity(null, prefix, reference, adminName);
    }
}
