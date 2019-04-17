package org.hostsharing.hsadminng.service.dto;
import java.time.LocalDate;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;

/**
 * A DTO for the Share entity.
 */
public class ShareDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate documentDate;

    @NotNull
    private LocalDate valueDate;

    @NotNull
    private ShareAction action;

    @NotNull
    private Integer quantity;

    @Size(max = 160)
    private String remark;


    private Long membershipId;

    private String membershipDocumentDate;

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

    public String getMembershipDocumentDate() {
        return membershipDocumentDate;
    }

    public void setMembershipDocumentDate(String membershipDocumentDate) {
        this.membershipDocumentDate = membershipDocumentDate;
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
            ", membership='" + getMembershipDocumentDate() + "'" +
            "}";
    }
}
