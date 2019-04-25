package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.accessfilter.AccessFor;
import org.hostsharing.hsadminng.service.accessfilter.ParentId;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.SelfId;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the Share entity.
 */
public class ShareDTO implements Serializable {

    @SelfId(resolver = ShareService.class)
    @AccessFor(read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long id;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate documentDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private LocalDate valueDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private ShareAction action;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Integer quantity;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, read = Role.SUPPORTER)
    private String remark;

    @ParentId(resolver = MembershipService.class)
    @AccessFor(init = Role.ADMIN, read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private Long membershipId;

    @AccessFor(read = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT})
    private String membershipDisplayReference;

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

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public ShareAction getAction() {
        return action;
    }

    public void setAction(ShareAction action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }

    public String getMembershipDisplayReference() {
        return membershipDisplayReference;
    }

    public void setMembershipDisplayReference(String membershipDisplayReference) {
        this.membershipDisplayReference = membershipDisplayReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShareDTO shareDTO = (ShareDTO) o;
        if (shareDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), shareDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ShareDTO{" +
            "id=" + getId() +
            ", documentDate='" + getDocumentDate() + "'" +
            ", valueDate='" + getValueDate() + "'" +
            ", action='" + getAction() + "'" +
            ", quantity=" + getQuantity() +
            ", remark='" + getRemark() + "'" +
            ", membership=" + getMembershipId() +
            ", membership='" + getMembershipDisplayReference() + "'" +
            "}";
    }
}
