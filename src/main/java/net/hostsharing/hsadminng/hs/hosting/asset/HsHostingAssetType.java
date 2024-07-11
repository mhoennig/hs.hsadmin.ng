package net.hostsharing.hsadminng.hs.hosting.asset;

import lombok.AllArgsConstructor;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.Node;

import javax.naming.NamingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static net.hostsharing.hsadminng.hs.hosting.asset.EntityTypeRelation.*;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.OPTIONAL;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationPolicy.REQUIRED;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationType.ASSIGNED_TO_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationType.BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.RelationType.PARENT_ASSET;

public enum HsHostingAssetType implements Node {
    SAME_TYPE, // pseudo-type for recursive references

    CLOUD_SERVER( // named e.g. vm1234
            inGroup("Server"),
            requires(HsBookingItemType.CLOUD_SERVER)),

    MANAGED_SERVER( // named e.g. vm1234
            inGroup("Server"),
            requires(HsBookingItemType.MANAGED_SERVER)),

    MANAGED_WEBSPACE( // named eg. xyz00
            inGroup("Webspace"),
            requires(HsBookingItemType.MANAGED_WEBSPACE),
            optionalParent(MANAGED_SERVER)),

    UNIX_USER( // named e.g. xyz00-abc
            inGroup("Webspace"),
            requiredParent(MANAGED_WEBSPACE)),

    EMAIL_ALIAS( // named e.g. xyz00-abc
            inGroup("Webspace"),
            requiredParent(MANAGED_WEBSPACE)),

    DOMAIN_SETUP( // named e.g. example.org
            inGroup("Domain"),
            optionalParent(SAME_TYPE)
    ),

    DOMAIN_DNS_SETUP( // named e.g. example.org
            inGroup("Domain"),
            requiredParent(DOMAIN_SETUP),
            assignedTo(MANAGED_WEBSPACE)),

    DOMAIN_HTTP_SETUP( // named e.g. example.org
            inGroup("Domain"),
            requiredParent(DOMAIN_SETUP),
            assignedTo(UNIX_USER)),

    DOMAIN_SMTP_SETUP( // named e.g. example.org
            inGroup("Domain"),
            requiredParent(DOMAIN_SETUP),
            assignedTo(MANAGED_WEBSPACE)),

    DOMAIN_MBOX_SETUP( // named e.g. example.org
            inGroup("Domain"),
            requiredParent(DOMAIN_SETUP),
            assignedTo(MANAGED_WEBSPACE)),

    // TODO.spec: SECURE_MX

    EMAIL_ADDRESS( // named e.g. sample@example.org
            inGroup("Domain"),
            requiredParent(DOMAIN_MBOX_SETUP)),

    PGSQL_INSTANCE( // TODO.spec: identifier to be specified
            inGroup("PostgreSQL"),
            requiredParent(MANAGED_SERVER)),

    PGSQL_USER( // named e.g. xyz00_abc
            inGroup("PostgreSQL"),
            requiredParent(PGSQL_INSTANCE),
            assignedTo(MANAGED_WEBSPACE)),

    PGSQL_DATABASE( // named e.g. xyz00_abc
            inGroup("PostgreSQL"),
            requiredParent(MANAGED_WEBSPACE), // TODO.spec: or PGSQL_USER?
            assignedTo(PGSQL_INSTANCE)), // TODO.spec: or swapping parent+assignedTo?

    MARIADB_INSTANCE( // TODO.spec: identifier to be specified
            inGroup("MariaDB"),
            requiredParent(MANAGED_SERVER)), // TODO.spec: or MANAGED_WEBSPACE?

    MARIADB_USER( // named e.g. xyz00_abc
            inGroup("MariaDB"),
            requiredParent(MARIADB_INSTANCE),
            assignedTo(MANAGED_WEBSPACE)),

    MARIADB_DATABASE( // named e.g. xyz00_abc
            inGroup("MariaDB"),
            requiredParent(MANAGED_WEBSPACE), // TODO.spec: or MARIADB_USER?
            assignedTo(MARIADB_INSTANCE)), // TODO.spec: or swapping parent+assignedTo?

    IP_NUMBER(
            inGroup("Server"),
            assignedTo(CLOUD_SERVER),
            assignedTo(MANAGED_SERVER),
            assignedTo(MANAGED_WEBSPACE)
    );

