// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.ArrayUtils;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.User;
import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.security.AuthoritiesConstants;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Verify.verify;
import static org.hostsharing.hsadminng.service.util.ReflectionUtil.initialize;

/**
 * These enum values are used to specify the minimum role required to grant access to resources,
 * see usages of {@link AccessFor}.
 * Also they can be assigned to users via {@link UserRoleAssignment}.
 * Some of the concrete values make only sense in one of these contexts.
 * <p>
 * There are two kinds of roles: independent and dependent.
 * Independent roles like {@link Hostmaster} are absolute roles which means unrelated to any concrete entity.
 * Dependent roles like {@link CustomerContractualContact} are relative to a specific entity,
 * in this case to a specific {@link Customer}.
 * <p>
 * <p>
 * Separate classes are used to make it possible to use roles in Java annotations
 * and also make it possible to have roles spread over multiple modules.
 * </p>
 */
public abstract class Role {

    // TODO mhoennig: We need to make sure that the classes are loaded
    // and thus the static initializers were called
    // before these maps are used in production code.
    private static Map<Class<? extends Role>, Role> rolesByClass = new HashMap<>();
    private static Map<String, Role> rolesByName = new HashMap<>();

    private final String authority;
    private final LazyRoles comprises;

    Role() {
        this.authority = AuthoritiesConstants.USER;
        // noinspection unchecked
        this.comprises = new LazyRoles();
    }

    @SafeVarargs
    Role(final Class<? extends Role>... comprisedRoleClasses) {
        this.authority = AuthoritiesConstants.USER;
        // noinspection unchecked
        this.comprises = new LazyRoles(comprisedRoleClasses);
    }

    @SafeVarargs
    Role(final String authority, final Class<? extends Role>... comprisedRoleClasses) {
        this.authority = authority;
        // noinspection unchecked
        this.comprises = new LazyRoles(comprisedRoleClasses);
    }

    public static Role of(final String authority) {
        final Role role = rolesByName.get(authority);
        verify(
                role != null,
                "unknown authority: %s, available authorities: ",
                authority,
                ArrayUtils.toString(rolesByName.keySet()));
        return role;
    }

