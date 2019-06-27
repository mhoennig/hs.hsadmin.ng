// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.hostsharing.hsadminng.service.accessfilter.Role.*;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the Share entity.
 */
@EntityTypeId(Share.ENTITY_TYPE_ID)
public class ShareDTO implements Serializable, AccessMappings {

    @SelfId(resolver = ShareService.class)
    @AccessFor(read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private Long id;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate documentDate;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate valueDate;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private ShareAction action;

    @NotNull
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private Integer quantity;

    @Size(max = 160)
    @AccessFor(init = Admin.class, update = Admin.class, read = Supporter.class)
    private String remark;

    @ParentId(resolver = MembershipService.class)
    @AccessFor(init = Admin.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private Long membershipId;

    @AccessFor(update = Ignored.class, read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String membershipDisplayLabel;

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

    public String getMembershipDisplayLabel() {
        return membershipDisplayLabel;
    }

    public void setMembershipDisplayLabel(String membershipDisplayLabel) {
        this.membershipDisplayLabel = membershipDisplayLabel;
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
                ", membershipDisplayLabel='" + getMembershipDisplayLabel() + "'" +
                "}";
    }

    @JsonComponent
    public static class JsonSerializer extends JsonSerializerWithAccessFilter<ShareDTO> {

        public JsonSerializer(final ApplicationContext ctx, final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }

    @JsonComponent
    public static class JsonDeserializer extends JsonDeserializerWithAccessFilter<ShareDTO> {

        public JsonDeserializer(final ApplicationContext ctx, final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }
}
