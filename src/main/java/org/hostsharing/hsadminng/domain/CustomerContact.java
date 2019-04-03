package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.util.Objects;

import org.hostsharing.hsadminng.domain.enumeration.CustomerContactRole;

/**
 * A CustomerContact.
 */
@Entity
@Table(name = "customer_contact")
public class CustomerContact implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "jhi_role", nullable = false)
    private CustomerContactRole role;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties("roles")
    private Contact contact;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties("roles")
    private Customer customer;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerContactRole getRole() {
        return role;
    }

    public CustomerContact role(CustomerContactRole role) {
        this.role = role;
        return this;
    }

    public void setRole(CustomerContactRole role) {
        this.role = role;
    }

    public Contact getContact() {
        return contact;
    }

    public CustomerContact contact(Contact contact) {
        this.contact = contact;
        return this;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Customer getCustomer() {
        return customer;
    }

    public CustomerContact customer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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
        CustomerContact customerContact = (CustomerContact) o;
        if (customerContact.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), customerContact.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "CustomerContact{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            "}";
    }
}
