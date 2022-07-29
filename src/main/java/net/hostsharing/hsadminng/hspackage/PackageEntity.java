package net.hostsharing.hsadminng.hspackage;

import lombok.Getter;
import net.hostsharing.hsadminng.hscustomer.CustomerEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "package_rv")
@Getter
public class PackageEntity {

    private @Id UUID uuid;

    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private CustomerEntity customer;
}
