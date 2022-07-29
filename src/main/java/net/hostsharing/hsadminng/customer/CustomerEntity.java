package net.hostsharing.hsadminng.customer;

import lombok.Getter;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "customer_rv")
@Getter
public class CustomerEntity {
    private @Id UUID uuid;
    private String prefix;
    private int reference;
    private @Column(name="adminusername")String adminUserName;
}
