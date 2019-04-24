package org.hostsharing.hsadminng.service.dto;
import java.time.LocalDate;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A DTO for the Membership entity.
 */
public class MembershipDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate admissionDocumentDate;

    private LocalDate cancellationDocumentDate;

    @NotNull
    private LocalDate memberFromDate;

    private LocalDate memberUntilDate;

    @Size(max = 160)
    private String remark;


    private Long customerId;

    private String customerPrefix;

    public MembershipDTO with(
        Consumer<MembershipDTO> builderFunction) {
        builderFunction.accept(this);
        return this;
    }

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
}
