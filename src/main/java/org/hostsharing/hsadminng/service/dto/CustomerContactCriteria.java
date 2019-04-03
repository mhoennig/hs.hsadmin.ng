package org.hostsharing.hsadminng.service.dto;

import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.CustomerContactRole;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

/**
 * Criteria class for the CustomerContact entity. This class is used in CustomerContactResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /customer-contacts?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class CustomerContactCriteria implements Serializable {
    /**
     * Class for filtering CustomerContactRole
     */
    public static class CustomerContactRoleFilter extends Filter<CustomerContactRole> {
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private CustomerContactRoleFilter role;

    private LongFilter contactId;

    private LongFilter customerId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public CustomerContactRoleFilter getRole() {
        return role;
    }

    public void setRole(CustomerContactRoleFilter role) {
        this.role = role;
    }

    public LongFilter getContactId() {
        return contactId;
    }

    public void setContactId(LongFilter contactId) {
        this.contactId = contactId;
    }

    public LongFilter getCustomerId() {
        return customerId;
    }

    public void setCustomerId(LongFilter customerId) {
        this.customerId = customerId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CustomerContactCriteria that = (CustomerContactCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(role, that.role) &&
            Objects.equals(contactId, that.contactId) &&
            Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        role,
        contactId,
        customerId
        );
    }

    @Override
    public String toString() {
        return "CustomerContactCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (role != null ? "role=" + role + ", " : "") +
                (contactId != null ? "contactId=" + contactId + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
            "}";
    }

}
