package net.hostsharing.hsadminng.test.pac;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.test.cust.TestCustomerEntity;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_package_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPackageEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private TestCustomerEntity customer;

    private String name;

    private String description;
}
