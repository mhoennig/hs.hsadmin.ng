// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.AssetService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.accessfilter.*;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A DTO for the Asset entity.
 */
public class AssetDTO implements Serializable, AccessMappings {

    @SelfId(resolver = AssetService.class)
    @AccessFor(read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private Long id;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private LocalDate documentDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private LocalDate valueDate;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private AssetAction action;

    @NotNull
    @AccessFor(init = Role.ADMIN, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private BigDecimal amount;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.SUPPORTER)
    private String remark;

    @ParentId(resolver = MembershipService.class)
    @AccessFor(init = Role.ADMIN, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
    private Long membershipId;

    @AccessFor(update = Role.IGNORED, read = { Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT })
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

    public AssetAction getAction() {
        return action;
    }

    public void setAction(AssetAction action) {
        this.action = action;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

        AssetDTO assetDTO = (AssetDTO) o;
        if (assetDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), assetDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "AssetDTO{" +
                "id=" + getId() +
                ", documentDate='" + getDocumentDate() + "'" +
                ", valueDate='" + getValueDate() + "'" +
                ", action='" + getAction() + "'" +
                ", amount=" + getAmount() +
                ", remark='" + getRemark() + "'" +
                ", membership=" + getMembershipId() +
                ", membershipDisplayLabel='" + getMembershipDisplayLabel() + "'" +
                "}";
    }

    @JsonComponent
    public static class JsonSerializer extends JsonSerializerWithAccessFilter<AssetDTO> {

        public JsonSerializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }

    @JsonComponent
    public static class JsonDeserializer extends JsonDeserializerWithAccessFilter<AssetDTO> {

        public JsonDeserializer(final ApplicationContext ctx) {
            super(ctx);
        }
    }
}
