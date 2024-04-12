
package net.hostsharing.hsadminng.hs.office.coopshares;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "hs_office_coopsharestransaction")
@NoArgsConstructor
public class HsOfficeCoopSharesTransactionRawEntity {

    @Id
    private UUID uuid;
}
