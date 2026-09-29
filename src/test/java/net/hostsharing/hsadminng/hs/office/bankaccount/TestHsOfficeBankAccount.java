package net.hostsharing.hsadminng.hs.office.bankaccount;


public class TestHsOfficeBankAccount {

    public static final HsOfficeBankAccountEntity TEST_BANK_ACCOUNT =
            hsOfficeBankAccount("some bankaccount", "DE98990300003931168623", "DEMRDEF0");

    static public HsOfficeBankAccountEntity hsOfficeBankAccount(final String holder, final String iban, final String bic) {
        return HsOfficeBankAccountEntity.builder()
                .holder(holder)
                .iban(iban)
                .bic(bic)
                .build();
    }
}
