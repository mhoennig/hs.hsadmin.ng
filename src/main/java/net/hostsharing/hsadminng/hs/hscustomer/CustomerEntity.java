package net.hostsharing.hsadminng.hs.hscustomer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "customer_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {
    private @Id UUID uuid;
    private String prefix;
    private int reference;
    private @Column(name="adminusername")String adminUserName;
}
