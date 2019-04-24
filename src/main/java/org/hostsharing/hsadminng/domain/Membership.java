package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Membership.
 */
@Entity
@Table(name = "membership")
public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "admission_document_date", nullable = false)
    private LocalDate admissionDocumentDate;

    @Column(name = "cancellation_document_date")
    private LocalDate cancellationDocumentDate;

    @NotNull
    @Column(name = "member_from_date", nullable = false)
    private LocalDate memberFromDate;

    @Column(name = "member_until_date")
    private LocalDate memberUntilDate;

    @Size(max = 160)
    @Column(name = "remark", length = 160)
    private String remark;

    @OneToMany(mappedBy = "membership")
    private Set<Share> shares = new HashSet<>();
    @OneToMany(mappedBy = "membership")
    private Set<Asset> assets = new HashSet<>();
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties("memberships")
    private Customer customer;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAdmissionDocumentDate() {
        return admissionDocumentDate;
    }

    public Membership admissionDocumentDate(LocalDate admissionDocumentDate) {
        this.admissionDocumentDate = admissionDocumentDate;
        return this;
    }

    public void setAdmissionDocumentDate(LocalDate admissionDocumentDate) {
        this.admissionDocumentDate = admissionDocumentDate;
    }

    public LocalDate getCancellationDocumentDate() {
        return cancellationDocumentDate;
    }

    public Membership cancellationDocumentDate(LocalDate cancellationDocumentDate) {
        this.cancellationDocumentDate = cancellationDocumentDate;
        return this;
    }

    public void setCancellationDocumentDate(LocalDate cancellationDocumentDate) {
        this.cancellationDocumentDate = cancellationDocumentDate;
    }

    public LocalDate getMemberFromDate() {
        return memberFromDate;
    }

    public Membership memberFromDate(LocalDate memberFromDate) {
        this.memberFromDate = memberFromDate;
        return this;
    }

    public void setMemberFromDate(LocalDate memberFromDate) {
        this.memberFromDate = memberFromDate;
    }

    public LocalDate getMemberUntilDate() {
        return memberUntilDate;
    }

    public Membership memberUntilDate(LocalDate memberUntilDate) {
        this.memberUntilDate = memberUntilDate;
        return this;
    }

    public void setMemberUntilDate(LocalDate memberUntilDate) {
        this.memberUntilDate = memberUntilDate;
    }

    public String getRemark() {
        return remark;
    }

    public Membership remark(String remark) {
        this.remark = remark;
        return this;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Set<Share> getShares() {
        return shares;
    }

    public Membership shares(Set<Share> shares) {
        this.shares = shares;
        return this;
    }

    public Membership addShare(Share share) {
        this.shares.add(share);
        share.setMembership(this);
        return this;
    }

    public Membership removeShare(Share share) {
        this.shares.remove(share);
        share.setMembership(null);
        return this;
    }

    public void setShares(Set<Share> shares) {
        this.shares = shares;
    }

    public Set<Asset> getAssets() {
        return assets;
    }

    public Membership assets(Set<Asset> assets) {
        this.assets = assets;
        return this;
    }

    public Membership addAsset(Asset asset) {
        this.assets.add(asset);
        asset.setMembership(this);
        return this;
    }

    public Membership removeAsset(Asset asset) {
        this.assets.remove(asset);
        asset.setMembership(null);
        return this;
    }

    public void setAssets(Set<Asset> assets) {
        this.assets = assets;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Membership customer(Customer customer) {
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
        Membership membership = (Membership) o;
        if (membership.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), membership.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Membership{" +
            "id=" + getId() +
            ", admissionDocumentDate='" + getAdmissionDocumentDate() + "'" +
            ", cancellationDocumentDate='" + getCancellationDocumentDate() + "'" +
            ", memberFromDate='" + getMemberFromDate() + "'" +
            ", memberUntilDate='" + getMemberUntilDate() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
