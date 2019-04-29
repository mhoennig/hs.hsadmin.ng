package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.SepaMandateService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the SepaMandate entity.
 */
public class SepaMandateDTO implements AccessMappings, FluentBuilder<SepaMandateDTO> {

    @SelfId(resolver = SepaMandateService.class)
    @AccessFor(read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long id;

    @NotNull
    @Size(max = 40)
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String reference;

    @Size(max = 34)
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String iban;

    @Size(max = 11)
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String bic;

    @NotNull
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate grantingDocumentDate;

    @AccessFor(init = Role.ADMIN, update = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate revokationDocumentDate;

    @NotNull
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate validFromDate;

    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, update = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate validUntilDate;

    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate lastUsedDate;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, update = Role.SUPPORTER, read = Role.SUPPORTER)
    private String remark;

    @ParentId(resolver = CustomerService.class)
    @AccessFor(init = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long customerId;

    @AccessFor(update = Role.IGNORED, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String customerDisplayLabel;

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

    public String getCustomerDisplayLabel() {
        return customerDisplayLabel;
    }

    public void setCustomerDisplayLabel(String customerDisplayLabel) {
        this.customerDisplayLabel = customerDisplayLabel;
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
            ", customerDisplayLabel='" + getCustomerDisplayLabel() + "'" +
            "}";
    }

    @JsonComponent
    public static class JsonSerializer extends JsonSerializerWithAccessFilter<SepaMandateDTO> {

        public JsonSerializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }

    @JsonComponent
    public static class JsonDeserializer extends JsonDeserializerWithAccessFilter<SepaMandateDTO> {

        public JsonDeserializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }
}
