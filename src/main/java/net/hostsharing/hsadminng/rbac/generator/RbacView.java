package net.hostsharing.hsadminng.rbac.generator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.hostsharing.hsadminng.rbac.object.BaseEntity;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.max;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition.GrantType.PERM_TO_ROLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacGrantDefinition.GrantType.ROLE_TO_ROLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.Part.AUTO_FETCH;
import static org.apache.commons.collections4.SetUtils.hashSet;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

@Getter
// TODO.refa: rename to RbacDSL
public class RbacView {

    public static final String GLOBAL = "rbac.global";
    public static final String OUTPUT_BASEDIR = "src/main/resources/db/changelog";

    private final EntityAlias rootEntityAlias;

    private final Set<RbacSubjectReference> userDefs = new LinkedHashSet<>();
    private final Set<RbacRoleDefinition> roleDefs = new LinkedHashSet<>();
    private final Set<RbacPermissionDefinition> permDefs = new LinkedHashSet<>();
    private final Map<String, EntityAlias> entityAliases = new HashMap<>() {

        @Override
        public EntityAlias put(final String key, final EntityAlias value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("duplicate entityAlias: " + key);
            }
            return super.put(key, value);
        }
    };
    private final Set<String> updatableColumns = new LinkedHashSet<>();
    private final Set<RbacGrantDefinition> grantDefs = new LinkedHashSet<>();
    private final Set<CaseDef> allCases = new LinkedHashSet<>();

    private String discriminatorColumName;
    private CaseDef processingCase;
    private SQL identityViewSqlQuery;
    private SQL orderBySqlExpression;
    private EntityAlias rootEntityAliasProxy;
    private RbacRoleDefinition previousRoleDef;
    private Set<String> limitDiagramToAliasNames;
    private final Map<String, CaseDef> cases = new LinkedHashMap<>() {
        @Override
        public CaseDef put(final String key, final CaseDef value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("duplicate case: " + key);
            }
            return super.put(key, value);
        }
    };

    /** Crates an RBAC definition template for the given entity class and defining the given alias.
     *
     * @param alias
     *  an alias name for this entity/table, which can be used in further grants
     *
     * @param entityClass
     *  the Java class for which this RBAC definition is to be defined
     *  (the class to which the calling method belongs)
     *
     * @return
     *  the newly created RBAC definition template
     *
     * @param <E>
     *     a JPA entity class extending RbacObject
     */
    public static <E extends BaseEntity> RbacView rbacViewFor(final String alias, final Class<E> entityClass) {
        return new RbacView(alias, entityClass);
    }

    RbacView(final String alias, final Class<? extends BaseEntity> entityClass) {
        rootEntityAlias = new EntityAlias(alias, entityClass);
        entityAliases.put(alias, rootEntityAlias);
        new RbacSubjectReference(CREATOR);
        entityAliases.put("rbac.global", new EntityAlias("rbac.global"));
    }

    /**
     * Specifies, which columns of the restricted view are updatable at all.
     *
     * @param columnNames
     *  A list of the updatable columns.
     *
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView withUpdatableColumns(final String... columnNames) {
        Collections.addAll(updatableColumns, columnNames);
        verifyVersionColumnExists();
        return this;
    }

    /** Specifies the SQL query which creates the identity view for this entity.
     *
     * <p>An identity view is a view which maps an objectUuid to an idName.
     * The idName should be a human-readable representation of the row, but as short as possible.
     * The idName must only consist of letters (A-Z, a-z), digits (0-9), dash (-), dot (.) and unserscore '_'.
     * It's used to create the object-specific-role-names like test_customer#abc:ADMIN - here 'abc' is the idName.
     * The idName not necessarily unique in a table, but it should be avoided.
     * </p>
     *
     * @param sqlExpression
     *  Either specify an SQL projection (the part between SELECT and FROM), e.g. `SQL.projection("columnName")
     *  or the whole SELECT query returning the uuid and idName columns,
     *  e.g. `SQL.query("SELECT ... AS uuid, ... AS idName FROM ... JOIN ...").
     *  Only add really important columns, just enough to create a short human-readable representation.
     *
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView withIdentityView(final SQL sqlExpression) {
        this.identityViewSqlQuery = sqlExpression;
        return this;
    }

    /**
     * Specifies a ORDER BY clause for the generated restricted view.
     *
     * <p>A restricted view is generated, no matter if the order was specified or not.</p>
     *
     * @param orderBySqlExpression
     *  That's the part behind `ORDER BY`, e.g. `SQL.expression("prefix").
     *
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView withRestrictedViewOrderBy(final SQL orderBySqlExpression) {
        this.orderBySqlExpression = orderBySqlExpression;
        return this;
    }

    /**
     * Specifies that the given role (OWNER, ADMIN, ...) is to be created for new/updated roles in this table.
     *
     * @param role
     *  OWNER, ADMIN, AGENT etc.
     * @param with
     *  a lambda which receives the created role to create grants and permissions to and from the newly created role,
     *  e.g. the owning user, incoming superroles, outgoing subroles
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView createRole(final Role role, final Consumer<RbacRoleDefinition> with) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        with.accept(newRoleDef);
        previousRoleDef = newRoleDef;
        return this;
    }

    /**
     * Specifies that the given role (OWNER, ADMIN, ...) is to be created for new/updated roles in this table,
     * which is becomes sub-role of the previously created role.
     *
     * @param role
     *  OWNER, ADMIN, AGENT etc.
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView createSubRole(final Role role) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        findOrCreateGrantDef(newRoleDef, previousRoleDef).toCreate();
        previousRoleDef = newRoleDef;
        return this;
    }


    /**
     * Specifies that the given role (OWNER, ADMIN, ...) is to be created for new/updated roles in this table,
     * which is becomes sub-role of the previously created role.
     *
     * @param role
     *  OWNER, ADMIN, AGENT etc.
     * @param with
     *  a lambda which receives the created role to create grants and permissions to and from the newly created role,
     *  e.g. the owning user, incoming superroles, outgoing subroles
     * @return
     *  the `this` instance itself to allow chained calls.
     */
    public RbacView createSubRole(final Role role, final Consumer<RbacRoleDefinition> with) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        findOrCreateGrantDef(newRoleDef, previousRoleDef).toCreate();
        with.accept(newRoleDef);
        previousRoleDef = newRoleDef;
        return this;
    }

    /**
     * Specifies that the given permission is to be created for each new row in the target table.
     *
     * <p>Grants to permissions created by this method have to be specified separately,
     * often it's easier to read to use createRole/createSubRole and use with.permission(...).</p>
     *
     * @param permission
     *  e.g. INSERT, SELECT, UPDATE, DELETE
     *
     * @return
     *  the newly created permission definition
     */
    public RbacPermissionDefinition createPermission(final Permission permission) {
        return createPermission(rootEntityAlias, permission);
    }

    /**
     * Specifies that the given permission is to be created for each new row in the target table,
     * but for another table, e.g. a table with details data with different access rights.
     *
     * <p>Grants to permissions created by this method have to be specified separately,
     * often it's easier to read to use createRole/createSubRole and use with.permission(...).</p>
     *
     * @param entityAliasName
     *  A previously defined entity alias name.
     *
     * @param permission
     *  e.g. INSERT, SELECT, UPDATE, DELETE
     *
     * @return
     *  the newly created permission definition
     */
    public RbacPermissionDefinition createPermission(final String entityAliasName, final Permission permission) {
        return createPermission(findEntityAlias(entityAliasName), permission);
    }

    private RbacPermissionDefinition createPermission(final EntityAlias entityAlias, final Permission permission) {
        return permDefs.stream()
                .filter(p -> p.permission == permission && p.entityAlias == entityAlias)
                .findFirst()
                // .map(g -> g.forCase(processingCase)) TODO.impl: not implemented case dependent
                .orElseGet(() -> new RbacPermissionDefinition(entityAlias, permission, null, true));
    }

    public <EC extends BaseEntity> RbacView declarePlaceholderEntityAliases(final String... aliasNames) {
        for (String alias : aliasNames) {
            entityAliases.put(alias, new EntityAlias(alias));
        }
        return this;
    }

    /**
     * Imports the RBAC template from the given entity class and defines an alias name for it.
     * This method is especially for proxy-entities, if the root entity does not have its own
     * roles, a proxy-entity can be specified and its roles can be used instead.
     *
     * @param aliasName
     *  An alias name for the entity class. The same entity class can be imported multiple times,
     *  if multiple references to its table exist, then distinct alias names habe to be defined.
     *
     * @param entityClass
     *  A JPA entity class extending RbacObject which also implements an `rbac` method returning
     *  its RBAC specification.
     *
     * @param fetchSql
     *  An SQL SELECT statement which fetches the referenced row. Use `${REF}` to speficiy the
     *  newly created or updated row (will be replaced by NEW/OLD from the trigger method).
     *
     * @param dependsOnColum
     *  The column, usually containing an uuid, on which this other table depends.
     *
     * @return
     *  the newly created permission definition
     *
     * @param <EC>
     *     a JPA entity class extending RbacObject
     */
    public <EC extends BaseEntity> RbacView importRootEntityAliasProxy(
            final String aliasName,
            final Class<? extends BaseEntity> entityClass,
            final ColumnValue forCase,
            final SQL fetchSql,
            final Column dependsOnColum) {
        if (rootEntityAliasProxy != null) {
            throw new IllegalStateException("there is already an entityAliasProxy: " + rootEntityAliasProxy);
        }
        rootEntityAliasProxy = importEntityAliasImpl(aliasName, entityClass, forCase, fetchSql, dependsOnColum, false, NOT_NULL);
        return this;
    }

    /**
     * Imports the RBAC template from the given entity class and defines an alias name for it.
     * This method is especially to declare sub-entities, e.g. details to a main object.
     *
     * @see {@link}
     *
     * @return
     *  the newly created permission definition
     *
     * @param <EC>
     *     a JPA entity class extending RbacObject
     */
    public RbacView importSubEntityAlias(
            final String aliasName, final Class<? extends BaseEntity> entityClass,
            final SQL fetchSql, final Column dependsOnColum) {
        importEntityAliasImpl(aliasName, entityClass, usingDefaultCase(), fetchSql, dependsOnColum, true, NOT_NULL);
        return this;
    }

    /**
     * Imports the RBAC template from the given entity class and defines an anlias name for it.
     *
     * @param aliasName
     *  An alias name for the entity class. The same entity class can be imported multiple times,
     *  if multiple references to its table exist, then distinct alias names habe to be defined.
     *
     * @param entityClass
     *  A JPA entity class extending RbacObject which also implements an `rbac` method returning
     *  its RBAC specification.
     *
     * @param usingCase
     *  Only use this case value for a switch within the rbac rules.
     *
     * @param fetchSql
     *  An SQL SELECT statement which fetches the referenced row. Use `${REF}` to speficiy the
     *  newly created or updated row (will be replaced by NEW/OLD from the trigger method).
     *
     * @param dependsOnColum
     *  The column, usually containing an uuid, on which this other table depends.
     *
     * @param nullable
     *  Specifies whether the dependsOnColum is nullable or not.
     *
     * @return
     *  the newly created permission definition
     *
     * @param <EC>
     *     a JPA entity class extending RbacObject
     */
    public RbacView importEntityAlias(
            final String aliasName, final Class<? extends BaseEntity> entityClass, final ColumnValue usingCase,
            final Column dependsOnColum, final SQL fetchSql, final Nullable nullable) {
        importEntityAliasImpl(aliasName, entityClass, usingCase, fetchSql, dependsOnColum, false, nullable);
        return this;
    }

    private EntityAlias importEntityAliasImpl(
            final String aliasName, final Class<? extends BaseEntity> entityClass, final ColumnValue usingCase,
            final SQL fetchSql, final Column dependsOnColum, boolean asSubEntity, final Nullable nullable) {

        final var entityAlias = ofNullable(entityAliases.get(aliasName))
                .orElseGet(() -> {
                    final var ea = new EntityAlias(aliasName, entityClass, usingCase, fetchSql, dependsOnColum, asSubEntity, nullable);
                    entityAliases.put(aliasName, ea);
                    return ea;
                });

        try {
            // TODO.rbac: this only works for directly recursive RBAC definitions, not for indirect recursion
            final var rbacDef = entityClass == rootEntityAlias.entityClass
                ? this
                : rbacDefinition(entityClass);
            importAsAlias(aliasName, rbacDef, usingCase, asSubEntity);
        } catch (final ReflectiveOperationException exc) {
            throw new RuntimeException("cannot import entity: " + entityClass, exc);
        }
        return entityAlias;
    }

    private static RbacView rbacDefinition(final Class<? extends BaseEntity> entityClass)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (RbacView) entityClass.getMethod("rbac").invoke(null);
    }

    private RbacView importAsAlias(final String aliasName, final RbacView importedRbacView, final ColumnValue forCase, final boolean asSubEntity) {
        final var mapper = new AliasNameMapper(importedRbacView, aliasName,
                asSubEntity ? entityAliases.keySet() : null);
        copyOf(importedRbacView.getEntityAliases().values()).stream()
                .filter(entityAlias -> !importedRbacView.isRootEntityAlias(entityAlias))
                .filter(entityAlias -> !entityAlias.isGlobal())
                .filter(entityAlias -> !asSubEntity || !entityAliases.containsKey(entityAlias.aliasName))
                .forEach(entityAlias -> {
                    final String mappedAliasName = mapper.map(entityAlias.aliasName);
                    entityAliases.put(mappedAliasName, new EntityAlias(mappedAliasName, entityAlias.entityClass));
                });
        copyOf(importedRbacView.getRoleDefs()).forEach(roleDef -> {
            new RbacRoleDefinition(findEntityAlias(mapper.map(roleDef.entityAlias.aliasName)), roleDef.role);
        });
        copyOf(importedRbacView.getGrantDefs()).forEach(grantDef -> {
            if ( grantDef.grantType() == ROLE_TO_ROLE && grantDef.matchesCase(forCase) ) {
                final var importedGrantDef = findOrCreateGrantDef(
                        findRbacRole(
                                mapper.map(grantDef.getSubRoleDef().entityAlias.aliasName),
                                grantDef.getSubRoleDef().getRole()),
                        findRbacRole(
                                mapper.map(grantDef.getSuperRoleDef().entityAlias.aliasName),
                                grantDef.getSuperRoleDef().getRole())
                );
                if (!grantDef.isAssumed()) {
                    importedGrantDef.unassumed();
                }
            }
        });
        return this;
    }

    public RbacView switchOnColumn(final String discriminatorColumName, final CaseDef... caseDefs) {
        this.discriminatorColumName = discriminatorColumName;
        allCases.addAll(stream(caseDefs).toList());

        stream(caseDefs).forEach(caseDef -> {
            this.processingCase = caseDef;
            caseDef.def.accept(this);
            this.processingCase = null;
        });
        return this;
    }

    private static <T> List<T> copyOf(final Collection<T> eas) {
        return eas.stream().toList();
    }

    private void verifyVersionColumnExists() {
        final var clazz = rootEntityAlias.entityClass;
        if (!hasVersionColumn(clazz)) {
            throw new IllegalArgumentException("@Version field required in updatable entity " + rootEntityAlias.entityClass);
        }
    }

    private static boolean hasVersionColumn(final Class<?> clazz) {
        if (stream(clazz.getDeclaredFields()).anyMatch(f -> f.getAnnotation(Version.class) != null)) {
            return true;
        }
        if (clazz.getSuperclass() != null) {
            return hasVersionColumn(clazz.getSuperclass());
        }
        return false;
    }

    /**
     * Starts declaring a grant to a given role.
     *
     * @param entityAlias
     *  A previously speciried entity alias name.
     * @param role
     *  OWNER, ADMIN, AGENT, ...
     * @return
     *  a grant builder
     */
    public RbacGrantBuilder toRole(final String entityAlias, final Role role) {
        return new RbacGrantBuilder(entityAlias, role);
    }

    public RbacExampleRole forExampleRole(final String entityAlias, final Role role) {
        return new RbacExampleRole(entityAlias, role);
    }

    private RbacGrantDefinition grantRoleToSubject(final RbacRoleDefinition roleDefinition, final RbacSubjectReference user) {
        return findOrCreateGrantDef(roleDefinition, user).toCreate();
    }

    private RbacGrantDefinition grantPermissionToRole(
            final RbacPermissionDefinition permDef,
            final RbacRoleDefinition roleDef) {
        return findOrCreateGrantDef(permDef, roleDef).toCreate();
    }

    private RbacGrantDefinition grantSubRoleToSuperRole(
            final RbacRoleDefinition subRoleDefinition,
            final RbacRoleDefinition superRoleDefinition) {
        return findOrCreateGrantDef(subRoleDefinition, superRoleDefinition).toCreate();
    }

    boolean isRootEntityAlias(final EntityAlias entityAlias) {
        return entityAlias == this.rootEntityAlias;
    }

    public boolean isEntityAliasProxy(final EntityAlias entityAlias) {
        return entityAlias == rootEntityAliasProxy;
    }

    public SQL getOrderBySqlExpression() {
        if (orderBySqlExpression == null) {
            return identityViewSqlQuery;
        }
        return orderBySqlExpression;
    }

    public void generateWithBaseFileName(final String baseFileName) {
        if (allCases.size() > 1) {
            allCases.forEach(caseDef -> {
                final var fileName = baseFileName + (caseDef.isDefaultCase() ? "" : "-" + caseDef.value) + ".md";
                new RbacViewMermaidFlowchartGenerator(this, caseDef)
                        .generateToMarkdownFile(Path.of(OUTPUT_BASEDIR, fileName));
            });
        } else {
            new RbacViewMermaidFlowchartGenerator(this).generateToMarkdownFile(Path.of(OUTPUT_BASEDIR, baseFileName + ".md"));
        }
        new RbacViewPostgresGenerator(this).generateToChangeLog(Path.of(OUTPUT_BASEDIR, baseFileName + ".sql"));
    }

    public RbacView limitDiagramTo(final String... aliasNames) {
        this.limitDiagramToAliasNames = Set.of(aliasNames);
        return this;
    }

    public boolean renderInDiagram(final EntityAlias ea) {
        return limitDiagramToAliasNames == null || limitDiagramToAliasNames.contains(ea.aliasName());
    }

    public boolean renderInDiagram(final RbacGrantDefinition g) {
        if ( limitDiagramToAliasNames == null ) {
            return true;
        }
        return switch (g.grantType()) {
        case ROLE_TO_USER ->
                renderInDiagram(g.getSubRoleDef().getEntityAlias());
        case ROLE_TO_ROLE ->
                renderInDiagram(g.getSuperRoleDef().getEntityAlias()) && renderInDiagram(g.getSubRoleDef().getEntityAlias());
        case PERM_TO_ROLE ->
                renderInDiagram(g.getSuperRoleDef().getEntityAlias()) && renderInDiagram(g.getPermDef().getEntityAlias());
        };
    }

    public class RbacGrantBuilder {

        private final RbacRoleDefinition superRoleDef;

        private RbacGrantBuilder(final String entityAlias, final Role role) {
            this.superRoleDef = findRbacRole(entityAlias, role);
        }

        public RbacView grantRole(final String entityAlias, final Role role) {
            findOrCreateGrantDef(findRbacRole(entityAlias, role), superRoleDef).toCreate();
            return RbacView.this;
        }

        public RbacView grantPermission(final Permission perm) {
            final var forTable = rootEntityAlias.getRawTableNameWithSchema();
            findOrCreateGrantDef(findRbacPerm(rootEntityAlias, perm, forTable), superRoleDef).toCreate();
            return RbacView.this;
        }

    }

    public enum Nullable {
        NOT_NULL, // DEFAULT
        NULLABLE
    }

    @Getter
    @EqualsAndHashCode
    public class RbacGrantDefinition {

        private final RbacSubjectReference userDef;
        private final RbacRoleDefinition superRoleDef;
        private final RbacRoleDefinition subRoleDef;
        private final RbacPermissionDefinition permDef;
        private boolean assumed = true;
        private boolean toCreate = false;
        private Set<CaseDef> forCases = new LinkedHashSet<>();

        @Override
        public String toString() {
            final var arrow = isAssumed() ? " --> " : " -- // --> ";
            final var grant = switch (grantType()) {
                case ROLE_TO_USER -> userDef.toString() + arrow + subRoleDef.toString();
                case ROLE_TO_ROLE -> superRoleDef + arrow + subRoleDef;
                case PERM_TO_ROLE -> superRoleDef + arrow + permDef;
            };
            final var condition = isConditional()
                    ? (" (" +forCases.stream().map(CaseDef::toString).collect(Collectors.joining("||")) + ")")
                    : "";
            return grant + condition;
        }

        RbacGrantDefinition(final RbacRoleDefinition subRoleDef, final RbacRoleDefinition superRoleDef, final CaseDef forCase) {
            this.userDef = null;
            this.subRoleDef = subRoleDef;
            this.superRoleDef = superRoleDef;
            this.permDef = null;
            this.forCases = forCase != null ? hashSet(forCase) : null;
            register(this);
        }

        public RbacGrantDefinition(final RbacPermissionDefinition permDef, final RbacRoleDefinition roleDef,
                final CaseDef forCase) {
            this.userDef = null;
            this.subRoleDef = null;
            this.superRoleDef = roleDef;
            this.permDef = permDef;
            this.forCases = forCase != null ? hashSet(forCase) : null;
            register(this);
        }

        public RbacGrantDefinition(final RbacRoleDefinition roleDef, final RbacSubjectReference userDef) {
            this.userDef = userDef;
            this.subRoleDef = roleDef;
            this.superRoleDef = null;
            this.permDef = null;
            register(this);
        }

        private void register(final RbacGrantDefinition rbacGrantDefinition) {
            grantDefs.add(rbacGrantDefinition);
        }

        @NotNull
        GrantType grantType() {
            return permDef != null ? PERM_TO_ROLE
                    : userDef != null ? GrantType.ROLE_TO_USER
                    : ROLE_TO_ROLE;
        }

        boolean isAssumed() {
            return assumed;
        }


        RbacGrantDefinition forCase(final CaseDef processingCase) {
            forCases.add(processingCase);
            return this;
        }

        boolean isConditional() {
            return forCases != null && !forCases.isEmpty() && forCases.size()<allCases.size();
        }

        boolean matchesCase(final ColumnValue requestedCase) {
            final var noCasesDefined = forCases == null;
            final var generateForAllCases = requestedCase == null;
            final boolean isGrantedForRequestedCase = forCases == null || forCases.stream().anyMatch(c -> c.isCase(requestedCase))
                    || forCases.stream().anyMatch(CaseDef::isDefaultCase) && !allCases.stream().anyMatch(c -> c.isCase(requestedCase));
            return  noCasesDefined || generateForAllCases || isGrantedForRequestedCase;
        }

        boolean isToCreate() {
            return toCreate;
        }

        RbacGrantDefinition toCreate() {
            toCreate = true;
            return this;
        }

        boolean dependsOnColumn(final String columnName) {
            return dependsRoleDefOnColumnName(this.superRoleDef, columnName)
                    || dependsRoleDefOnColumnName(this.subRoleDef, columnName);
        }

        private Boolean dependsRoleDefOnColumnName(final RbacRoleDefinition superRoleDef, final String columnName) {
            return ofNullable(superRoleDef)
                    .map(r -> r.getEntityAlias().dependsOnColum())
                    .map(d -> columnName.equals(d.column))
                    .orElse(false);
        }

        public RbacGrantDefinition unassumed() {
            this.assumed = false;
            return this;
        }

        public long level() {
            return max(asList(
                    superRoleDef != null ? superRoleDef.entityAlias.level() : 0,
                    subRoleDef != null ? subRoleDef.entityAlias.level() : 0,
                    permDef != null ? permDef.entityAlias.level() : 0));
        }

        public enum GrantType {
            ROLE_TO_USER,
            ROLE_TO_ROLE,
            PERM_TO_ROLE
        }
    }

    public class RbacExampleRole {

        final EntityAlias subRoleEntity;
        final Role subRole;
        private EntityAlias superRoleEntity;
        Role superRole;

        public RbacExampleRole(final String entityAlias, final Role role) {
            this.subRoleEntity = findEntityAlias(entityAlias);
            this.subRole = role;
        }

        public RbacView wouldBeGrantedTo(final String entityAlias, final Role role) {
            this.superRoleEntity = findEntityAlias(entityAlias);
            this.superRole = role;
            return RbacView.this;
        }
    }

    @Getter
    @EqualsAndHashCode
    public class RbacPermissionDefinition {

        final EntityAlias entityAlias;
        final Permission permission;
        final String tableName;
        final boolean toCreate;

        private RbacPermissionDefinition(final EntityAlias entityAlias, final Permission permission, final String tableName,
                final boolean toCreate) {
            this.entityAlias = entityAlias;
            this.permission = permission;
            this.tableName = tableName;
            this.toCreate = toCreate;
            permDefs.add(this);
        }

        /**
         * Grants the permission under definition to the given role.
         *
         * @param entityAlias
         *  A previously declared entity alias name.
         * @param role
         *  OWNER, ADMIN, ...
         * @return
         *  The RbacView specification to which this permission definition belongs.
         */
        public RbacView grantedTo(final String entityAlias, final Role role) {
            findOrCreateGrantDef(this, findRbacRole(entityAlias, role)).toCreate();
            return RbacView.this;
        }

        @Override
        public String toString() {
            return "perm:" + entityAlias.aliasName + permission + ofNullable(tableName).map(tn -> ":" + tn).orElse("");
        }
    }

    @Getter
    @EqualsAndHashCode
    public class RbacRoleDefinition {

        private final EntityAlias entityAlias;
        private final Role role;
        private boolean toCreate;

        public RbacRoleDefinition(final EntityAlias entityAlias, final Role role) {
            this.entityAlias = entityAlias;
            this.role = role;
            roleDefs.add(this);
        }

        public RbacRoleDefinition toCreate() {
            this.toCreate = true;
            return this;
        }

        /**
         * Specifies which user becomes the owner of newly created objects.
         * @param userRole
         *  GLOBAL_ADMIN, CREATOR, ...
         * @return
         *  The grant definition for further chained calls.
         */
        public RbacGrantDefinition owningUser(final RbacSubjectReference.UserRole userRole) {
            return grantRoleToSubject(this, findUserRef(userRole));
        }

        /**
         * Specifies which permission is to be created for newly created objects.
         * @param permission
         *  INSERT, SELECT, ...
         * @return
         *  The grant definition for further chained calls.
         */
        public RbacGrantDefinition permission(final Permission permission) {
            return grantPermissionToRole(createPermission(entityAlias, permission), this);
        }

        /**
         * Specifies in incoming super role which gets granted the role under definition.
         *
         * <p>Incoming means an incoming grant arrow in our grant-diagrams.
         * Super-role means that it's the role to which another role is granted.
         * Both means actually the same, just in different aspects.</p>
         *
         * @param entityAlias
         *  A previously declared entity alias name.
         * @param role
         *  OWNER, ADMIN, ...
         * @return
         *  The grant definition for further chained calls.
         */
        public RbacGrantDefinition incomingSuperRole(final String entityAlias, final Role role) {
            final var incomingSuperRole = findRbacRole(entityAlias, role);
            return grantSubRoleToSuperRole(this, incomingSuperRole);
        }

        /**
         * Specifies in outgoing sub role which gets granted the role under definition.
         *
         * <p>Outgoing means an outgoing grant arrow in our grant-diagrams.
         * Sub-role means which is granted to another role.
         * Both means actually the same, just in different aspects.</p>
         *
         * @param entityAlias
         *  A previously declared entity alias name.
         * @param role
         *  OWNER, ADMIN, ...
         * @return
         *  The grant definition for further chained calls.
         */
        public RbacGrantDefinition outgoingSubRole(final String entityAlias, final Role role) {
            final var outgoingSubRole = findRbacRole(entityAlias, role);
            return grantSubRoleToSuperRole(outgoingSubRole, this);
        }

        @Override
        public String toString() {
            return "role:" + entityAlias.aliasName + role;
        }

        public boolean isGlobal(final Role role) {
            return entityAlias.isGlobal() && this.role == role;
        }
    }

    public RbacSubjectReference findUserRef(final RbacSubjectReference.UserRole userRole) {
        return userDefs.stream().filter(u -> u.role == userRole).findFirst().orElseThrow();
    }

    @EqualsAndHashCode
    public class RbacSubjectReference {

        public enum UserRole {
            GLOBAL_ADMIN,
            CREATOR
        }

        final UserRole role;

        public RbacSubjectReference(final UserRole creator) {
            this.role = creator;
            userDefs.add(this);
        }

        @Override
        public String toString() {
            return "user:" + role;
        }
    }

    EntityAlias findEntityAlias(final String aliasName) {
        final var found = entityAliases.get(aliasName);
        if (found == null) {
            throw new IllegalArgumentException("entityAlias not found: " + aliasName);
        }
        return found;
    }

    RbacRoleDefinition findRbacRole(final EntityAlias entityAlias, final Role role) {
        return roleDefs.stream()
                .filter(r -> r.getEntityAlias() == entityAlias && r.getRole().equals(role))
                .findFirst()
                .orElseGet(() -> new RbacRoleDefinition(entityAlias, role));
    }

    public RbacRoleDefinition findRbacRole(final String entityAliasName, final Role role) {
        return findRbacRole(findEntityAlias(entityAliasName), role);

    }

    RbacPermissionDefinition findRbacPerm(final EntityAlias entityAlias, final Permission perm, String tableName) {
        return permDefs.stream()
            .filter(p -> p.getEntityAlias() == entityAlias && p.getPermission() == perm)
            .findFirst()
            .orElseGet(() -> new RbacPermissionDefinition(entityAlias, perm, tableName, true)); // TODO: true => toCreate
    }

    private RbacGrantDefinition findOrCreateGrantDef(final RbacRoleDefinition roleDefinition, final RbacSubjectReference user) {
        return grantDefs.stream()
                .filter(g -> g.subRoleDef == roleDefinition && g.userDef == user)
                .findFirst()
                .orElseGet(() -> new RbacGrantDefinition(roleDefinition, user));
    }

    private RbacGrantDefinition findOrCreateGrantDef(final RbacPermissionDefinition permDef, final RbacRoleDefinition roleDef) {
        return grantDefs.stream()
                .filter(g -> g.permDef == permDef && g.superRoleDef == roleDef)
                .findFirst()
                .map(g -> g.forCase(processingCase))
                .orElseGet(() -> new RbacGrantDefinition(permDef, roleDef, processingCase));
    }

    private RbacGrantDefinition findOrCreateGrantDef(
            final RbacRoleDefinition subRoleDefinition,
            final RbacRoleDefinition superRoleDefinition) {
        final var distinctGrantDef = grantDefs.stream()
                .filter(g -> g.subRoleDef == subRoleDefinition && g.superRoleDef == superRoleDefinition)
                .findFirst()
                .map(g -> g.forCase(processingCase))
                .orElseGet(() -> new RbacGrantDefinition(subRoleDefinition, superRoleDefinition, processingCase));
        return distinctGrantDef;
    }

    record EntityAlias(String aliasName, Class<? extends BaseEntity> entityClass, ColumnValue usingCase, SQL fetchSql, Column dependsOnColum, boolean isSubEntity, Nullable nullable) {

        public EntityAlias(final String aliasName) {
            this(aliasName, null, null, null, null, false, null);
        }

        public EntityAlias(final String aliasName, final Class<? extends BaseEntity> entityClass) {
            this(aliasName, entityClass, null, null, null, false, null);
        }

        boolean isGlobal() {
            return aliasName().equals("rbac.global");
        }

        boolean isPlaceholder() {
            return entityClass == null;
        }

        @NotNull
        public SQL fetchSql() {
            if (fetchSql == null) {
                return SQL.noop();
            }
            return switch (fetchSql.part) {
                case SQL_QUERY -> fetchSql;
                case AUTO_FETCH ->
                        SQL.query("SELECT * FROM " + getRawTableNameWithSchema() + " WHERE uuid = ${ref}." + dependsOnColum.column);
                default -> throw new IllegalStateException("unexpected SQL definition: " + fetchSql);
            };
        }

        boolean isFetchedByDirectForeignKey() {
            return fetchSql != null && fetchSql.part == AUTO_FETCH;
        }

        private String withoutEntitySuffix(final String simpleEntityName) {
            // TODO.impl: maybe introduce an annotation like @RbacObjectName("hsOfficeContact")?
            if ( simpleEntityName.endsWith("RbacEntity")) {
                return simpleEntityName.substring(0, simpleEntityName.length() - "RbacEntity".length());
            }
            return simpleEntityName.substring(0, simpleEntityName.length() - "Entity".length());
        }

        String simpleName() {
            return isGlobal()
                    ? aliasName
                    : uncapitalize(withoutEntitySuffix(entityClass.getSimpleName()));
        }

        String getRawTableNameWithSchema() {
            if ( aliasName.equals("rbac.global")) {
                return "rbac.global"; // TODO: maybe we should introduce a GlobalEntity class?
            }
            return withoutRvSuffix(entityClass.getAnnotation(Table.class).name());
        }

        String getRawTableSchemaPrefix() {
            final var rawTableNameWithSchema = getRawTableNameWithSchema();
            final var parts = rawTableNameWithSchema.split("\\.");
            final var rawTableSchemaPrefix = parts.length > 1 ? parts[0] + "." : "";
            return rawTableSchemaPrefix;
        }

        String getRawTableName() {
            final var rawTableNameWithSchema = getRawTableNameWithSchema();
            final var parts = rawTableNameWithSchema.split("\\.");
            final var rawTableName = parts.length > 1 ? parts[1] : rawTableNameWithSchema;
            return rawTableName;
        }

        String getRawTableShortName() {
            // TODO.impl: some combined function and trigger names are too long
            // maybe we should shorten the table name e.g. hs_office_coopsharestransaction -> hsof.coopsharetx
            // this is just a workaround:
            return getRawTableName()
                    .replace("hs_office_", "hsof_")
                    .replace("hs_booking_", "hsbk_")
                    .replace("hs_hosting_", "hsho_")
                    .replace("coopsharestransaction", "coopsharetx")
                    .replace("coopassetstransaction", "coopassettx");
        }

        String dependsOnColumName() {
            if (dependsOnColum == null) {
                throw new IllegalStateException(
                        "Entity " + aliasName + "(" + entityClass.getSimpleName() + ")" + ": please add dependsOnColum");
            }
            return dependsOnColum.column;
        }

        long level() {
            return aliasName.chars().filter(ch -> ch == '.').count() + 1;
        }

        boolean isCaseDependent() {
            return usingCase != null && usingCase.value != null;
        }
    }

    public static String withoutRvSuffix(final String tableName) {
        return tableName.substring(0, tableName.length() - "_rv".length());
    }

    public enum Role {

        OWNER,
        ADMIN,
        AGENT,
        TENANT,
        REFERRER,

        @Deprecated
        GUEST;

        @Override
        public String toString() {
            return ":" + name();
        }
    }

    public enum Permission {
        INSERT,
        DELETE,
        UPDATE,
        SELECT;

        @Override
        public String toString() {
            return ":" + name();
        }
    }

    public static class SQL {

        /**
         * DSL method to specify an SQL SELECT expression which fetches the related entity,
         * using the reference `${ref}` of the root entity and `${columns}` for the projection.
         *
         * <p>The query <strong>must define</strong> the entity alias name of the fetched table
         * as its alias for, so it can be used in the generated projection (the columns between
         * `SELECT` and `FROM`.</p>
         *
         * <p>`${ref}` is going to be replaced by either `NEW` or `OLD` of the trigger function.
         * `into ...` will be added with a variable name prefixed with either `new` or `old`.</p>
         *
         * <p>`${columns}` is going to be replaced by the columns which are needed for the query,
         * e.g. `*` or `uuid`.</p>
         *
         * @param sql an SQL SELECT expression (not ending with ';)
         * @return the wrapped SQL expression
         */
        public static SQL fetchedBySql(final String sql) {
            if ( !sql.startsWith("SELECT ${columns}") ) {
                throw new IllegalArgumentException("SQL SELECT expression must start with 'SELECT ${columns}', but is: " + sql);
            }
            validateExpression(sql);
            return new SQL(sql, Part.SQL_QUERY);
        }

        /**
         * DSL method to specify that a related entity is to be fetched by a simple SELECT statement
         * using the raw table from the @Table statement of the entity to fetch
         * and the dependent column of the root entity.
         *
         * @return the wrapped SQL definition object
         */
        public static SQL directlyFetchedByDependsOnColumn() {
            return new SQL(null, AUTO_FETCH);
        }

        /**
         * DSL method to explicitly specify that there is no SQL query.
         *
         * @return a wrapped SQL definition object representing a noop query
         */
        public static SQL noop() {
            return new SQL(null, Part.NOOP);
        }

        /**
         * Generic DSL method to specify an SQL SELECT expression.
         *
         * @param sql an SQL SELECT expression (not ending with ';)
         * @return the wrapped SQL expression
         */
        public static SQL query(final String sql) {
            validateExpression(sql);
            return new SQL(sql, Part.SQL_QUERY);
        }

        /**
         * Generic DSL method to specify an SQL SELECT expression by just the projection part.
         *
         * @param projection an SQL SELECT expression, the list of columns after 'SELECT'
         * @return the wrapped SQL projection
         */
        public static SQL projection(final String projection) {
            validateProjection(projection);
            return new SQL(projection, Part.SQL_PROJECTION);
        }

        public static SQL expression(final String sqlExpression) {
            // TODO: validate
            return new SQL(sqlExpression, Part.SQL_EXPRESSION);
        }

        enum Part {
            NOOP,
            SQL_QUERY,
            AUTO_FETCH,
            SQL_PROJECTION,
            SQL_EXPRESSION
        }

        final String sql;
        final Part part;

        private SQL(final String sql, final Part part) {
            this.sql = sql;
            this.part = part;
        }

        private static void validateProjection(final String projection) {
            if (projection.toUpperCase().matches("[ \t]*$SELECT[ \t]")) {
                throw new IllegalArgumentException("SQL projection must not start with 'SELECT': " + projection);
            }
            if (projection.matches(";[ \t]*$")) {
                throw new IllegalArgumentException("SQL projection must not end with ';': " + projection);
            }
        }

        private static void validateExpression(final String sql) {
            if (sql.matches(";[ \t]*$")) {
                throw new IllegalArgumentException("SQL expression must not end with ';': " + sql);
            }
        }
    }

    public static class Column {

        public static Column dependsOnColumn(final String column) {
            return new Column(column);
        }

        public final String column;

        private Column(final String column) {
            this.column = column;
        }
    }

    public static class ColumnValue {

        public static ColumnValue usingDefaultCase() {
            return new ColumnValue(null);
        }

        public static <E extends Enum<E>> ColumnValue usingCase(final E value) {
            return new ColumnValue(value.name());
        }
        public final String value;

        private ColumnValue(final String value) {
            this.value = value;
        }
    }

    private static class AliasNameMapper {

        private final RbacView importedRbacView;
        private final String outerAliasName;

        private final Set<String> outerAliasNames;

        AliasNameMapper(final RbacView importedRbacView, final String outerAliasName, final Set<String> outerAliasNames) {
            this.importedRbacView = importedRbacView;
            this.outerAliasName = outerAliasName;
            this.outerAliasNames = (outerAliasNames == null) ? Collections.emptySet() : outerAliasNames;
        }

        String map(final String originalAliasName) {
            if (outerAliasNames.contains(originalAliasName) || originalAliasName.equals("rbac.global")) {
                return originalAliasName;
            }
            if (originalAliasName.equals(importedRbacView.rootEntityAlias.aliasName)) {
                return outerAliasName;
            }
            return outerAliasName + "." + originalAliasName;
        }
    }

    public static class CaseDef extends ColumnValue {

        final Consumer<RbacView> def;

        private CaseDef(final String discriminatorColumnValue, final Consumer<RbacView> def) {
            super(discriminatorColumnValue);
            this.def = def;
        }


        public static CaseDef inCaseOf(final String discriminatorColumnValue, final Consumer<RbacView> def) {
            return new CaseDef(discriminatorColumnValue, def);
        }

        public static CaseDef inOtherCases(final Consumer<RbacView> def) {
            return new CaseDef(null, def);
        }

        @Override
        public int hashCode() {
            return ofNullable(value).map(String::hashCode).orElse(0);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            final CaseDef caseDef = (CaseDef) other;
            return Objects.equals(value, caseDef.value);
        }

        boolean isDefaultCase() {
            return value == null;
        }

        @Override
        public String toString() {
            return isDefaultCase()
                ? "inOtherCases"
                : "inCaseOf:" + value;
        }

        public boolean isCase(final ColumnValue requestedCase) {
            return Objects.equals(requestedCase.value, this.value);
        }
    }

    private static void generateRbacView(final Class<? extends BaseEntity> c) {
        final Method mainMethod = stream(c.getMethods()).filter(
                        m -> isStatic(m.getModifiers()) && m.getName().equals("main")
                )
                .findFirst()
                .orElse(null);
        if (mainMethod != null) {
            try {
                mainMethod.invoke(null, new Object[] { null });
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.err.println("WARNING: no main method in: " + c.getName() + " => no RBAC rules generated");
        }
    }

    public static Set<Class<? extends BaseEntity>> findRbacEntityClasses(String packageName) {
        final var reflections = new Reflections(packageName, TypeAnnotationsScanner.class);
        return reflections.getTypesAnnotatedWith(Entity.class).stream()
                .filter(c -> stream(c.getInterfaces()).anyMatch(i -> i== BaseEntity.class))
                .filter(c -> stream(c.getDeclaredMethods())
                        .anyMatch(m -> m.getName().equals("rbac") && Modifier.isStatic(m.getModifiers()))
                )
                .map(RbacView::castToSubclassOfBaseEntity)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends BaseEntity> castToSubclassOfBaseEntity(final Class<?> clazz) {
        return (Class<? extends BaseEntity>) clazz;
    }

    /**
     * This main method generates the RbacViews (PostgreSQL+diagram) for all given entity classes.
     */
    public static void main(String[] args) throws Exception {
        findRbacEntityClasses("net.hostsharing.hsadminng")
                .forEach(RbacView::generateRbacView);
    }
}
