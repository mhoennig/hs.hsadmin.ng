package org.hostsharing.hsadminng.domain;


import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    @NotNull
    @Size(max = 80)
    @Column(name = "name", length = 80, nullable = false)
    private String name;

    @NotNull
    @Size(max = 400)
    @Column(name = "contractual_address", length = 400, nullable = false)
    private String contractualAddress;

    @Size(max = 80)
    @Column(name = "contractual_salutation", length = 80)
    private String contractualSalutation;

    @Size(max = 400)
    @Column(name = "billing_address", length = 400)
    private String billingAddress;

    @Size(max = 80)
    @Column(name = "billing_salutation", length = 80)
    private String billingSalutation;

    @OneToMany(mappedBy = "customer")
    private Set<CustomerContact> roles = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<Membership> memberships = new HashSet<>();

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

    public String getName() {
        return name;
    }

    public Customer name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContractualAddress() {
        return contractualAddress;
    }

    public Customer contractualAddress(String contractualAddress) {
        this.contractualAddress = contractualAddress;
        return this;
    }

    public void setContractualAddress(String contractualAddress) {
        this.contractualAddress = contractualAddress;
    }

    public String getContractualSalutation() {
        return contractualSalutation;
    }

    public Customer contractualSalutation(String contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
        return this;
    }

    public void setContractualSalutation(String contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public Customer billingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
        return this;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingSalutation() {
        return billingSalutation;
    }

    public Customer billingSalutation(String billingSalutation) {
        this.billingSalutation = billingSalutation;
        return this;
    }

    public void setBillingSalutation(String billingSalutation) {
        this.billingSalutation = billingSalutation;
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
            ", name='" + getName() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            "}";
    }
}
