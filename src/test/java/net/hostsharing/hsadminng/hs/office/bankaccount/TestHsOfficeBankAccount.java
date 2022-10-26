package net.hostsharing.hsadminng.hs.office.bankaccount;

import java.util.UUID;

public class TestHsOfficeBankAccount {

    public static final HsOfficeBankAccountEntity TEST_BANK_ACCOUNT =
            hsOfficeBankAccount("some bankaccount", "DE67500105173931168623", "INGDDEFFXXX");

    static public HsOfficeBankAccountEntity hsOfficeBankAccount(final String holder, final String iban, final String bic) {
        return HsOfficeBankAccountEntity.builder()
                .holder(holder)
                .iban(iban)
                .bic(bic)
                .build();
    }
}
