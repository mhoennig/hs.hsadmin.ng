package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A Membership.
 */
@Entity
@Table(name = "membership")
public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENTITY_NAME = "membership";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "since_date", nullable = false)
    private LocalDate sinceDate;

    @Column(name = "until_date")
    private LocalDate untilDate;

    @OneToMany(mappedBy = "member")
    private Set<Share> shares = new HashSet<>();

    @OneToMany(mappedBy = "member")
    private Set<Asset> assets = new HashSet<>();

    @NotNull
    @ManyToOne
    @JsonIgnoreProperties("memberships")
    private Customer customer;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSinceDate() {
        return sinceDate;
    }

    public Membership sinceDate(LocalDate sinceDate) {
        this.sinceDate = sinceDate;
        return this;
    }

    public void setSinceDate(LocalDate sinceDate) {
        this.sinceDate = sinceDate;
    }

    public LocalDate getUntilDate() {
        return untilDate;
    }

    public Membership untilDate(LocalDate untilDate) {
        this.untilDate = untilDate;
        return this;
    }

    public void setUntilDate(LocalDate untilDate) {
        this.untilDate = untilDate;
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
        share.setMember(this);
        return this;
    }

    public Membership removeShare(Share share) {
        this.shares.remove(share);
        share.setMember(null);
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
        asset.setMember(this);
        return this;
    }

    public Membership removeAsset(Asset asset) {
        this.assets.remove(asset);
        asset.setMember(null);
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
            ", sinceDate='" + getSinceDate() + "'" +
            ", untilDate='" + getUntilDate() + "'" +
            "}";
    }
}
