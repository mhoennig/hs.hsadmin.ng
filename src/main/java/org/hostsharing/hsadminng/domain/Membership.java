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
    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @NotNull
    @Column(name = "member_from", nullable = false)
    private LocalDate memberFrom;

    @Column(name = "member_until")
    private LocalDate memberUntil;

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

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public Membership documentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
        return this;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDate getMemberFrom() {
        return memberFrom;
    }

    public Membership memberFrom(LocalDate memberFrom) {
        this.memberFrom = memberFrom;
        return this;
    }

    public void setMemberFrom(LocalDate memberFrom) {
        this.memberFrom = memberFrom;
    }

    public LocalDate getMemberUntil() {
        return memberUntil;
    }

    public Membership memberUntil(LocalDate memberUntil) {
        this.memberUntil = memberUntil;
        return this;
    }

    public void setMemberUntil(LocalDate memberUntil) {
        this.memberUntil = memberUntil;
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
            ", documentDate='" + getDocumentDate() + "'" +
            ", memberFrom='" + getMemberFrom() + "'" +
            ", memberUntil='" + getMemberUntil() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
