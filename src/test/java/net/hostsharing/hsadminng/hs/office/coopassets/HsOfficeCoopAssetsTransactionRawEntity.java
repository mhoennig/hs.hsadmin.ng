
package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "hs_office_coopassetstransaction")
@NoArgsConstructor
public class HsOfficeCoopAssetsTransactionRawEntity {

    @Id
    private UUID uuid;
}
