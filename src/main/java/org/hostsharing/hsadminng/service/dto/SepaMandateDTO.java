package org.hostsharing.hsadminng.service.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the SepaMandate entity.
 */
public class SepaMandateDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 40)
    private String reference;

    @Size(max = 34)
    private String iban;

    @Size(max = 11)
    private String bic;

    @NotNull
    private LocalDate grantingDocumentDate;

    private LocalDate revokationDocumentDate;

    @NotNull
    private LocalDate validFromDate;

    private LocalDate validUntilDate;

    private LocalDate lastUsedDate;

    @Size(max = 160)
    private String remark;


    private Long customerId;

    private String customerPrefix;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public LocalDate getGrantingDocumentDate() {
        return grantingDocumentDate;
    }

    public void setGrantingDocumentDate(LocalDate grantingDocumentDate) {
        this.grantingDocumentDate = grantingDocumentDate;
    }

    public LocalDate getRevokationDocumentDate() {
        return revokationDocumentDate;
    }

    public void setRevokationDocumentDate(LocalDate revokationDocumentDate) {
        this.revokationDocumentDate = revokationDocumentDate;
    }

    public LocalDate getValidFromDate() {
        return validFromDate;
    }

    public void setValidFromDate(LocalDate validFromDate) {
        this.validFromDate = validFromDate;
    }

    public LocalDate getValidUntilDate() {
        return validUntilDate;
    }

    public void setValidUntilDate(LocalDate validUntilDate) {
        this.validUntilDate = validUntilDate;
    }

    public LocalDate getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(LocalDate lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPrefix() {
        return customerPrefix;
    }

    public void setCustomerPrefix(String customerPrefix) {
        this.customerPrefix = customerPrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SepaMandateDTO sepaMandateDTO = (SepaMandateDTO) o;
        if (sepaMandateDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), sepaMandateDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "SepaMandateDTO{" +
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
            ", customer=" + getCustomerId() +
            ", customer='" + getCustomerPrefix() + "'" +
            "}";
    }
}
