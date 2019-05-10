// Licensed under Apache-2.0
package org.hostsharing.hsadminng.domain;

import org.hostsharing.hsadminng.service.accessfilter.Role;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * A UserRoleAssignment.
 */
@Entity
@Table(name = "user_role_assignment")
public class UserRoleAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Size(max = 32)
    @Column(name = "entity_type_id", length = 32, nullable = false)
    private String entityTypeId;

    @NotNull
    @Column(name = "entity_object_id", nullable = false)
    private Long entityObjectId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    private Role assignedRole;

    @ManyToOne
    @JsonIgnoreProperties("requireds")
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityTypeId() {
        return entityTypeId;
    }

    public UserRoleAssignment entityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
        return this;
    }

    public void setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public Long getEntityObjectId() {
        return entityObjectId;
    }

    public UserRoleAssignment entityObjectId(Long entityObjectId) {
        this.entityObjectId = entityObjectId;
        return this;
    }

    public void setEntityObjectId(Long entityObjectId) {
        this.entityObjectId = entityObjectId;
    }

    public Role getAssignedRole() {
        return assignedRole;
    }

    public UserRoleAssignment assignedRole(Role assignedRole) {
        this.assignedRole = assignedRole;
        return this;
    }

    public void setAssignedRole(Role assignedRole) {
        this.assignedRole = assignedRole;
    }

    public User getUser() {
        return user;
    }

    public UserRoleAssignment user(User user) {
        this.user = user;
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRoleAssignment userRoleAssignment = (UserRoleAssignment) o;
        if (userRoleAssignment.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), userRoleAssignment.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "UserRoleAssignment{" +
                "id=" + getId() +
                ", entityTypeId='" + getEntityTypeId() + "'" +
                ", entityObjectId=" + getEntityObjectId() +
                ", assignedRole='" + getAssignedRole() + "'" +
                "}";
    }
}
