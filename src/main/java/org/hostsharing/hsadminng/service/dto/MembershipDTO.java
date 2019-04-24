package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.AccessFor;
import org.hostsharing.hsadminng.service.accessfilter.ParentId;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.SelfId;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A DTO for the Membership entity.
 */
public class MembershipDTO implements Serializable {

    @SelfId
    @AccessFor(read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long id;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate documentDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate memberFrom;

    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate memberUntil;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, read = Role.SUPPORTER)
    private String remark;

    @ParentId(CustomerDTO.class)
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long customerId;

    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
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
