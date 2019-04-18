package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A SepaMandate.
 */
@Entity
@Table(name = "sepa_mandate")
public class SepaMandate implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Size(max = 40)
    @Column(name = "reference", length = 40, nullable = false, unique = true)
    private String reference;

    @Size(max = 34)
    @Column(name = "iban", length = 34)
    private String iban;

    @Size(max = 11)
    @Column(name = "bic", length = 11)
    private String bic;

    @NotNull
    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @NotNull
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "last_used")
    private LocalDate lastUsed;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Size(max = 160)
    @Column(name = "remark", length = 160)
    private String remark;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties("sepamandates")
    private Customer customer;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public SepaMandate reference(String reference) {
        this.reference = reference;
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIban() {
        return iban;
    }

    public SepaMandate iban(String iban) {
        this.iban = iban;
        return this;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public SepaMandate bic(String bic) {
        this.bic = bic;
        return this;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public SepaMandate documentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
        return this;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public SepaMandate validFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public SepaMandate validUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDate getLastUsed() {
        return lastUsed;
    }

    public SepaMandate lastUsed(LocalDate lastUsed) {
        this.lastUsed = lastUsed;
        return this;
    }

    public void setLastUsed(LocalDate lastUsed) {
        this.lastUsed = lastUsed;
    }

    public LocalDate getCancellationDate() {
        return cancellationDate;
    }

    public SepaMandate cancellationDate(LocalDate cancellationDate) {
        this.cancellationDate = cancellationDate;
        return this;
    }

    public void setCancellationDate(LocalDate cancellationDate) {
        this.cancellationDate = cancellationDate;
    }

    public String getRemark() {
        return remark;
    }

    public SepaMandate remark(String remark) {
        this.remark = remark;
        return this;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Customer getCustomer() {
        return customer;
    }

    public SepaMandate customer(Customer customer) {
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
        SepaMandate sepaMandate = (SepaMandate) o;
        if (sepaMandate.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), sepaMandate.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "SepaMandate{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", iban='" + getIban() + "'" +
            ", bic='" + getBic() + "'" +
            ", documentDate='" + getDocumentDate() + "'" +
            ", validFrom='" + getValidFrom() + "'" +
            ", validUntil='" + getValidUntil() + "'" +
            ", lastUsed='" + getLastUsed() + "'" +
            ", cancellationDate='" + getCancellationDate() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
