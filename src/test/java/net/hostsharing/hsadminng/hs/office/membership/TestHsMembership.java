package net.hostsharing.hsadminng.hs.office.membership;

import io.hypersistence.utils.hibernate.type.range.Range;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.partner.HsOfficeTestRealPartner.TEST_PARTNER;

public class TestHsMembership {

    public static final HsOfficeMembershipEntity TEST_MEMBERSHIP =
            HsOfficeMembershipEntity.builder()
                    .partner(TEST_PARTNER)
                    .memberNumberSuffix("01")
                    .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                    .build();
}
