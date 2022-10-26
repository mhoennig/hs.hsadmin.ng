package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;

import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;

public class TestHsMembership {

    public static final HsOfficeMembershipEntity TEST_MEMBERSHIP =
            HsOfficeMembershipEntity.builder()
                    .partner(TEST_PARTNER)
                    .memberNumber(300001)
                    .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                    .build();
}
