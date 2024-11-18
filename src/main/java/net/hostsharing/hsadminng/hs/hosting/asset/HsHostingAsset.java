package net.hostsharing.hsadminng.hs.hosting.asset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProject;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(builderMethodName = "baseBuilder", toBuilder = true)
public abstract class HsHostingAsset implements Stringifyable, BaseEntity<HsHostingAsset>, PropertiesProvider {

    static Stringify<HsHostingAsset> stringify = stringify(HsHostingAsset.class)
            .withProp(HsHostingAsset::getType)
            .withProp(HsHostingAsset::getIdentifier)
            .withProp(HsHostingAsset::getCaption)
            .withProp(HsHostingAsset::getParentAsset)
            .withProp(HsHostingAsset::getAssignedToAsset)
            .withProp(HsHostingAsset::getBookingItem)
            .withProp(HsHostingAsset::getConfig)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingitemuuid")
    private HsBookingItemRealEntity bookingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid")
    private HsHostingAssetRealEntity parentAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignedtoassetuuid")
    private HsHostingAssetRealEntity assignedToAsset;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsHostingAssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarmcontactuuid")
    private HsOfficeContactRealEntity alarmContact;

    @OneToMany(cascade = CascadeType.REFRESH, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid", referencedColumnName = "uuid")
    private List<HsHostingAssetRealEntity> subHostingAssets;

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

    public void putConfig(Map<String, Object> newConfig) {
        PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper;}, config).assign(newConfig);
    }

    public List<HsHostingAssetRealEntity> getSubHostingAssets() {
        if (subHostingAssets == null) {
            subHostingAssets = new ArrayList<>();
        }
        return subHostingAssets;
    }

    @Override
    public PatchableMapWrapper<Object> directProps() {
        return getConfig();
    }

    public HsBookingProject getRelatedProject() {
        return Optional.ofNullable(getBookingItem())
                .map(HsBookingItem::getRelatedProject)
                .orElseGet(() -> Optional.ofNullable(getParentAsset())
                        .map(HsHostingAsset::getRelatedProject)
                        .orElse(null));
    }

    @Override
    public Object getContextValue(final String propName) {
        final var v = directProps().get(propName);
        if (v != null) {
            return v;
        }

        if (getBookingItem() != null) {
            return getBookingItem().getResources().get(propName);
        }
        if (getParentAsset() != null && getParentAsset().getBookingItem() != null) {
            return getParentAsset().getBookingItem().getResources().get(propName);
        }
        return emptyMap();
    }

    @Override
    public String toShortString() {
        return getType() + ":" + getIdentifier();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }
}
