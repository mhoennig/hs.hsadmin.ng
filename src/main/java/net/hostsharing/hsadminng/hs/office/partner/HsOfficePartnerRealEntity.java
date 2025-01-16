package net.hostsharing.hsadminng.hs.office.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(schema = "hs_office", name = "partner")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@DisplayAs("RealPartner")
public class HsOfficePartnerRealEntity extends HsOfficePartner<HsOfficePartnerRealEntity> {
}
