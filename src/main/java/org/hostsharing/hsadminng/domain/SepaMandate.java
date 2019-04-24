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
    @Column(name = "granting_document_date", nullable = false)
    private LocalDate grantingDocumentDate;

    @Column(name = "revokation_document_date")
    private LocalDate revokationDocumentDate;

    @NotNull
    @Column(name = "valid_from_date", nullable = false)
    private LocalDate validFromDate;

    @Column(name = "valid_until_date")
    private LocalDate validUntilDate;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

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

    public LocalDate getGrantingDocumentDate() {
        return grantingDocumentDate;
    }

    public SepaMandate grantingDocumentDate(LocalDate grantingDocumentDate) {
        this.grantingDocumentDate = grantingDocumentDate;
        return this;
    }

    public void setGrantingDocumentDate(LocalDate grantingDocumentDate) {
        this.grantingDocumentDate = grantingDocumentDate;
    }

    public LocalDate getRevokationDocumentDate() {
        return revokationDocumentDate;
    }

    public SepaMandate revokationDocumentDate(LocalDate revokationDocumentDate) {
        this.revokationDocumentDate = revokationDocumentDate;
        return this;
    }

    public void setRevokationDocumentDate(LocalDate revokationDocumentDate) {
        this.revokationDocumentDate = revokationDocumentDate;
    }

    public LocalDate getValidFromDate() {
        return validFromDate;
    }

    public SepaMandate validFromDate(LocalDate validFromDate) {
        this.validFromDate = validFromDate;
        return this;
    }

    public void setValidFromDate(LocalDate validFromDate) {
        this.validFromDate = validFromDate;
    }

    public LocalDate getValidUntilDate() {
        return validUntilDate;
    }

    public SepaMandate validUntilDate(LocalDate validUntilDate) {
        this.validUntilDate = validUntilDate;
        return this;
    }

    public void setValidUntilDate(LocalDate validUntilDate) {
        this.validUntilDate = validUntilDate;
    }

    public LocalDate getLastUsedDate() {
        return lastUsedDate;
    }

    public SepaMandate lastUsedDate(LocalDate lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
        return this;
    }

    public void setLastUsedDate(LocalDate lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
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
            ", grantingDocumentDate='" + getGrantingDocumentDate() + "'" +
            ", revokationDocumentDate='" + getRevokationDocumentDate() + "'" +
            ", validFromDate='" + getValidFromDate() + "'" +
            ", validUntilDate='" + getValidUntilDate() + "'" +
            ", lastUsedDate='" + getLastUsedDate() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }
}
