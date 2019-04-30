// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.ShareAction;

import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LocalDateFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the Share entity. This class is used in ShareResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /shares?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class ShareCriteria implements Serializable {

    /**
     * Class for filtering ShareAction
     */
    public static class ShareActionFilter extends Filter<ShareAction> {
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LocalDateFilter documentDate;

    private LocalDateFilter valueDate;

    private ShareActionFilter action;

    private IntegerFilter quantity;

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

    public ShareActionFilter getAction() {
        return action;
    }

    public void setAction(ShareActionFilter action) {
        this.action = action;
    }

    public IntegerFilter getQuantity() {
        return quantity;
    }

    public void setQuantity(IntegerFilter quantity) {
        this.quantity = quantity;
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
        final ShareCriteria that = (ShareCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(documentDate, that.documentDate) &&
                Objects.equals(valueDate, that.valueDate) &&
                Objects.equals(action, that.action) &&
                Objects.equals(quantity, that.quantity) &&
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
                quantity,
                remark,
                membershipId);
    }

    @Override
    public String toString() {
        return "ShareCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (documentDate != null ? "documentDate=" + documentDate + ", " : "") +
                (valueDate != null ? "valueDate=" + valueDate + ", " : "") +
                (action != null ? "action=" + action + ", " : "") +
                (quantity != null ? "quantity=" + quantity + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (membershipId != null ? "membershipId=" + membershipId + ", " : "") +
                "}";
    }

}
