package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    @Column(name = "jhi_number", nullable = false, unique = true)
    private Integer number;

    @NotNull
    @Pattern(regexp = "[a-z][a-z0-9]+")
    @Column(name = "prefix", nullable = false, unique = true)
    private String prefix;

    @OneToMany(mappedBy = "customer")
    private Set<Membership> memberships = new HashSet<>();
    @OneToMany(mappedBy = "customer")
    private Set<CustomerContact> roles = new HashSet<>();
    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public Customer number(Integer number) {
        this.number = number;
        return this;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getPrefix() {
        return prefix;
    }

    public Customer prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Set<Membership> getMemberships() {
        return memberships;
    }

    public Customer memberships(Set<Membership> memberships) {
        this.memberships = memberships;
        return this;
    }

    public Customer addMembership(Membership membership) {
        this.memberships.add(membership);
        membership.setCustomer(this);
        return this;
    }

    public Customer removeMembership(Membership membership) {
        this.memberships.remove(membership);
        membership.setCustomer(null);
        return this;
    }

    public void setMemberships(Set<Membership> memberships) {
        this.memberships = memberships;
    }

    public Set<CustomerContact> getRoles() {
        return roles;
    }

    public Customer roles(Set<CustomerContact> customerContacts) {
        this.roles = customerContacts;
        return this;
    }

    public Customer addRole(CustomerContact customerContact) {
        this.roles.add(customerContact);
        customerContact.setCustomer(this);
        return this;
    }

    public Customer removeRole(CustomerContact customerContact) {
        this.roles.remove(customerContact);
        customerContact.setCustomer(null);
        return this;
    }

    public void setRoles(Set<CustomerContact> customerContacts) {
        this.roles = customerContacts;
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
        Customer customer = (Customer) o;
        if (customer.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), customer.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Customer{" +
            "id=" + getId() +
            ", number=" + getNumber() +
            ", prefix='" + getPrefix() + "'" +
            "}";
    }
}
