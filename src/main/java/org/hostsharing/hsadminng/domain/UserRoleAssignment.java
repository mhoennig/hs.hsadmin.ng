// Licensed under Apache-2.0
package org.hostsharing.hsadminng.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeNode;
import org.hostsharing.hsadminng.repository.UserRepository;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.service.accessfilter.Role.Supporter;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.lang.reflect.Field;
import java.util.Objects;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.of;

/**
 * A UserRoleAssignment.
 */
@Entity
@Table(name = "user_role_assignment")
@EntityTypeId(UserRoleAssignment.ENTITY_TYPE_ID)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class UserRoleAssignment implements AccessMappings {

    private static final long serialVersionUID = 1L;

    private static final String USER_FIELD_NAME = "user";

    public static final String ENTITY_TYPE_ID = "rights.UserRoleAssignment";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @SelfId(resolver = UserRoleAssignmentService.class)
    @AccessFor(read = Supporter.class)
    private Long id;

    @NotNull
    @Size(max = 32)
    @Column(name = "entity_type_id", length = 32, nullable = false)
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private String entityTypeId;

    @NotNull
    @Column(name = "entity_object_id", nullable = false)
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private Long entityObjectId;

    @NotNull
    @Column(name = "assigned_role", nullable = false)
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private String assignedRole;

    @ManyToOne
    @JsonIgnoreProperties("requireds")
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove

    public Long getId() {
        return id;
    }

    public UserRoleAssignment id(final long id) {
        this.id = id;
        return this;
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
        return assignedRole != null ? Role.of(assignedRole) : null;
    }

    public UserRoleAssignment assignedRole(Role assignedRole) {
        this.assignedRole = of(assignedRole, Role::name);
        return this;
    }

    public void setAssignedRole(Role assignedRole) {
        this.assignedRole = of(assignedRole, Role::name);
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
                ", entityTypeId='" + entityTypeId + "'" +
                ", entityObjectId=" + entityObjectId +
                ", assignedRole='" + assignedRole + "'" +
                "}";
    }

    @JsonComponent
    public static class UserRoleAssignmentJsonSerializer extends JsonSerializerWithAccessFilter<UserRoleAssignment> {

        public UserRoleAssignmentJsonSerializer(
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }

        @Override
        protected JSonFieldWriter<UserRoleAssignment> jsonFieldWriter(final Field field) {
            if (USER_FIELD_NAME.equals(field.getName())) {
                return (final UserRoleAssignment dto, final JsonGenerator jsonGenerator) -> jsonGenerator
                        .writeNumberField(USER_FIELD_NAME, dto.getUser().getId());
            }
            return super.jsonFieldWriter(field);
        }
    }

    @JsonComponent
    public static class UserRoleAssignmentJsonDeserializer extends JsonDeserializerWithAccessFilter<UserRoleAssignment> {

        private final UserRepository userRepository;

        public UserRoleAssignmentJsonDeserializer(
                final UserRepository userRepository,
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
            this.userRepository = userRepository;
        }

        @Override
        protected JSonFieldReader<UserRoleAssignment> jsonFieldReader(final TreeNode treeNode, final Field field) {
            if ("user".equals(field.getName())) {
                return (final UserRoleAssignment target) -> target
                        .setUser(userRepository.getOne(getSubNode(treeNode, "id").asLong()));
            }

            return super.jsonFieldReader(treeNode, field);
        }
    }
}
