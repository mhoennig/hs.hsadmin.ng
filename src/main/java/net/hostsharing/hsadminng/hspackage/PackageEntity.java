package net.hostsharing.hsadminng.hspackage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hostsharing.hsadminng.hscustomer.CustomerEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "package_rv")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PackageEntity {

    private @Id UUID uuid;

    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private CustomerEntity customer;
}
