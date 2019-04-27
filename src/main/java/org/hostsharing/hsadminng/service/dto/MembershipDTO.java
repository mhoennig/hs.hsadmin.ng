package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the Membership entity.
 */
public class MembershipDTO extends FluentBuilder<MembershipDTO> implements Serializable, AccessMappings {

    @SelfId(resolver = MembershipService.class)
    @AccessFor(read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long id;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate admissionDocumentDate;

    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate cancellationDocumentDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate memberFromDate;

    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate memberUntilDate;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.SUPPORTER)
    private String remark;

    @ParentId(resolver = CustomerService.class)
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long customerId;

    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String customerPrefix;

    @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY, read = Role.FINANCIAL_CONTACT)
    private String displayLabel;

    @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY, read = Role.FINANCIAL_CONTACT)
    private String customerDisplayLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAdmissionDocumentDate() {
        return admissionDocumentDate;
    }

    public void setAdmissionDocumentDate(LocalDate admissionDocumentDate) {
        this.admissionDocumentDate = admissionDocumentDate;
    }

    public LocalDate getCancellationDocumentDate() {
        return cancellationDocumentDate;
    }

    public void setCancellationDocumentDate(LocalDate cancellationDocumentDate) {
        this.cancellationDocumentDate = cancellationDocumentDate;
    }

    public LocalDate getMemberFromDate() {
        return memberFromDate;
    }

    public void setMemberFromDate(LocalDate memberFromDate) {
        this.memberFromDate = memberFromDate;
    }

    public LocalDate getMemberUntilDate() {
        return memberUntilDate;
    }

    public void setMemberUntilDate(LocalDate memberUntilDate) {
        this.memberUntilDate = memberUntilDate;
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

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getCustomerDisplayLabel() {
        return customerDisplayLabel;
    }

    public void setCustomerDisplayLabel(final String customerDisplayLabel) {
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

        MembershipDTO membershipDTO = (MembershipDTO) o;
        if (membershipDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), membershipDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "MembershipDTO{" +
            "id=" + getId() +
            ", admissionDocumentDate='" + getAdmissionDocumentDate() + "'" +
            ", cancellationDocumentDate='" + getCancellationDocumentDate() + "'" +
            ", memberFromDate='" + getMemberFromDate() + "'" +
            ", memberUntilDate='" + getMemberUntilDate() + "'" +
            ", remark='" + getRemark() + "'" +
            ", customer=" + getCustomerId() +
            ", customer='" + getCustomerPrefix() + "'" +
            "}";
    }

    @JsonComponent
    public static class MembershipJsonSerializer extends JsonSerializerWithAccessFilter<MembershipDTO> {

        public MembershipJsonSerializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }

    @JsonComponent
    public static class MembershipJsonDeserializer extends JsonDeserializerWithAccessFilter<MembershipDTO> {

        public MembershipJsonDeserializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }
}
