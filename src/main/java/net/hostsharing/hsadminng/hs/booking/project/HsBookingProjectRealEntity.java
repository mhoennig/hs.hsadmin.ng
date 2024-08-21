package net.hostsharing.hsadminng.hs.booking.project;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "hs_booking_project")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
public class HsBookingProjectRealEntity extends HsBookingProject {
}
