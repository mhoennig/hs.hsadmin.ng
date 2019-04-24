package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.DtoLoader;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

abstract class JSonAccessFilter<T> {
    private final ApplicationContext ctx;
    final T dto;
    final Field selfIdField;
    final Field parentIdField;

    JSonAccessFilter(final ApplicationContext ctx, final T dto) {
        this.ctx = ctx;
        this.dto = dto;
        this.selfIdField = determineFieldWithAnnotation(dto.getClass(), SelfId.class);
        this.parentIdField = determineFieldWithAnnotation(dto.getClass(), ParentId.class);
    }

    boolean isParentIdField(final Field field) {
        return field.equals(parentIdField);
    }

    Long getId() {
        if (selfIdField == null) {
            return null;
        }
        return (Long) ReflectionUtil.getValue(dto, selfIdField);
    }

    /**
     * @param field to get a display representation for
     * @return a simplified, decently user readable, display representation of the given field
     */
    String toDisplay(final Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }

    /**
     * @return the role of the login user in relation to the dto, this filter is created for.
     */
    Role getLoginUserRole() {
        final Role roleOnSelf = getLoginUserRoleOnSelf();
        if ( roleOnSelf.isIndependent() ) {
            return roleOnSelf;
        }
        return getLoginUserRoleOnAncestorOfDtoClassIfHigher(roleOnSelf, dto);
    }

    private Role getLoginUserRoleOnSelf() {
        return SecurityUtils.getLoginUserRoleFor(dto.getClass(), getId() );
    }

    private Role getLoginUserRoleOnAncestorOfDtoClassIfHigher(final Role baseRole, final Object dto) {
        final Field parentIdField = determineFieldWithAnnotation(dto.getClass(), ParentId.class);

        if ( parentIdField == null ) {
            return baseRole;
        }

        final ParentId parentIdAnnot = parentIdField.getAnnotation(ParentId.class);
        final Class<?> parentDtoClass = parentIdAnnot.value();
        final Long parentId = (Long) ReflectionUtil.getValue(dto, parentIdField);
        final Role roleOnParent = SecurityUtils.getLoginUserRoleFor(parentDtoClass, parentId);

        final Object parentEntity = findParentDto(parentDtoClass, parentId);
        return Role.broadest(baseRole, getLoginUserRoleOnAncestorOfDtoClassIfHigher(roleOnParent, parentEntity));
    }

    private Object findParentDto(Class<?> parentDtoClass, Long parentId) {
        // TODO: generalize, e.g. via "all beans that implement DtoLoader<CustomerDTO>
        if ( parentDtoClass == MembershipDTO.class ) {
            final DtoLoader<MembershipDTO> dtoLoader = ctx.getAutowireCapableBeanFactory().createBean(MembershipService.class);
            return dtoLoader.findOne(parentId).get();
        }
        if ( parentDtoClass == CustomerDTO.class ) {
            final DtoLoader<CustomerDTO> dtoLoader = ctx.getAutowireCapableBeanFactory().createBean(CustomerService.class);
            return dtoLoader.findOne(parentId).get();
        }
        throw new NotImplementedException("no DtoLoader implemented for " + parentDtoClass);
    }

    private static Field determineFieldWithAnnotation(final Class<?> dtoClass, final Class<? extends Annotation> idAnnotationClass) {
        Field parentIdField = null;
        for (Field field : dtoClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(idAnnotationClass)) {
                if (parentIdField != null) {
                    throw new AssertionError("multiple @" + idAnnotationClass.getSimpleName() + " detected in " + field.getDeclaringClass().getSimpleName());
                }
                parentIdField = field;
            }
        }
        return parentIdField;
    }
}
