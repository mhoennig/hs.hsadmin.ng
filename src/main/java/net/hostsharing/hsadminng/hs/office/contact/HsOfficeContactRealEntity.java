package net.hostsharing.hsadminng.hs.office.contact;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "hs_office", name = "contact")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@DisplayAs("RealContact")
public class HsOfficeContactRealEntity extends HsOfficeContact {

}
