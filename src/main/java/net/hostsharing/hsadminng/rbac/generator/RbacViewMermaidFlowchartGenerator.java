package net.hostsharing.hsadminng.rbac.generator;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.rbac.generator.RbacView.CaseDef;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition.GrantType.*;

public class RbacViewMermaidFlowchartGenerator {

    public static final String HOSTSHARING_DARK_ORANGE = "#dd4901";
    public static final String HOSTSHARING_LIGHT_ORANGE = "#feb28c";
    public static final String HOSTSHARING_DARK_BLUE = "#274d6e";
    public static final String HOSTSHARING_LIGHT_BLUE = "#99bcdb";
    private final RbacView rbacDef;

    private final List<RbacView.EntityAlias> usedEntityAliases;

    private final CaseDef forCase;
    private final StringWriter flowchart = new StringWriter();

    public RbacViewMermaidFlowchartGenerator(final RbacView rbacDef, final CaseDef forCase) {
        this.rbacDef = rbacDef;
        this.forCase = forCase;

        usedEntityAliases = rbacDef.getGrantDefs().stream()
                        .flatMap(g -> Stream.of(
                                g.getSuperRoleDef() != null ? g.getSuperRoleDef().getEntityAlias() : null,
                                g.getSubRoleDef() != null ? g.getSubRoleDef().getEntityAlias() : null,
                                g.getPermDef() != null ? g.getPermDef().getEntityAlias() : null))
                        .filter(Objects::nonNull)
                        .sorted(comparing(RbacView.EntityAlias::aliasName))
                        .distinct()
                        .filter(rbacDef::renderInDiagram)
                        .collect(Collectors.toList());

        flowchart.writeLn("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                """);
        renderEntitySubgraphs();
        renderGrants();
    }

    public RbacViewMermaidFlowchartGenerator(final RbacView rbacDef) {
       this(rbacDef, null);
    }
    private void renderEntitySubgraphs() {
        usedEntityAliases.stream()
                .filter(entityAlias -> !rbacDef.isEntityAliasProxy(entityAlias))
                .filter(entityAlias -> !entityAlias.isPlaceholder())
                .filter(rbacDef::renderInDiagram)
                .forEach(this::renderEntitySubgraph);
    }

    private void renderEntitySubgraph(final RbacView.EntityAlias entity) {
        if (!rbacDef.renderInDiagram(entity)) {
            return;
        }

        final var color = rbacDef.isRootEntityAlias(entity) ? HOSTSHARING_DARK_ORANGE
                : entity.isSubEntity() ? HOSTSHARING_LIGHT_ORANGE
                : HOSTSHARING_LIGHT_BLUE;
        flowchart.writeLn("""            
            subgraph %{aliasName}["`**%{aliasName}**`"]
                direction TB
                style %{aliasName} fill:%{fillColor},stroke:%{strokeColor},stroke-width:8px
            """
            .replace("%{aliasName}", entity.aliasName())
            .replace("%{fillColor}", color )
            .replace("%{strokeColor}", HOSTSHARING_DARK_BLUE ));

        flowchart.indented( () -> {
            usedEntityAliases.stream()
                    .filter(e -> e.aliasName().startsWith(entity.aliasName() + ":"))
                    .forEach(this::renderEntitySubgraph);

            wrapOutputInSubgraph(entity.aliasName() + ":roles", color,
                    rbacDef.getRoleDefs().stream()
                    .filter(r -> r.getEntityAlias() == entity)
                    .map(this::roleDef)
                    .collect(joining("\n")));

            wrapOutputInSubgraph(entity.aliasName() + ":permissions", color,
                rbacDef.getPermDefs().stream()
                        .filter(p -> p.getEntityAlias() == entity)
                        .map(this::permDef)
                        .collect(joining("\n")));

            if (rbacDef.isRootEntityAlias(entity) && rbacDef.getRootEntityAliasProxy() != null ) {
                renderEntitySubgraph(rbacDef.getRootEntityAliasProxy());
            }

        });
        flowchart.chopEmptyLines();
        flowchart.writeLn("end");
        flowchart.writeLn();
    }

