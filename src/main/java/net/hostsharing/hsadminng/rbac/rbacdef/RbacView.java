package net.hostsharing.hsadminng.rbac.rbacdef;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionEntity;
import net.hostsharing.hsadminng.hs.office.coopshares.HsOfficeCoopSharesTransactionEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerDetailsEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relationship.HsOfficeRelationshipEntity;
import net.hostsharing.hsadminng.hs.office.sepamandate.HsOfficeSepaMandateEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.test.cust.TestCustomerEntity;
import net.hostsharing.hsadminng.test.dom.TestDomainEntity;
import net.hostsharing.hsadminng.test.pac.TestPackageEntity;

import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.autoFetched;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

@Getter
public class RbacView {

    public static final String GLOBAL = "global";
    public static final String OUTPUT_BASEDIR = "src/main/resources/db/changelog";

    private final EntityAlias rootEntityAlias;

    private final Set<RbacUserReference> userDefs = new LinkedHashSet<>();
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

    private SQL identityViewSqlQuery;
    private SQL orderBySqlExpression;
    private EntityAlias rootEntityAliasProxy;
    private RbacRoleDefinition previousRoleDef;

    public static <E extends RbacObject> RbacView rbacViewFor(final String alias, final Class<E> entityClass) {
        return new RbacView(alias, entityClass);
    }

    RbacView(final String alias, final Class<? extends RbacObject> entityClass) {
        rootEntityAlias = new EntityAlias(alias, entityClass);
        entityAliases.put(alias, rootEntityAlias);
        new RbacUserReference(CREATOR);
        entityAliases.put("global", new EntityAlias("global"));
    }

    public RbacView withUpdatableColumns(final String... columnNames) {
        Collections.addAll(updatableColumns, columnNames);
        verifyVersionColumnExists();
        return this;
    }

    public RbacView withIdentityView(final SQL sqlExpression) {
        this.identityViewSqlQuery = sqlExpression;
        return this;
    }

    public RbacView withRestrictedViewOrderBy(final SQL orderBySqlExpression) {
        this.orderBySqlExpression = orderBySqlExpression;
        return this;
    }

