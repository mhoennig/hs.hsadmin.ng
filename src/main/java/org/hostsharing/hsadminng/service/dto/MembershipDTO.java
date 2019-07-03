// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.hostsharing.hsadminng.service.accessfilter.Role.*;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A DTO for the Membership entity.
 */
@EntityTypeId(Membership.ENTITY_TYPE_ID)
public class MembershipDTO implements AccessMappings, FluentBuilder<MembershipDTO> {

    @SelfId(resolver = MembershipService.class)
    @AccessFor(read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private Long id;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate admissionDocumentDate;

    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate cancellationDocumentDate;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate memberFromDate;

    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate memberUntilDate;

    @Size(max = 160)
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private String remark;

    @ParentId(resolver = CustomerService.class)
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private Long customerId;

    @AccessFor(update = Ignored.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String customerPrefix;

    @AccessFor(update = Ignored.class, read = CustomerFinancialContact.class)
    private String customerDisplayLabel;

    @AccessFor(update = Ignored.class, read = CustomerFinancialContact.class)
    private String displayLabel;

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

    public String getCustomerDisplayLabel() {
        return customerDisplayLabel;
    }

    public void setCustomerDisplayLabel(final String customerDisplayLabel) {
        this.customerDisplayLabel = customerDisplayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
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
                ", customerPrefix='" + getCustomerPrefix() + "'" +
                ", customerDisplayLabel='" + getCustomerDisplayLabel() + "'" +
                ", displayLabel='" + getDisplayLabel() + "'" +
                "}";
    }

    @JsonComponent
    public static class JsonSerializer extends JsonSerializerWithAccessFilter<MembershipDTO> {

        public JsonSerializer(
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }

    @JsonComponent
    public static class JsonDeserializer extends JsonDeserializerWithAccessFilter<MembershipDTO> {

        public JsonDeserializer(
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }
}