    private final String groupName;
    private final EntityTypeRelation<?, ?>[] relations;

    HsHostingAssetType(
            final String groupName,
            final EntityTypeRelation<?, ?>... relations
            ) {
        this.groupName = groupName;
        this.relations = relations;
    }

    HsHostingAssetType() {
        this.groupName = null;
        this.relations = null;
    }

    /// just syntactic sugar
    private static String inGroup(final String groupName) {
        return groupName;
    }

    // TODO.refa: try to get rid of the following similar methods:

    public RelationPolicy bookingItemPolicy() {
        return stream(relations)
                .filter(r -> r.relationType == BOOKING_ITEM)
                .map(r -> r.relationPolicy)
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(RelationPolicy.FORBIDDEN);
    }

    public HsBookingItemType bookingItemType() {
        return stream(relations)
                .filter(r -> r.relationType == BOOKING_ITEM)
                .map(r -> HsBookingItemType.valueOf(r.relatedType(this).toString()))
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(null);
    }

    public RelationPolicy parentAssetPolicy() {
        return stream(relations)
                .filter(r -> r.relationType == PARENT_ASSET)
                .map(r -> r.relationPolicy)
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(RelationPolicy.FORBIDDEN);
    }

    public HsHostingAssetType parentAssetType() {
        return stream(relations)
                .filter(r -> r.relationType == PARENT_ASSET)
                .map(r -> HsHostingAssetType.valueOf(r.relatedType(this).toString()))
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(null);
    }

    public RelationPolicy assignedToAssetPolicy() {
        return stream(relations)
                .filter(r -> r.relationType == ASSIGNED_TO_ASSET)
                .map(r -> r.relationPolicy)
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(RelationPolicy.FORBIDDEN);
    }

    public HsHostingAssetType assignedToAssetType() {
        return stream(relations)
                .filter(r -> r.relationType == ASSIGNED_TO_ASSET)
                .map(r -> HsHostingAssetType.valueOf(r.relatedType(this).toString()))
                .reduce(HsHostingAssetType::onlyASingleElementExpectedException)
                .orElse(null);
    }

    private static <X> X onlyASingleElementExpectedException(Object a, Object b) {
        throw new IllegalStateException("Only a single element expected to match criteria.");
    }

    @Override
    public List<String> edges() {
        return stream(relations)
                .map(r -> nodeName() + r.edge + r.relatedType(this).nodeName())
                .toList();
    }

    @Override
    public String nodeName() {
        return "HA_" + name();
    }

    public static <T extends Enum<?>> HsHostingAssetType of(final T value) {
        return value == null ? null : valueOf(value.name());
    }

    static String asString(final HsHostingAssetType type) {
        return type == null ? null : type.name();
    }

    private static String renderAsPlantUML(final String caption, final Set<String> includedHostingGroups) {
        final String bookingNodes = stream(HsBookingItemType.values())
                .map(t -> "    entity " + t.nodeName())
                .collect(joining("\n"));
        final String hostingGroups = includedHostingGroups.stream().sorted()
                .map(HsHostingAssetType::generateGroup)
                .collect(joining("\n"));
        final String hostingAssetNodes = stream(HsHostingAssetType.values())
                .filter(t -> t.isInGroups(includedHostingGroups))
                .map(t -> "entity " + t.nodeName())
                .collect(joining("\n"));
        final String bookingItemEdges = stream(HsBookingItemType.values())
                .map(HsBookingItemType::edges)
                .flatMap(Collection::stream)
                .collect(joining("\n"));
        final String hostingAssetEdges = stream(HsHostingAssetType.values())
                .filter(t ->  t.isInGroups(includedHostingGroups))
                .map(HsHostingAssetType::edges)
                .flatMap(Collection::stream)
                .collect(joining("\n"));
        return """

                ### %{caption}

                ```plantuml
                @startuml
                left to right direction

                package Booking #feb28c {
                %{bookingNodes}
                }
                
                package Hosting #feb28c{
                %{hostingGroups}
                }

                %{bookingItemEdges}

                %{hostingAssetEdges}

                package Legend #white {
                    SUB_ENTITY1 *--> REQUIRED_PARENT_ENTITY
                    SUB_ENTITY2 *..> OPTIONAL_PARENT_ENTITY
                    ASSIGNED_ENTITY1 o--> REQUIRED_ASSIGNED_TO_ENTITY1
                    ASSIGNED_ENTITY2 o..> OPTIONAL_ASSIGNED_TO_ENTITY2
                }
                Booking -down[hidden]->Legend
                ```
                """
                    .replace("%{caption}", caption)
                    .replace("%{bookingNodes}", bookingNodes)
                    .replace("%{hostingGroups}", hostingGroups)
                    .replace("%{hostingAssetNodeStyles}", hostingAssetNodes)
                    .replace("%{bookingItemEdges}", bookingItemEdges)
                    .replace("%{hostingAssetEdges}", hostingAssetEdges);
    }

