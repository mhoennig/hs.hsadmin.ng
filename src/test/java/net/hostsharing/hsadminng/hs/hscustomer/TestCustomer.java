package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.hs.hscustomer.CustomerEntity;

import static java.util.UUID.randomUUID;

public class TestCustomer {

    public static final CustomerEntity xxx = hsCustomer("xxx", 10001, "xxx@example.com");
    static final CustomerEntity yyy = hsCustomer("yyy", 10002, "yyy@example.com");


    static public CustomerEntity hsCustomer(final String prefix, final int reference, final String adminName) {
        return new CustomerEntity(randomUUID(), prefix, reference, adminName);
    }
}
