package net.hostsharing.hsadminng.hs.office.relation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(schema = "hs_office", name = "relation")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@DisplayAs("RealRelation")
public class HsOfficeRelationRealEntity extends HsOfficeRelation {
}
