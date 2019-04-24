package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A Asset.
 */
@Entity
@Table(name = "asset")
public class Asset implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENTITY_NAME = "asset";
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @NotNull
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AssetAction action;

    @NotNull
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Size(max = 160)
    @Column(name = "remark", length = 160)
    private String remark;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties("assets")
    private Membership membership;

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

    public Asset documentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
        return this;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public Asset valueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
        return this;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public AssetAction getAction() {
        return action;
    }

    public Asset action(AssetAction action) {
        this.action = action;
        return this;
    }

    public void setAction(AssetAction action) {
        this.action = action;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Asset amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public Asset remark(String remark) {
        this.remark = remark;
        return this;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Membership getMembership() {
        return membership;
    }

    public Asset membership(Membership membership) {
        this.membership = membership;
        return this;
    }

    public void setMembership(Membership membership) {
        this.membership = membership;
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
        Asset asset = (Asset) o;
        if (asset.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), asset.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Asset{" +
            "id=" + getId() +
            ", documentDate='" + getDocumentDate() + "'" +
            ", valueDate='" + getValueDate() + "'" +
            ", action='" + getAction() + "'" +
            ", amount=" + getAmount() +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
