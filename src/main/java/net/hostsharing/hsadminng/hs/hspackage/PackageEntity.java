package net.hostsharing.hsadminng.hs.hspackage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "package_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PackageEntity {

    private @Id UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private CustomerEntity customer;

    private String name;

    private String description;
}
