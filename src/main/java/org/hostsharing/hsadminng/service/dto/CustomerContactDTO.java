package org.hostsharing.hsadminng.service.dto;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.CustomerContactRole;

/**
 * A DTO for the CustomerContact entity.
 */
public class CustomerContactDTO implements Serializable {

    private Long id;

    @NotNull
    private CustomerContactRole role;


    private Long contactId;

    private String contactEmail;

    private Long customerId;

    private String customerPrefix;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerContactRole getRole() {
        return role;
    }

    public void setRole(CustomerContactRole role) {
        this.role = role;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPrefix() {
        return customerPrefix;
    }

    public void setCustomerPrefix(String customerPrefix) {
        this.customerPrefix = customerPrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomerContactDTO customerContactDTO = (CustomerContactDTO) o;
        if (customerContactDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), customerContactDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "CustomerContactDTO{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", contact=" + getContactId() +
            ", contact='" + getContactEmail() + "'" +
            ", customer=" + getCustomerId() +
            ", customer='" + getCustomerPrefix() + "'" +
            "}";
    }
}
