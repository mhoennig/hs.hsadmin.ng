package org.hostsharing.hsadminng.service.dto;
import java.time.LocalDate;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Membership entity.
 */
public class MembershipDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate documentDate;

    @NotNull
    private LocalDate memberFrom;

    private LocalDate memberUntil;

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

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDate getMemberFrom() {
        return memberFrom;
    }

    public void setMemberFrom(LocalDate memberFrom) {
        this.memberFrom = memberFrom;
    }

    public LocalDate getMemberUntil() {
        return memberUntil;
    }

    public void setMemberUntil(LocalDate memberUntil) {
        this.memberUntil = memberUntil;
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
            ", documentDate='" + getDocumentDate() + "'" +
            ", memberFrom='" + getMemberFrom() + "'" +
            ", memberUntil='" + getMemberUntil() + "'" +
            ", remark='" + getRemark() + "'" +
            ", customer=" + getCustomerId() +
            ", customer='" + getCustomerPrefix() + "'" +
            "}";
    }
}
