package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProject;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
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
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.lowerInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.upperInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(builderMethodName = "baseBuilder", toBuilder = true)
public abstract class HsBookingItem implements Stringifyable, BaseEntity<HsBookingItem>, PropertiesProvider {

    private static Stringify<HsBookingItem> stringify = stringify(HsBookingItem.class)
            .withProp(HsBookingItem::getType)
            .withProp(HsBookingItem::getCaption)
            .withProp(HsBookingItem::getProject)
            .withProp(e -> e.getValidity().asString())
            .withProp(HsBookingItem::getResources)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectuuid")
    private HsBookingProjectRealEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentitemuuid")
    private HsBookingItemRealEntity parentItem;

    @NotNull
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsBookingItemType type;

    @Builder.Default
    @Type(PostgreSQLRangeType.class)
    @Column(name = "validity", columnDefinition = "daterange")
    private Range<LocalDate> validity = Range.closedInfinite(LocalDate.now());

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "resources")
    private Map<String, Object> resources = new HashMap<>();

    @OneToMany(cascade = CascadeType.REFRESH, orphanRemoval = true)
    @JoinColumn(name = "parentitemuuid", referencedColumnName = "uuid")
    private List<HsBookingItemRealEntity> subBookingItems;

    @Transient
    private PatchableMapWrapper<Object> resourcesWrapper;

    @Transient
    private boolean isLoaded;

    @PostLoad
    public void markAsLoaded() {
        this.isLoaded = true;
    }

    public PatchableMapWrapper<Object> getResources() {
        return PatchableMapWrapper.of(resourcesWrapper, (newWrapper) -> {resourcesWrapper = newWrapper;}, resources);
    }

    public void putResources(Map<String, Object> newResources) {
        getResources().assign(newResources);
    }

    public void setValidFrom(final LocalDate validFrom) {
        setValidity(toPostgresDateRange(validFrom, getValidTo()));
    }

    public void setValidTo(final LocalDate validTo) {
        setValidity(toPostgresDateRange(getValidFrom(), validTo));
    }

    public LocalDate getValidFrom() {
        return lowerInclusiveFromPostgresDateRange(getValidity());
    }

    public LocalDate getValidTo() {
        return upperInclusiveFromPostgresDateRange(getValidity());
    }

    @Override
    public PatchableMapWrapper<Object> directProps() {
        return getResources();
    }

    @Override
    public Object getContextValue(final String propName) {
        final var v = resources.get(propName);
        if (v != null) {
            return v;
        }
        if (parentItem != null) {
            return parentItem.getResources().get(propName);
        }
        return emptyMap();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(getRelatedProject()).map(HsBookingProject::toShortString).orElse("D-???????-?") +
                ":" + caption;
    }

    public HsBookingProject getRelatedProject() {
        return project != null ? project
                : parentItem != null ? parentItem.getRelatedProject()
                : null; // can be the case for technical assets like IP-numbers
    }
}
