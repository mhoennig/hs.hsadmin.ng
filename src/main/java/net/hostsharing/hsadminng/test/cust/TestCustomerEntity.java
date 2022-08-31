package net.hostsharing.hsadminng.test.cust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_customer_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestCustomerEntity {
    private @Id UUID uuid;
    private String prefix;
    private int reference;
    private @Column(name="adminusername")String adminUserName;
}
