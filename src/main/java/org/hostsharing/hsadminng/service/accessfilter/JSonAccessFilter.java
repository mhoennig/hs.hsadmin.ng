package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import java.lang.reflect.Field;

abstract class JSonAccessFilter<T> {
    final T dto;
    Field selfIdField = null;
    Field parentIdField = null;

    JSonAccessFilter(final T dto) {
        this.dto = dto;
        determineIdFields();
    }

    void determineIdFields() {
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SelfId.class)) {
                if (selfIdField != null) {
                    throw new AssertionError("multiple @" + SelfId.class.getSimpleName() + " detected in " + field.getDeclaringClass().getSimpleName());
                }
                selfIdField = field;
            }
            if (field.isAnnotationPresent(ParentId.class)) {
                if (parentIdField != null) {
                    throw new AssertionError("multiple @" + ParentId.class.getSimpleName() + " detected in " + field.getDeclaringClass().getSimpleName());
                }
                parentIdField = field;
            }
        }
    }

    Long getId() {
        if (selfIdField == null) {
            return null;
        }
        return (Long) ReflectionUtil.getValue(dto, selfIdField);
    }

    String toDisplay(final Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }

    Role getLoginUserRole() {
        final Role roleOnSelf = getLoginUserRoleOnSelf();
        final Role roleOnParent = getLoginUserRoleOnParent();
        return roleOnSelf.covers(roleOnParent) ? roleOnSelf : roleOnParent;
    }


    private Role getLoginUserRoleOnSelf() {
        // TODO: find broadest role in self and recursively in parent
        return SecurityUtils.getLoginUserRoleFor(dto.getClass(), getId() );
    }

    private Role getLoginUserRoleOnParent() {
        if ( parentIdField == null ) {
            return Role.ANYBODY;
        }
        final ParentId parentId = parentIdField.getAnnotation(ParentId.class);
        return SecurityUtils.getLoginUserRoleFor(parentId.value(), (Long) ReflectionUtil.getValue(dto, parentIdField) );
    }
}
