// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.AssetAction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.*;

/**
 * A DTO for the Asset entity.
 */
public class AssetDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate documentDate;

    @NotNull
    private LocalDate valueDate;

    @NotNull
    private AssetAction action;

    @NotNull
    private BigDecimal amount;

    @Size(max = 160)
    private String remark;

    private Long membershipId;

    private String membershipAdmissionDocumentDate;

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

    public String getMembershipAdmissionDocumentDate() {
        return membershipAdmissionDocumentDate;
    }

    public void setMembershipAdmissionDocumentDate(String membershipAdmissionDocumentDate) {
        this.membershipAdmissionDocumentDate = membershipAdmissionDocumentDate;
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
                ", membership='" + getMembershipAdmissionDocumentDate() + "'" +
                "}";
    }
}
