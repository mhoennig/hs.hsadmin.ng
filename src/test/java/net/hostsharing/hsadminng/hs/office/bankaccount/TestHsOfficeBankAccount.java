package net.hostsharing.hsadminng.hs.office.bankaccount;

import java.util.UUID;

public class TestHsOfficeBankAccount {

    public static final HsOfficeBankAccountEntity someBankAccount =
            hsOfficeBankAccount("some bankaccount", "DE67500105173931168623", "INGDDEFFXXX");

    static public HsOfficeBankAccountEntity hsOfficeBankAccount(final String holder, final String iban, final String bic) {
        return HsOfficeBankAccountEntity.builder()
                .uuid(UUID.randomUUID())
                .holder(holder)
                .iban(iban)
                .bic(bic)
                .build();
    }
}
