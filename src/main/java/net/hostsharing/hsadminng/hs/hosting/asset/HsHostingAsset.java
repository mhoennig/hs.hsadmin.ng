package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;
import net.hostsharing.hsadminng.rbac.rbacobject.BaseEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

public interface HsHostingAsset extends Stringifyable, BaseEntity<HsHostingAsset>, PropertiesProvider {

    Stringify<HsHostingAsset> stringify = stringify(HsHostingAsset.class)
            .withProp(HsHostingAsset::getType)
            .withProp(HsHostingAsset::getIdentifier)
            .withProp(HsHostingAsset::getCaption)
            .withProp(HsHostingAsset::getParentAsset)
            .withProp(HsHostingAsset::getAssignedToAsset)
            .withProp(HsHostingAsset::getBookingItem)
            .withProp(HsHostingAsset::getConfig)
            .quotedValues(false);


    void setUuid(UUID uuid);
    HsHostingAssetType getType();
    HsHostingAsset getParentAsset();
    void setIdentifier(String s);
    String getIdentifier();
    HsBookingItemEntity getBookingItem();
    HsHostingAsset getAssignedToAsset();
    HsOfficeContactRealEntity getAlarmContact();
    List<? extends HsHostingAsset> getSubHostingAssets();
    String getCaption();
    Map<String, Object> getConfig();

    default HsBookingProjectEntity getRelatedProject() {
        return Optional.ofNullable(getBookingItem())
                .map(HsBookingItemEntity::getRelatedProject)
                .orElseGet(() -> Optional.ofNullable(getParentAsset())
                        .map(HsHostingAsset::getRelatedProject)
                        .orElse(null));
    }

    @Override
    default Object getContextValue(final String propName) {
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
    default String toShortString() {
        return getType() + ":" + getIdentifier();
    }
}
