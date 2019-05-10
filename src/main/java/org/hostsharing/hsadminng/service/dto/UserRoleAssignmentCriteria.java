// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.Role;

import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the UserRoleAssignment entity. This class is used in UserRoleAssignmentResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /user-role-assignments?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class UserRoleAssignmentCriteria implements Serializable {

    /**
     * Class for filtering UserRole
     */
    public static class UserRoleFilter extends Filter<Role> {
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter entityTypeId;

    private LongFilter entityObjectId;

    private UserRoleFilter assignedRole;

    private LongFilter userId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(StringFilter entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public LongFilter getEntityObjectId() {
        return entityObjectId;
    }

    public void setEntityObjectId(LongFilter entityObjectId) {
        this.entityObjectId = entityObjectId;
    }

    public UserRoleFilter getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(UserRoleFilter assignedRole) {
        this.assignedRole = assignedRole;
    }

    public LongFilter getUserId() {
        return userId;
    }

    public void setUserId(LongFilter userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserRoleAssignmentCriteria that = (UserRoleAssignmentCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entityTypeId, that.entityTypeId) &&
                Objects.equals(entityObjectId, that.entityObjectId) &&
                Objects.equals(assignedRole, that.assignedRole) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                entityTypeId,
                entityObjectId,
                assignedRole,
                userId);
    }

    @Override
    public String toString() {
        return "UserRoleAssignmentCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (entityTypeId != null ? "entityTypeId=" + entityTypeId + ", " : "") +
                (entityObjectId != null ? "entityObjectId=" + entityObjectId + ", " : "") +
                (assignedRole != null ? "assignedRole=" + assignedRole + ", " : "") +
                (userId != null ? "userId=" + userId + ", " : "") +
                "}";
    }

}
