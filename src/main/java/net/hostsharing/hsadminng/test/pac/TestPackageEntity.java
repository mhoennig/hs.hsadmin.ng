package net.hostsharing.hsadminng.test.pac;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.test.cust.TestCustomerEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_package_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPackageEntity {

    private @Id UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private TestCustomerEntity customer;

    private String name;

    private String description;
}