    private void wrapOutputInSubgraph(final String name, final String color, final String content) {
        if (!StringUtils.isEmpty(content)) {
            flowchart.ensureSingleEmptyLine();
            flowchart.writeLn("subgraph " + name + "[ ]\n");
            flowchart.indented(() -> {
                flowchart.writeLn("style %{aliasName} fill:%{fillColor},stroke:white"
                            .replace("%{aliasName}", name)
                            .replace("%{fillColor}", color));
                flowchart.writeLn();
                flowchart.writeLn(content);
            });
            flowchart.chopEmptyLines();
            flowchart.writeLn("end");
            flowchart.writeLn();
        }
    }

    private void renderGrants() {
        renderGrants(ROLE_TO_USER, "%% granting roles to users");
        renderGrants(ROLE_TO_ROLE, "%% granting roles to roles");
        renderGrants(PERM_TO_ROLE, "%% granting permissions to roles");
    }

    private void renderGrants(final RbacView.RbacGrantDefinition.GrantType grantType, final String comment) {
        final var grantsOfRequestedType = rbacDef.getGrantDefs().stream()
                .filter(g -> g.grantType() == grantType)
                .filter(rbacDef::renderInDiagram)
                .filter(this::isToBeRenderedForThisCase)
                .toList();
        if ( !grantsOfRequestedType.isEmpty()) {
            flowchart.ensureSingleEmptyLine();
            flowchart.writeLn(comment);
            grantsOfRequestedType.forEach(g -> flowchart.writeLn(grantDef(g)));
        }
    }

    private boolean isToBeRenderedForThisCase(final RbacView.RbacGrantDefinition g) {
        if ( g.grantType() == ROLE_TO_USER )
            return true;
        if ( forCase == null && !g.isConditional() )
            return true;
        final var isToBeRenderedInThisGraph = g.getForCases() == null || g.getForCases().contains(forCase);
        return isToBeRenderedInThisGraph;
    }

    private String grantDef(final RbacView.RbacGrantDefinition grant) {
        final var arrow = (grant.isToCreate() ? " ==>" : " -.->")
                        + (grant.isAssumed() ? " " : "|XX| ");
        final var grantDef = switch (grant.grantType()) {
            case ROLE_TO_USER ->
                // TODO: other user types not implemented yet
                    "user:creator" + arrow + roleId(grant.getSubRoleDef());
            case ROLE_TO_ROLE ->
                    roleId(grant.getSuperRoleDef()) + arrow + roleId(grant.getSubRoleDef());
            case PERM_TO_ROLE -> roleId(grant.getSuperRoleDef()) + arrow + permId(grant.getPermDef());
        };
        return grantDef;
    }

    private String permDef(final RbacView.RbacPermissionDefinition perm) {
        return permId(perm) + "{{" + perm.getEntityAlias().aliasName() + perm.getPermission() + "}}";
    }

    private static String permId(final RbacView.RbacPermissionDefinition permDef) {
        return "perm:" + permDef.getEntityAlias().aliasName() + permDef.getPermission();
    }

    private String roleDef(final RbacView.RbacRoleDefinition roleDef) {
        return roleId(roleDef) + "[[" + roleDef.getEntityAlias().aliasName() + roleDef.getRole() + "]]";
    }

    private static String roleId(final RbacView.RbacRoleDefinition r) {
        return "role:" + r.getEntityAlias().aliasName() + r.getRole();
    }

    @Override
    public String toString() {
        return flowchart.toString();
    }

    @SneakyThrows
    public void generateToMarkdownFile(final Path path) {
        Files.writeString(
                path,
                """
                    ### rbac %{entityAlias}%{case}
    
                    This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.
    
                    ```mermaid
                    %{flowchart}
                    ```
                    """
                    .replace("%{entityAlias}", rbacDef.getRootEntityAlias().aliasName())
                    .replace("%{flowchart}", flowchart.toString())
                    .replace("%{case}", forCase == null ? "" : " " + forCase),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Markdown-File: " + path.toAbsolutePath());
    }
}
