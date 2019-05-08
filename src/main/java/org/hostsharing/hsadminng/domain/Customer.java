// Licensed under Apache-2.0
package org.hostsharing.hsadminng.domain;

import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENTITY_TYPE_ID = "customer.Customer";

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private CustomerKind kind;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 80)
    @Column(name = "birth_place", length = 80)
    private String birthPlace;

    @Size(max = 80)
    @Column(name = "registration_court", length = 80)
    private String registrationCourt;

    @Size(max = 80)
    @Column(name = "registration_number", length = 80)
    private String registrationNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_region", nullable = false)
    private VatRegion vatRegion;

    @Size(max = 40)
    @Column(name = "vat_number", length = 40)
    private String vatNumber;

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

    public Customer id(long id) {
        this.id = id;
        return this;
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

    public CustomerKind getKind() {
        return kind;
    }

    public Customer kind(CustomerKind kind) {
        this.kind = kind;
        return this;
    }

    public void setKind(CustomerKind kind) {
        this.kind = kind;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Customer birthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public Customer birthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
        return this;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getRegistrationCourt() {
        return registrationCourt;
    }

    public Customer registrationCourt(String registrationCourt) {
        this.registrationCourt = registrationCourt;
        return this;
    }

    public void setRegistrationCourt(String registrationCourt) {
        this.registrationCourt = registrationCourt;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Customer registrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public VatRegion getVatRegion() {
        return vatRegion;
    }

    public Customer vatRegion(VatRegion vatRegion) {
        this.vatRegion = vatRegion;
        return this;
    }

    public void setVatRegion(VatRegion vatRegion) {
        this.vatRegion = vatRegion;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public Customer vatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
        return this;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
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

    public Customer removeMembership(Membership membership) {
        this.memberships.remove(membership);
        membership.setCustomer(null);
        return this;
    }

    public void setMemberships(Set<Membership> memberships) {
        this.memberships = memberships;
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

    public Customer removeSepamandate(SepaMandate sepaMandate) {
        this.sepamandates.remove(sepaMandate);
        sepaMandate.setCustomer(null);
        return this;
    }

    public void setSepamandates(Set<SepaMandate> sepaMandates) {
        this.sepamandates = sepaMandates;
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
                ", kind='" + getKind() + "'" +
                ", birthDate='" + getBirthDate() + "'" +
                ", birthPlace='" + getBirthPlace() + "'" +
                ", registrationCourt='" + getRegistrationCourt() + "'" +
                ", registrationNumber='" + getRegistrationNumber() + "'" +
                ", vatRegion='" + getVatRegion() + "'" +
                ", vatNumber='" + getVatNumber() + "'" +
                ", contractualSalutation='" + getContractualSalutation() + "'" +
                ", contractualAddress='" + getContractualAddress() + "'" +
                ", billingSalutation='" + getBillingSalutation() + "'" +
                ", billingAddress='" + getBillingAddress() + "'" +
                ", remark='" + getRemark() + "'" +
                "}";
    }
}