    public static <T extends Role> T of(final Class<T> roleClass) {
        // prevent initialization and thus recursive call to `Role.of(...)` within `newInstance(...)`
        final Class<T> initializedRoleClass = initialize(roleClass);
        {
            final T role = (T) rolesByClass.get(initializedRoleClass);
            if (role != null) {
                return role;
            }
        }
        {
            T newRole = (T) ReflectionUtil.newInstance(initializedRoleClass);
            rolesByClass.put(initializedRoleClass, newRole);
            rolesByName.put(newRole.name(), newRole);
            return newRole;
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + name() + ")";
    }

    public abstract String name();

    public static class IndependentRole extends Role {

        @SafeVarargs
        IndependentRole(final String authority, final Class<? extends Role>... comprisedRoleClasses) {
            super(authority, comprisedRoleClasses);
        }

        public String name() {
            return authority();
        }
    }

    public static class DependentRole extends Role {

        DependentRole() {
        }

        @SafeVarargs
        DependentRole(final Class<? extends Role>... comprisedRoleClasses) {
            super(AuthoritiesConstants.USER, comprisedRoleClasses);
        }

        public String name() {
            return getClass().getSimpleName(); // TODO: decide if it's ok for use in the DB table
        }
    }

    /**
     * Default for access rights requirement. You can read it as: 'Nobody is allowed to ...'.
     * This is usually used for fields which are managed by hsadminNg itself.
     * <p>
     * This role cannot be assigned to a user.
     * </p>
     */
    public static class Nobody extends DependentRole {

        public static final Nobody ROLE = Role.of(Nobody.class);
    }

    /**
     * Hostmasters are initialize/update/read and field which, except where NOBODY is allowed to.
     */
    public static class Hostmaster extends IndependentRole {

        /**
         * Hostmasters role to be assigned to users via via {@link User#setAuthorities}.
         */
        public static final Hostmaster ROLE = Role.of(Hostmaster.class);

        Hostmaster() {
            super(AuthoritiesConstants.HOSTMASTER, Admin.class);
        }
    }

    public static class Admin extends IndependentRole {

        public static final Admin ROLE = Role.of(Admin.class);

        Admin() {
            super(AuthoritiesConstants.ADMIN, Supporter.class);
        }
    }

    public static class Supporter extends IndependentRole {

        public static final Supporter ROLE = Role.of(Supporter.class);

        Supporter() {
            super(AuthoritiesConstants.SUPPORTER, CustomerContractualContact.class);
        }
    }

    /**
     * This role is for contractual contacts of a customer, like a director of the company.
     * <p>
     * Who has this role, has the broadest access to all resources which belong to this customer.
     * Everything which relates to the contract with the customer, needs this role.
     * <p>
     * This role can be assigned to a user via {@link UserRoleAssignment}.
     * </p>
     */
    public static class CustomerContractualContact extends DependentRole {

        public static final CustomerContractualContact ROLE = Role.of(CustomerContractualContact.class);

        CustomerContractualContact() {
            super(CustomerFinancialContact.class, CustomerTechnicalContact.class);
        }
    }

    public static class CustomerFinancialContact extends DependentRole {

        public static final CustomerFinancialContact ROLE = Role.of(CustomerFinancialContact.class);

        CustomerFinancialContact() {
            super(AnyCustomerContact.class);
        }
    }

    public static class CustomerTechnicalContact extends DependentRole {

        public static final CustomerTechnicalContact ROLE = Role.of(CustomerTechnicalContact.class);

        CustomerTechnicalContact() {
            super(
                  AnyCustomerContact.class,
                  AnyCustomerUser.class); // TODO mhoennig: how to add roles of other modules?
        }
    }

    public static class AnyCustomerContact extends DependentRole {

        public static final AnyCustomerContact ROLE = Role.of(AnyCustomerContact.class);

        AnyCustomerContact() {
            super(Anybody.class);
        }
    }

    public static class ActualCustomerUser extends DependentRole {

        public static final ActualCustomerUser ROLE = Role.of(ActualCustomerUser.class);

        ActualCustomerUser() {
            super(AnyCustomerUser.class);
        }
    }

    public static class AnyCustomerUser extends DependentRole {

        public static final Role ROLE = Role.of(AnyCustomerUser.class);

        AnyCustomerUser() {
            super(Anybody.class);
        }
    }

    /**
     * This role is meant to specify that a resources can be accessed by anybody, even without login.
     * <p>
     * It can be used to specify to grant rights to any use, even if unauthorized.
     * </p>
     */
    public static class Anybody extends IndependentRole {

        public static final Role ROLE = Role.of(Anybody.class);

        Anybody() {
            super(AuthoritiesConstants.ANONYMOUS);
        }
    }

    /**
     * Pseudo-role to mark init/update access as ignored because the field is display-only.
     * <p>
     * This allows REST clients to send the whole response back as a new update request.
     * This role is not covered by any and covers itself no role.
     * <p>
     * It's only used to ignore the field.
     * </p>
     */
    public static class Ignored extends DependentRole {

        public static final Role ROLE = Role.of(Ignored.class);
    }

    /**
     * @param field a field of a DTO with AccessMappings
     * @return true if update access can be ignored because the field is just for display anyway
     */
    public static boolean toBeIgnoredForUpdates(final Field field) {
        final AccessFor accessForAnnot = field.getAnnotation(AccessFor.class);
        if (accessForAnnot == null) {
            return true;
        }
        final Class<? extends Role>[] updateAccessFor = field.getAnnotation(AccessFor.class).update();
        return updateAccessFor.length == 1 && updateAccessFor[0] == Ignored.class;
    }

    /**
     * @return the independent authority related 1:1 to this Role or empty if no independent authority is related 1:1
     * @see AuthoritiesConstants
     */
    public String authority() {
        return authority;
    }

    /**
     * @return the role with the broadest access rights
     */
    public static Role broadest(final Role role, final Role... roles) {
        Role broadests = role;
        for (Role r : roles) {
            if (r.covers(broadests.getClass())) {
                broadests = r;
            }
        }
        return broadests;
    }

    /**
     * Determines if 'this' actual role covered the given required role.
     * <p>
     * Where 'this' means the Java instance itself as a role of a system user.
     * <p>
     * {@code
     * AssignedHostmaster.ROLE.covers(AssignedRole.ANY_CUSTOMER_USER) == true
     * }
     *
     * @param roleClass The required role for a resource.
     * @return whether this role comprises the given role
     */
    public boolean covers(final Class<? extends Role> roleClass) {
        if (getClass() == Ignored.class || roleClass == Ignored.class) {
            return false;
        }
        if (getClass() == roleClass) {
            return true;
        }
        for (Role role : comprises.get()) {
            if (role.covers(roleClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if 'this' actual role covers any of the given required roles.
     * <p>
     * Where 'this' means the Java instance itself as a role of a system user.
     * <p>
     * {@code
     * AssignedHostmaster.ROLE.coversAny(AssignedRole.CUSTOMER_CONTRACTUAL_CONTACT, AssignedRole.CUSTOMER_FINANCIAL_CONTACT) == true
     * }
     *
     * @param roleClasses The alternatively required roles for a resource. Must be at least one.
     * @return whether this role comprises any of the given roles
     */
    public boolean coversAny(final Class<? extends Role>... roleClasses) {
        verify(roleClasses != null && roleClasses.length > 0, "role classes expected");

        for (Class<? extends Role> roleClass : roleClasses) {
            if (this.covers(roleClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this role of a user allows to initialize the given field when creating the resource.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToInit(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return coversAny(accessFor.init());
    }

    /**
     * Checks if this role of a user allows to update the given field.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToUpdate(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return coversAny(accessFor.update());
    }

    /**
     * Checks if this role of a user allows to read the given field.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToRead(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return coversAny(accessFor.read());
    }
}

class LazyRoles {

    private final Class<? extends Role>[] comprisedRoleClasses;
    private Role[] comprisedRoles = null;

    LazyRoles(Class<? extends Role>... comprisedRoleClasses) {
        this.comprisedRoleClasses = comprisedRoleClasses;
    }

    Role[] get() {
        if (comprisedRoles == null) {
            comprisedRoles = new Role[comprisedRoleClasses.length];
            for (int n = 0; n < comprisedRoleClasses.length; ++n) {
                comprisedRoles[n] = Role.of(comprisedRoleClasses[n]);
            }
        }
        return comprisedRoles;
    }
}
