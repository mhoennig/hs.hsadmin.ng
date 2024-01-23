package net.hostsharing.hsadminng.hs.office.coopshares;

public enum HsOfficeCoopSharesTransactionType {
    ADJUSTMENT,     // correction of wrong bookings
    SUBSCRIPTION,   // shares signed, e.g. with the declaration of accession, >0
    CANCELLATION;   // shares terminated, e.g. when a membership is resigned, <0
}
