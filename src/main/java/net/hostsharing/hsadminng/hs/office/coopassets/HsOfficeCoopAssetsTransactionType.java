package net.hostsharing.hsadminng.hs.office.coopassets;

public enum HsOfficeCoopAssetsTransactionType {
    /**
     * correction of wrong bookings, value can be positive or negative
     */
    ADJUSTMENT,

    /**
     * payment received from member after signing shares, value >0
     */
    DEPOSIT,

    /**
     * payment send to member after cancellation of shares, value <0
     */
    DISBURSAL,

    /**
     * transferring shares to another member, value <0
     */
    TRANSFER,

    /**
     * receiving shares from another member, value >0
     */
    ADOPTION,

    /**
     * settlement with members dept, value <0
     */
    CLEARING,

    /**
     * assignment of balance sheet loss in case of cancellation of shares, value <0
     */
    LOSS,

    /**
     * limitation period was reached after impossible disbursal, value <0
     */
    LIMITATION
}
