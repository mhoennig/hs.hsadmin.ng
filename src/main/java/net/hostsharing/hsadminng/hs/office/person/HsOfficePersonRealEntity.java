package net.hostsharing.hsadminng.hs.office.person;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(schema = "hs_office", name = "person")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants
@DisplayAs("RealPerson")
public class HsOfficePersonRealEntity extends HsOfficePerson<HsOfficePersonRealEntity> {
}
