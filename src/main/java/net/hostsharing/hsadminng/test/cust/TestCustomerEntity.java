package net.hostsharing.hsadminng.test.cust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_customer_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestCustomerEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    private String prefix;
    private int reference;

    @Column(name="adminusername")
    private String adminUserName;
}
