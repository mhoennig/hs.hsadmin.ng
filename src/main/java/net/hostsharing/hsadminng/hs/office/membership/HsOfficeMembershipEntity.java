package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;
import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.PostgresDateRange.*;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_membership_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Membership")
public class HsOfficeMembershipEntity implements Stringifyable {

    private static Stringify<HsOfficeMembershipEntity> stringify = stringify(HsOfficeMembershipEntity.class)
            .withProp(HsOfficeMembershipEntity::getMemberNumber)
            .withProp(e -> e.getPartner().toShortString())
            .withProp(e -> e.getMainDebitor().toShortString())
            .withProp(e -> e.getValidity().asString())
            .withProp(HsOfficeMembershipEntity::getReasonForTermination)
            .withSeparator(", ")
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    @ManyToOne
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "maindebitoruuid")
    private HsOfficeDebitorEntity mainDebitor;

    @Column(name = "membernumber")
    private int memberNumber;

    @Column(name = "validity", columnDefinition = "daterange")
    @Type(PostgreSQLRangeType.class)
    private Range<LocalDate> validity;

    @Column(name = "reasonfortermination")
    @Enumerated(EnumType.STRING)
    //@Type(PostgreSQLEnumType.class)
    private HsOfficeReasonForTermination reasonForTermination;

    public void setValidFrom(final LocalDate validFrom) {
        setValidity(toPostgresDateRange(validFrom, getValidTo()));
    }

    public void setValidTo(final LocalDate validTo) {
        setValidity(toPostgresDateRange(getValidFrom(), validTo));
    }

    public LocalDate getValidFrom() {
        return lowerInclusiveFromPostgresDateRange(getValidity());
    }

    public LocalDate getValidTo() {
        return upperInclusiveFromPostgresDateRange(getValidity());
    }


    public Range<LocalDate> getValidity() {
        if (validity == null) {
            validity = Range.infinite(LocalDate.class);
        }
        ;
        return validity;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return String.valueOf(memberNumber);
    }

    @PrePersist
    void init() {
        if (getReasonForTermination() == null) {
            setReasonForTermination(HsOfficeReasonForTermination.NONE);
        }
    }
}
