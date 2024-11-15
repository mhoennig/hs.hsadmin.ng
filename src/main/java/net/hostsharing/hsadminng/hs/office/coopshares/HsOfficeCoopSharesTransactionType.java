package net.hostsharing.hsadminng.hs.office.coopshares;

public enum HsOfficeCoopSharesTransactionType {
    /**
     * reversal of wrong bookings, with either positive or negative value identical to reversed transaction
     */
    REVERSAL,

    /**
     * shares signed, e.g. with the declaration of accession, value >0
     */
    SUBSCRIPTION,

    /**
     * shares terminated, e.g. when a membership is resigned, value <0
     */
    CANCELLATION;
}
