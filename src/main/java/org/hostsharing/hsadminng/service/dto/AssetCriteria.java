package org.hostsharing.hsadminng.service.dto;

import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;
import io.github.jhipster.service.filter.BigDecimalFilter;
import io.github.jhipster.service.filter.LocalDateFilter;

/**
 * Criteria class for the Asset entity. This class is used in AssetResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /assets?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class AssetCriteria implements Serializable {
    /**
     * Class for filtering AssetAction
     */
    public static class AssetActionFilter extends Filter<AssetAction> {
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LocalDateFilter documentDate;

    private LocalDateFilter valueDate;

    private AssetActionFilter action;

    private BigDecimalFilter amount;

    private StringFilter remark;

    private LongFilter membershipId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public LocalDateFilter getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDateFilter documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDateFilter getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDateFilter valueDate) {
        this.valueDate = valueDate;
    }

    public AssetActionFilter getAction() {
        return action;
    }

    public void setAction(AssetActionFilter action) {
        this.action = action;
    }

    public BigDecimalFilter getAmount() {
        return amount;
    }

    public void setAmount(BigDecimalFilter amount) {
        this.amount = amount;
    }

    public StringFilter getRemark() {
        return remark;
    }

    public void setRemark(StringFilter remark) {
        this.remark = remark;
    }

    public LongFilter getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(LongFilter membershipId) {
        this.membershipId = membershipId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AssetCriteria that = (AssetCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(documentDate, that.documentDate) &&
            Objects.equals(valueDate, that.valueDate) &&
            Objects.equals(action, that.action) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(remark, that.remark) &&
            Objects.equals(membershipId, that.membershipId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        documentDate,
        valueDate,
        action,
        amount,
        remark,
        membershipId
        );
    }

    @Override
    public String toString() {
        return "AssetCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (documentDate != null ? "documentDate=" + documentDate + ", " : "") +
                (valueDate != null ? "valueDate=" + valueDate + ", " : "") +
                (action != null ? "action=" + action + ", " : "") +
                (amount != null ? "amount=" + amount + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (membershipId != null ? "membershipId=" + membershipId + ", " : "") +
            "}";
    }

}
