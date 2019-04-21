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
    @Column(name = "reference", nullable = false, unique = true)
    private Integer reference;

    @NotNull
    @Size(max = 3)
    @Pattern(regexp = "[a-z][a-z0-9]+")
    @Column(name = "prefix", length = 3, nullable = false, unique = true)
    private String prefix;

    @NotNull
    @Size(max = 80)
    @Column(name = "name", length = 80, nullable = false)
    private String name;

    @Size(max = 80)
    @Column(name = "contractual_salutation", length = 80)
    private String contractualSalutation;

    @NotNull
    @Size(max = 400)
    @Column(name = "contractual_address", length = 400, nullable = false)
    private String contractualAddress;

    @Size(max = 80)
    @Column(name = "billing_salutation", length = 80)
    private String billingSalutation;

    @Size(max = 400)
    @Column(name = "billing_address", length = 400)
    private String billingAddress;

    @Size(max = 160)
    @Column(name = "remark", length = 160)
    private String remark;

    @OneToMany(mappedBy = "customer")
    private Set<Membership> memberships = new HashSet<>();
    @OneToMany(mappedBy = "customer")
    private Set<SepaMandate> sepamandates = new HashSet<>();
    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getReference() {
        return reference;
    }

    public Customer reference(Integer reference) {
        this.reference = reference;
        return this;
    }

    public void setReference(Integer reference) {
        this.reference = reference;
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

    public String getRemark() {
        return remark;
    }

    public Customer remark(String remark) {
        this.remark = remark;
        return this;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public Set<SepaMandate> getSepamandates() {
        return sepamandates;
    }

    public Customer sepamandates(Set<SepaMandate> sepaMandates) {
        this.sepamandates = sepaMandates;
        return this;
    }

    public Customer addSepamandate(SepaMandate sepaMandate) {
        this.sepamandates.add(sepaMandate);
        sepaMandate.setCustomer(this);
        return this;
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
            ", reference=" + getReference() +
            ", prefix='" + getPrefix() + "'" +
            ", name='" + getName() + "'" +
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