    private boolean isInGroups(final Set<String> assetGroups) {
        return groupName != null && assetGroups.contains(groupName);
    }

    private static String generateGroup(final String group) {
        return "    package " + group + " #99bcdb {\n"
                + stream(HsHostingAssetType.values())
                .filter(t -> group.equals(t.groupName))
                .map(t -> "        entity " + t.nodeName())
                .collect(joining("\n"))
                + "\n    }\n";
    }

    static String renderAsEmbeddedPlantUml() {

        final var markdown = new StringBuilder("""
                ## HostingAsset Type Structure

                """);

        // rendering all types in a single diagram is currently ignored
        renderAsPlantUML("Domain", stream(HsHostingAssetType.values())
                .filter(t -> t.groupName != null)
                .map(t -> t.groupName)
                .collect(toSet()));

        markdown.append(renderAsPlantUML("Domain", Set.of("Domain", "Webspace", "Server")))
                .append(renderAsPlantUML("MariaDB", Set.of("MariaDB", "Webspace", "Server")))
                .append(renderAsPlantUML("PostgreSQL", Set.of("PostgreSQL", "Webspace", "Server")));

        markdown.append("""

                This code generated was by %{this}.main, do not amend manually.
                """
                        .replace("%{this}", HsHostingAssetType.class.getSimpleName()));

        return markdown.toString();
    }

    public static void main(final String[] args) throws IOException, NamingException {
        Files.writeString(
                Path.of("doc/hs-hosting-asset-type-structure.md"),
                renderAsEmbeddedPlantUml(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public enum RelationPolicy {
        FORBIDDEN, OPTIONAL, REQUIRED
    }

    public enum RelationType {
        BOOKING_ITEM,
                PARENT_ASSET,
                ASSIGNED_TO_ASSET
    }
}

@AllArgsConstructor
class EntityTypeRelation<E, T extends Node> {

    final HsHostingAssetType.RelationPolicy relationPolicy;
    final HsHostingAssetType.RelationType relationType;
    final Function<HsHostingAssetEntity, E> getter;
    private final T relatedType;
    final String edge;

    public T relatedType(final HsHostingAssetType referringType) {
        //noinspection unchecked
        return relatedType == HsHostingAssetType.SAME_TYPE ? (T) referringType : relatedType;
    }

    static EntityTypeRelation<HsBookingItemEntity, HsBookingItemType> requires(final HsBookingItemType bookingItemType) {
        return new EntityTypeRelation<>(REQUIRED, BOOKING_ITEM, HsHostingAssetEntity::getBookingItem, bookingItemType, " *==> ");
    }

    static EntityTypeRelation<HsHostingAssetEntity, HsHostingAssetType> optionalParent(final HsHostingAssetType hostingAssetType) {
        return new EntityTypeRelation<>(OPTIONAL, PARENT_ASSET, HsHostingAssetEntity::getParentAsset, hostingAssetType, " o..> ");
    }

    static EntityTypeRelation<HsHostingAssetEntity, HsHostingAssetType> requiredParent(final HsHostingAssetType hostingAssetType) {
        return new EntityTypeRelation<>(REQUIRED, PARENT_ASSET, HsHostingAssetEntity::getParentAsset, hostingAssetType, " *==> ");
    }

    static EntityTypeRelation<HsHostingAssetEntity, HsHostingAssetType> assignedTo(final HsHostingAssetType hostingAssetType) {
        return new EntityTypeRelation<>(REQUIRED, ASSIGNED_TO_ASSET, HsHostingAssetEntity::getAssignedToAsset, hostingAssetType, " o..> ");
    }
}
