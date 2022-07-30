package net.hostsharing.hsadminng.hscustomer;

import static java.util.UUID.randomUUID;

public class TestCustomer {

    static final CustomerEntity xxx = customer("xxx", 10001, "xxx@example.com");
    static final CustomerEntity yyy = customer("yyy", 10002, "yyy@example.com");


    static public CustomerEntity customer(final String prefix, final int reference, final String adminName) {
        return new CustomerEntity(randomUUID(), prefix, reference, adminName);
    }
}