    public RbacView createRole(final Role role, final Consumer<RbacRoleDefinition> with) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        with.accept(newRoleDef);
        previousRoleDef = newRoleDef;
        return this;
    }

    public RbacView createSubRole(final Role role) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        findOrCreateGrantDef(newRoleDef, previousRoleDef).toCreate();
        previousRoleDef = newRoleDef;
        return this;
    }

    public RbacView createSubRole(final Role role, final Consumer<RbacRoleDefinition> with) {
        final RbacRoleDefinition newRoleDef = findRbacRole(rootEntityAlias, role).toCreate();
        findOrCreateGrantDef(newRoleDef, previousRoleDef).toCreate();
        with.accept(newRoleDef);
        previousRoleDef = newRoleDef;
        return this;
    }

    public RbacPermissionDefinition createPermission(final Permission permission) {
        return createPermission(rootEntityAlias, permission);
    }

    public RbacPermissionDefinition createPermission(final String entityAliasName, final Permission permission) {
        return createPermission(findEntityAlias(entityAliasName), permission);
    }

    private RbacPermissionDefinition createPermission(final EntityAlias entityAlias, final Permission permission) {
        return new RbacPermissionDefinition(entityAlias, permission, null, true);
    }

    public <EC extends RbacObject> RbacView declarePlaceholderEntityAliases(final String... aliasNames) {
        for (String alias : aliasNames) {
            entityAliases.put(alias, new EntityAlias(alias));
        }
        return this;
    }

    public <EC extends RbacObject> RbacView importRootEntityAliasProxy(
            final String aliasName,
            final Class<? extends HasUuid> entityClass,
            final SQL fetchSql,
            final Column dependsOnColum) {
        if (rootEntityAliasProxy != null) {
            throw new IllegalStateException("there is already an entityAliasProxy: " + rootEntityAliasProxy);
        }
        rootEntityAliasProxy = importEntityAliasImpl(aliasName, entityClass, fetchSql, dependsOnColum, false);
        return this;
    }

    public RbacView importSubEntityAlias(
            final String aliasName, final Class<? extends HasUuid> entityClass,
            final SQL fetchSql, final Column dependsOnColum) {
        importEntityAliasImpl(aliasName, entityClass, fetchSql, dependsOnColum, true);
        return this;
    }

    public RbacView importEntityAlias(
            final String aliasName, final Class<? extends HasUuid> entityClass,
            final Column dependsOnColum, final SQL fetchSql) {
        importEntityAliasImpl(aliasName, entityClass, fetchSql, dependsOnColum, false);
        return this;
    }

    public RbacView importEntityAlias(
            final String aliasName, final Class<? extends HasUuid> entityClass,
            final Column dependsOnColum) {
        importEntityAliasImpl(aliasName, entityClass, autoFetched(), dependsOnColum, false);
        return this;
    }

    private EntityAlias importEntityAliasImpl(
            final String aliasName, final Class<? extends HasUuid> entityClass,
            final SQL fetchSql, final Column dependsOnColum, boolean asSubEntity) {
        final var entityAlias = new EntityAlias(aliasName, entityClass, fetchSql, dependsOnColum, asSubEntity);
        entityAliases.put(aliasName, entityAlias);
        try {
            importAsAlias(aliasName, rbacDefinition(entityClass), asSubEntity);
        } catch (final ReflectiveOperationException exc) {
            throw new RuntimeException("cannot import entity: " + entityClass, exc);
        }
        return entityAlias;
    }

    private static RbacView rbacDefinition(final Class<? extends RbacObject> entityClass)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (RbacView) entityClass.getMethod("rbac").invoke(null);
    }

    private RbacView importAsAlias(final String aliasName, final RbacView importedRbacView, final boolean asSubEntity) {
        final var mapper = new AliasNameMapper(importedRbacView, aliasName,
                asSubEntity ? entityAliases.keySet() : null);
        importedRbacView.getEntityAliases().values().stream()
                .filter(entityAlias -> !importedRbacView.isRootEntityAlias(entityAlias))
                .filter(entityAlias -> !entityAlias.isGlobal())
                .filter(entityAlias -> !asSubEntity || !entityAliases.containsKey(entityAlias.aliasName))
                .forEach(entityAlias -> {
                    final String mappedAliasName = mapper.map(entityAlias.aliasName);
                    entityAliases.put(mappedAliasName, new EntityAlias(mappedAliasName, entityAlias.entityClass));
                });
        importedRbacView.getRoleDefs().forEach(roleDef -> {
            new RbacRoleDefinition(findEntityAlias(mapper.map(roleDef.entityAlias.aliasName)), roleDef.role);
        });
        importedRbacView.getGrantDefs().forEach(grantDef -> {
            if (grantDef.grantType() == RbacGrantDefinition.GrantType.ROLE_TO_ROLE) {
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

    private void verifyVersionColumnExists() {
        if (stream(rootEntityAlias.entityClass.getDeclaredFields())
                .noneMatch(f -> f.getAnnotation(Version.class) != null)) {
            // TODO: convert this into throw Exception once RbacEntity is a base class with @Version field
            System.err.println("@Version field required in updatable entity " + rootEntityAlias.entityClass);
        }
    }

    public RbacGrantBuilder toRole(final String entityAlias, final Role role) {
        return new RbacGrantBuilder(entityAlias, role);
    }

    public RbacExampleRole forExampleRole(final String entityAlias, final Role role) {
        return new RbacExampleRole(entityAlias, role);
    }

    private RbacGrantDefinition grantRoleToUser(final RbacRoleDefinition roleDefinition, final RbacUserReference user) {
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
        new RbacViewMermaidFlowchartGenerator(this).generateToMarkdownFile(Path.of(OUTPUT_BASEDIR, baseFileName + ".md"));
        new RbacViewPostgresGenerator(this).generateToChangeLog(Path.of(OUTPUT_BASEDIR, baseFileName + ".sql"));
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

        public RbacView grantPermission(final String entityAliasName, final Permission perm) {
            final var entityAlias = findEntityAlias(entityAliasName);
            final var forTable = entityAlias.getRawTableName();
            findOrCreateGrantDef(findRbacPerm(entityAlias, perm, forTable), superRoleDef).toCreate();
            return RbacView.this;
        }

    }

    @Getter
    @EqualsAndHashCode
    public class RbacGrantDefinition {

        private final RbacUserReference userDef;
        private final RbacRoleDefinition superRoleDef;
        private final RbacRoleDefinition subRoleDef;
        private final RbacPermissionDefinition permDef;
        private boolean assumed = true;
        private boolean toCreate = false;

        @Override
        public String toString() {
            final var arrow = isAssumed() ? " --> " : " -- // --> ";
            return switch (grantType()) {
                case ROLE_TO_USER -> userDef.toString() + arrow + subRoleDef.toString();
                case ROLE_TO_ROLE -> superRoleDef + arrow + subRoleDef;
                case PERM_TO_ROLE -> superRoleDef + arrow + permDef;
            };
        }

        RbacGrantDefinition(final RbacRoleDefinition subRoleDef, final RbacRoleDefinition superRoleDef) {
            this.userDef = null;
            this.subRoleDef = subRoleDef;
            this.superRoleDef = superRoleDef;
            this.permDef = null;
            register(this);
        }

        public RbacGrantDefinition(final RbacPermissionDefinition permDef, final RbacRoleDefinition roleDef) {
            this.userDef = null;
            this.subRoleDef = null;
            this.superRoleDef = roleDef;
            this.permDef = permDef;
            register(this);
        }

        public RbacGrantDefinition(final RbacRoleDefinition roleDef, final RbacUserReference userDef) {
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
            return permDef != null ? GrantType.PERM_TO_ROLE
                    : userDef != null ? GrantType.ROLE_TO_USER
                    : GrantType.ROLE_TO_ROLE;
        }

        boolean isAssumed() {
            return assumed;
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

        public void unassumed() {
            this.assumed = false;
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

        private RbacPermissionDefinition(final EntityAlias entityAlias, final Permission permission, final String tableName, final boolean toCreate) {
            this.entityAlias = entityAlias;
            this.permission = permission;
            this.tableName = tableName;
            this.toCreate = toCreate;
            permDefs.add(this);
        }

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

        public RbacGrantDefinition owningUser(final RbacUserReference.UserRole userRole) {
            return grantRoleToUser(this, findUserRef(userRole));
        }

        public RbacGrantDefinition permission(final Permission permission) {
            return grantPermissionToRole(createPermission(entityAlias, permission), this);
        }

        public RbacGrantDefinition incomingSuperRole(final String entityAlias, final Role role) {
            final var incomingSuperRole = findRbacRole(entityAlias, role);
            return grantSubRoleToSuperRole(this, incomingSuperRole);
        }

        public RbacGrantDefinition outgoingSubRole(final String entityAlias, final Role role) {
            final var outgoingSubRole = findRbacRole(entityAlias, role);
            return grantSubRoleToSuperRole(outgoingSubRole, this);
        }

        @Override
        public String toString() {
            return "role:" + entityAlias.aliasName + role;
        }
    }

    public RbacUserReference findUserRef(final RbacUserReference.UserRole userRole) {
        return userDefs.stream().filter(u -> u.role == userRole).findFirst().orElseThrow();
    }

    @EqualsAndHashCode
    public class RbacUserReference {

        public enum UserRole {
            GLOBAL_ADMIN,
            CREATOR
        }

        final UserRole role;

        public RbacUserReference(final UserRole creator) {
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


    RbacPermissionDefinition findRbacPerm(final EntityAlias entityAlias, final Permission perm) {
        return findRbacPerm(entityAlias, perm, null);
    }

    public RbacPermissionDefinition findRbacPerm(final String entityAliasName, final Permission perm, String tableName) {
        return findRbacPerm(findEntityAlias(entityAliasName), perm, tableName);
    }

    public RbacPermissionDefinition findRbacPerm(final String entityAliasName, final Permission perm) {
        return findRbacPerm(findEntityAlias(entityAliasName), perm);
    }

    private RbacGrantDefinition findOrCreateGrantDef(final RbacRoleDefinition roleDefinition, final RbacUserReference user) {
        return grantDefs.stream()
                .filter(g -> g.subRoleDef == roleDefinition && g.userDef == user)
                .findFirst()
                .orElseGet(() -> new RbacGrantDefinition(roleDefinition, user));
    }

    private RbacGrantDefinition findOrCreateGrantDef(final RbacPermissionDefinition permDef, final RbacRoleDefinition roleDef) {
        return grantDefs.stream()
                .filter(g -> g.permDef == permDef && g.subRoleDef == roleDef)
                .findFirst()
                .orElseGet(() -> new RbacGrantDefinition(permDef, roleDef));
    }

    private RbacGrantDefinition findOrCreateGrantDef(
            final RbacRoleDefinition subRoleDefinition,
            final RbacRoleDefinition superRoleDefinition) {
        return grantDefs.stream()
                .filter(g -> g.subRoleDef == subRoleDefinition && g.superRoleDef == superRoleDefinition)
                .findFirst()
                .orElseGet(() -> new RbacGrantDefinition(subRoleDefinition, superRoleDefinition));
    }

    record EntityAlias(String aliasName, Class<? extends RbacObject> entityClass, SQL fetchSql, Column dependsOnColum, boolean isSubEntity) {

        public EntityAlias(final String aliasName) {
            this(aliasName, null, null, null, false);
        }

        public EntityAlias(final String aliasName, final Class<? extends RbacObject> entityClass) {
            this(aliasName, entityClass, null, null, false);
        }

        boolean isGlobal() {
            return aliasName().equals("global");
        }

        boolean isPlaceholder() {
            return entityClass == null;
        }

        @NotNull
        @Override
        public SQL fetchSql() {
            if (fetchSql == null) {
                return SQL.noop();
            }
            return switch (fetchSql.part) {
                case SQL_QUERY -> fetchSql;
                case AUTO_FETCH ->
                        SQL.query("SELECT * FROM " + getRawTableName() + " WHERE uuid = ${ref}." + dependsOnColum.column);
                default -> throw new IllegalStateException("unexpected SQL definition: " + fetchSql);
            };
        }

        public boolean hasFetchSql() {
            return fetchSql != null;
        }

        private String withoutEntitySuffix(final String simpleEntityName) {
            return simpleEntityName.substring(0, simpleEntityName.length() - "Entity".length());
        }

        String simpleName() {
            return isGlobal()
                    ? aliasName
                    : uncapitalize(withoutEntitySuffix(entityClass.getSimpleName()));
        }

        String getRawTableName() {
            if ( aliasName.equals("global")) {
                return "global"; // TODO: maybe we should introduce a GlobalEntity class?
            }
            return withoutRvSuffix(entityClass.getAnnotation(Table.class).name());
        }

        String dependsOnColumName() {
            if (dependsOnColum == null) {
                throw new IllegalStateException(
                        "Entity " + aliasName + "(" + entityClass.getSimpleName() + ")" + ": please add dependsOnColum");
            }
            return dependsOnColum.column;
        }
    }

    public static String withoutRvSuffix(final String tableName) {
        return tableName.substring(0, tableName.length() - "_rv".length());
    }

    public record Role(String roleName) {

        public static final Role OWNER = new Role("owner");
        public static final Role ADMIN = new Role("admin");
        public static final Role AGENT = new Role("agent");
        public static final Role TENANT = new Role("tenant");
        public static final Role REFERRER = new Role("referrer");

        @Override
        public String toString() {
            return ":" + roleName;
        }

        @Override
        public boolean equals(final Object obj) {
            return ((obj instanceof Role) && ((Role) obj).roleName.equals(this.roleName));
        }
    }

    public record Permission(String permission) {

        public static final Permission INSERT = new Permission("INSERT");
        public static final Permission DELETE = new Permission("DELETE");
        public static final Permission UPDATE = new Permission("UPDATE");
        public static final Permission SELECT = new Permission("SELECT");

        public static Permission custom(final String permission) {
            return new Permission(permission);
        }

        @Override
        public String toString() {
            return ":" + permission;
        }
    }

    public static class SQL {

        /**
         * DSL method to specify an SQL SELECT expression which fetches the related entity,
         * using the reference `${ref}` of the root entity.
         * `${ref}` is going to be replaced by either `NEW` or `OLD` of the trigger function.
         * `into ...` will be added with a variable name prefixed with either `new` or `old`.
         *
         * @param sql an SQL SELECT expression (not ending with ';)
         * @return the wrapped SQL expression
         */
        public static SQL fetchedBySql(final String sql) {
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
        public static SQL autoFetched() {
            return new SQL(null, Part.AUTO_FETCH);
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
            if (outerAliasNames.contains(originalAliasName) || originalAliasName.equals("global")) {
                return originalAliasName;
            }
            if (originalAliasName.equals(importedRbacView.rootEntityAlias.aliasName)) {
                return outerAliasName;
            }
            return outerAliasName + "." + originalAliasName;
        }
    }

    public static void main(String[] args) {
        Stream.of(
                TestCustomerEntity.class,
                TestPackageEntity.class,
                TestDomainEntity.class,
                HsOfficePersonEntity.class,
                HsOfficePartnerEntity.class,
                HsOfficePartnerDetailsEntity.class,
                HsOfficeBankAccountEntity.class,
                HsOfficeDebitorEntity.class,
                HsOfficeRelationshipEntity.class,
                HsOfficeCoopAssetsTransactionEntity.class,
                HsOfficeContactEntity.class,
                HsOfficeSepaMandateEntity.class,
                HsOfficeCoopSharesTransactionEntity.class,
                HsOfficeMembershipEntity.class
        ).forEach(c -> {
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
                System.err.println("no main method in: " + c.getName());
            }
        });
    }
}
