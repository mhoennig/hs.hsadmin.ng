package net.hostsharing.hsadminng.hscustomer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "customer_rv")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {
    private @Id UUID uuid;
    private String prefix;
    private int reference;
    private @Column(name="adminusername")String adminUserName;
}
