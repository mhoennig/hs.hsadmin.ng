package net.hostsharing.hsadminng.hs.office.coopassets;

public enum HsOfficeCoopAssetsTransactionType {
    ADJUSTMENT, // correction of wrong bookings
    DEPOSIT,    // payment received from member after signing shares, >0
    DISBURSAL,  // payment send to member after cancellation of shares, <0
    TRANSFER,   // transferring shares to another member, <0
    ADOPTION,   // receiving shares from another member, >0
    CLEARING,   // settlement with members dept, <0
    LOSS,       // assignment of balance sheet loss in case of cancellation of shares, <0
    LIMITATION  // limitation period was reached after impossible disbursal, <0
}
