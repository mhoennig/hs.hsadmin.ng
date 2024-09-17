package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.*;

// TODO: cleanup - this code was 'hacked' to quickly fix a specific problem, needs refactoring
@Service
public class RbacGrantsDiagramService {

    private static final int GRANT_LIMIT = 500;

    public static void writeToFile(final String title, final String graph, final String fileName) {

        new File("doc/temp").mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("""
                    ### all grants to %s
                                        
                    ```mermaid
                    %s
                    ```
                    """.formatted(title, graph));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Include {
        DETAILS,
        USERS,
        PERMISSIONS,
        NOT_ASSUMED,
        TEST_ENTITIES,
        NON_TEST_ENTITIES;

        public static final EnumSet<Include> ALL = EnumSet.allOf(Include.class);
        public static final EnumSet<Include> ALL_TEST_ENTITY_RELATED = EnumSet.of(USERS, DETAILS, NOT_ASSUMED, TEST_ENTITIES, PERMISSIONS);
        public static final EnumSet<Include> ALL_NON_TEST_ENTITY_RELATED = EnumSet.of(USERS, DETAILS, NOT_ASSUMED, NON_TEST_ENTITIES, PERMISSIONS);
    }

    @Autowired
    private Context context;

    @Autowired
    private RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    private EntityManager em;

    private Map<UUID, List<RawRbacGrantEntity>> descendantsByUuid = new HashMap<>();

    public String allGrantsTocurrentSubject(final EnumSet<Include> includes) {
        final var graph = new LimitedHashSet<RawRbacGrantEntity>();
        for ( UUID subjectUuid: context.fetchCurrentSubjectOrAssumedRolesUuids() ) {
            traverseGrantsTo(graph, subjectUuid, includes);
            }
        return toMermaidFlowchart(graph, includes);
    }

    private void traverseGrantsTo(final Set<RawRbacGrantEntity> graph, final UUID refUuid, final EnumSet<Include> includes) {
        final var grants = rawGrantRepo.findByAscendingUuid(refUuid);
        grants.forEach(g -> {
            if (!includes.contains(PERMISSIONS) && g.getDescendantIdName().startsWith("perm:")) {
                return;
            }
            if ( !g.getDescendantIdName().startsWith("role:rbac.global")) {
                if (!includes.contains(TEST_ENTITIES) && g.getDescendantIdName().contains(":rbactest.")) {
                    return;
                }
                if (!includes.contains(NON_TEST_ENTITIES) && !g.getDescendantIdName().contains(":rbactest.")) {
                    return;
                }
            }
            graph.add(g);
            if (includes.contains(NOT_ASSUMED) || g.isAssumed()) {
                traverseGrantsTo(graph, g.getDescendantUuid(), includes);
            }
        });
    }

    public String allGrantsFrom(final UUID targetObject, final String op, final EnumSet<Include> includes) {
        final var refUuid = (UUID) em.createNativeQuery("SELECT uuid FROM rbac.permission WHERE objectuuid=:targetObject AND op=:op")
                .setParameter("targetObject", targetObject)
                .setParameter("op", op)
                .getSingleResult();
        final var graph = new LimitedHashSet<RawRbacGrantEntity>();
        traverseGrantsFrom(graph, refUuid, includes);
        return toMermaidFlowchart(graph, includes);
    }

    private void traverseGrantsFrom(final Set<RawRbacGrantEntity> graph, final UUID refUuid, final EnumSet<Include> option) {
        final var grants = findDescendantsByUuid(refUuid);
        grants.forEach(g -> {
            if (!option.contains(USERS) && g.getAscendantIdName().startsWith("user:")) {
                return;
            }
            graph.add(g);
            if (option.contains(NOT_ASSUMED) || g.isAssumed()) {
                traverseGrantsFrom(graph, g.getAscendingUuid(), option);
            }
        });
    }

    private List<RawRbacGrantEntity> findDescendantsByUuid(final UUID refUuid) {
        // TODO.impl: if that UUID already got processed, do we need to return anything at all?
        return descendantsByUuid.computeIfAbsent(refUuid, uuid -> rawGrantRepo.findByDescendantUuid(uuid));
    }

    private String toMermaidFlowchart(final HashSet<RawRbacGrantEntity> graph, final EnumSet<Include> includes) {
        final var entities =
                includes.contains(DETAILS)
                ? graph.stream()
                    .flatMap(g -> Stream.of(
                            new Node(g.getAscendantIdName(), g.getAscendingUuid()),
                            new Node(g.getDescendantIdName(), g.getDescendantUuid()))
                    )
                    .collect(groupingBy(RbacGrantsDiagramService::renderEntityIdName))
                    .entrySet().stream()
                    .map(entity -> "subgraph " + cleanId(entity.getKey()) + renderSubgraph(entity.getKey()) + "\n\n    "
                            + entity.getValue().stream()
                            .map(n -> renderNode(n.idName(), n.uuid()).replace("\n", "\n    "))
                            .sorted()
                            .distinct()
                            .collect(joining("\n\n    ")))
                    .collect(joining("\n\nend\n\n"))
                        + "\n\nend\n\n"
                : "";

        final var grants = graph.stream()
                .map(g -> cleanId(g.getAscendantIdName())
                        + " -->" + (g.isAssumed() ? " " : "|XX| ")
                        + cleanId(g.getDescendantIdName()))
                .sorted()
                .collect(joining("\n"));

        final var avoidCroppedNodeLabels = "%%{init:{'flowchart':{'htmlLabels':false}}}%%\n\n";
        return (includes.contains(DETAILS) ? avoidCroppedNodeLabels : "")
                + (graph.size() >= GRANT_LIMIT ? "%% too many grants, graph is cropped\n" : "")
                + "flowchart TB\n\n"
                + entities
                + grants;
    }

    private String renderSubgraph(final String entityId) {
        // this does not work according to Mermaid bug https://github.com/mermaid-js/mermaid/issues/3806
        //        if (entityId.contains("#")) {
        //            final var parts = entityId.split("#");
        //            final var table = parts[0];
        //            final var entity = parts[1];
        //            if (table.equals("entity")) {
        //                return "[" + entity "]";
        //            }
        //            return "[" + table + "\n" + entity + "]";
        //        }
        return "[" + cleanId(entityId) + "]";
    }

    private static String renderEntityIdName(final Node node) {
        final var refType = refType(node.idName());
        if (refType.equals("user")) {
            return "users";
        }
        if (refType.equals("perm")) {
            return node.idName().split(":", 3)[1];
        }
        if (refType.equals("role")) {
            final var withoutRolePrefix = node.idName().substring("role:".length());
            return withoutRolePrefix.substring(0, withoutRolePrefix.lastIndexOf(':'));
        }
        throw new IllegalArgumentException("unknown refType '" + refType + "' in '" + node.idName() + "'");
    }

    private String renderNode(final String idName, final UUID uuid) {
        return cleanId(idName) + renderNodeContent(idName, uuid);
    }

    private String renderNodeContent(final String idName, final UUID uuid) {
        final var refType = refType(idName);

        if (refType.equals("user")) {
            final var displayName = idName.substring(refType.length()+1);
            return "(" + displayName + "\nref:" + uuid + ")";
        }
        if (refType.equals("role")) {
            final var roleType = idName.substring(idName.lastIndexOf(':') + 1);
            return "[" + roleType + "\nref:" + uuid + "]";
        }
        if (refType.equals("perm")) {
            final var parts = idName.split(":");
            final var permType = parts[2];
            return "{{" + permType + "\nref:" + uuid + "}}";
        }
        return "";
    }

    private static String refType(final String idName) {
        return idName.split(":", 2)[0];
    }

    @NotNull
    private static String cleanId(final String idName) {
        return idName.replaceAll("@.*", "")
                .replace("[", "").replace("]", "").replace("(", "").replace(")", "").replace(",", "").replace(">", ":").replace("|", "_");
    }


    static class LimitedHashSet<T> extends HashSet<T> {

        @Override
        public boolean add(final T t) {
            if (size() < GRANT_LIMIT ) {
                return super.add(t);
            } else {
                return false;
            }
        }
    }

}

record Node(String idName, UUID uuid) {

}
