// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static com.google.common.base.Verify.verify;

import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.IdToDtoResolver;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
        if (roleOnSelf.isIndependent()) {
            return roleOnSelf;
        }
        return getLoginUserRoleOnAncestorOfDtoClassIfHigher(roleOnSelf, dto);
    }

    private Role getLoginUserRoleOnSelf() {
        return SecurityUtils.getLoginUserRoleFor(dto.getClass(), getId());
    }

    private Role getLoginUserRoleOnAncestorOfDtoClassIfHigher(final Role baseRole, final Object dto) {
        final Field parentIdField = determineFieldWithAnnotation(dto.getClass(), ParentId.class);

        if (parentIdField == null) {
            return baseRole;
        }

        final ParentId parentIdAnnot = parentIdField.getAnnotation(ParentId.class);
        final Class<? extends IdToDtoResolver> parentDtoLoader = parentIdAnnot.resolver();
        final Class<IdToDtoResolver> rawType = IdToDtoResolver.class;

        final Class<?> parentDtoClass = ReflectionUtil.<T> determineGenericInterfaceParameter(parentDtoLoader, rawType, 0);
        final Long parentId = ReflectionUtil.getValue(dto, parentIdField);
        final Role roleOnParent = SecurityUtils.getLoginUserRoleFor(parentDtoClass, parentId);

        final Object parentEntity = loadDto(parentDtoLoader, parentId);
        return Role.broadest(baseRole, getLoginUserRoleOnAncestorOfDtoClassIfHigher(roleOnParent, parentEntity));
    }

    @SuppressWarnings("unchecked")
    protected Object loadDto(final Class<? extends IdToDtoResolver> resolverClass, final Long id) {
        verify(id != null, "id must not be null for " + resolverClass.getSimpleName());

        final AutowireCapableBeanFactory beanFactory = ctx.getAutowireCapableBeanFactory();
        verify(
                beanFactory != null,
                "no bean factory found, probably missing mock configuration for ApplicationContext, e.g. given(...)");

        final IdToDtoResolver<MembershipDTO> resolverBean = beanFactory.createBean(resolverClass);
        verify(
                resolverBean != null,
                "no " + resolverClass.getSimpleName()
                        + " bean created, probably missing mock configuration for AutowireCapableBeanFactory, e.g. given(...)");

        return resolverBean.findOne(id)
                .orElseThrow(
                        () -> new BadRequestAlertException(
                                "Can't resolve entity ID " + id + " via " + resolverClass,
                                resolverClass.getSimpleName(),
                                "isNotFound"));
    }

    private static Field determineFieldWithAnnotation(
            final Class<?> dtoClass,
            final Class<? extends Annotation> idAnnotationClass) {
        Field parentIdField = null;
        for (Field field : dtoClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(idAnnotationClass)) {
                if (parentIdField != null) {
                    throw new AssertionError(
                            "multiple @" + idAnnotationClass.getSimpleName() + " detected in "
                                    + field.getDeclaringClass().getSimpleName());
                }
                parentIdField = field;
            }
        }
        return parentIdField;
    }
}
