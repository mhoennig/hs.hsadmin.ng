package net.hostsharing.hsadminng.hs.migration;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import org.hibernate.annotations.Type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@Entity
@Table(name = "hs_hosting_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HsHostingAssetRawEntity implements HsHostingAsset {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingitemuuid")
    private HsBookingItemEntity bookingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid")
    private HsHostingAssetRawEntity parentAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignedtoassetuuid")
    private HsHostingAssetRawEntity assignedToAsset;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsHostingAssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarmcontactuuid")
    private HsOfficeContactEntity alarmContact;

    @OneToMany(cascade = CascadeType.REFRESH, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid", referencedColumnName = "uuid")
    private List<HsHostingAssetRawEntity> subHostingAssets;

    @Column(name = "identifier")
    private String identifier; // e.g. vm1234, xyz00, example.org, xyz00_abc

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "config")
    private Map<String, Object> config = new HashMap<>();

    @Transient
    private PatchableMapWrapper<Object> configWrapper;

    @Transient
    private boolean isLoaded;

    @PostLoad
    public void markAsLoaded() {
        this.isLoaded = true;
    }

    public PatchableMapWrapper<Object> getConfig() {
        return PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper;}, config);
    }

    @Override
    public Map<String, Object> directProps() {
        return config;
    }

    @Override
    public String toString() {
        return stringify.using(HsHostingAssetRawEntity.class).apply(this);
    }
}
