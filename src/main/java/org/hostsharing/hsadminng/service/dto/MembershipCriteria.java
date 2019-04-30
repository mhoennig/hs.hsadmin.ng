// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.LocalDateFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the Membership entity. This class is used in MembershipResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /memberships?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class MembershipCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LocalDateFilter admissionDocumentDate;

    private LocalDateFilter cancellationDocumentDate;

    private LocalDateFilter memberFromDate;

    private LocalDateFilter memberUntilDate;

    private StringFilter remark;

    private LongFilter shareId;

    private LongFilter assetId;

    private LongFilter customerId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public LocalDateFilter getAdmissionDocumentDate() {
        return admissionDocumentDate;
    }

    public void setAdmissionDocumentDate(LocalDateFilter admissionDocumentDate) {
        this.admissionDocumentDate = admissionDocumentDate;
    }

    public LocalDateFilter getCancellationDocumentDate() {
        return cancellationDocumentDate;
    }

    public void setCancellationDocumentDate(LocalDateFilter cancellationDocumentDate) {
        this.cancellationDocumentDate = cancellationDocumentDate;
    }

    public LocalDateFilter getMemberFromDate() {
        return memberFromDate;
    }

    public void setMemberFromDate(LocalDateFilter memberFromDate) {
        this.memberFromDate = memberFromDate;
    }

    public LocalDateFilter getMemberUntilDate() {
        return memberUntilDate;
    }

    public void setMemberUntilDate(LocalDateFilter memberUntilDate) {
        this.memberUntilDate = memberUntilDate;
    }

    public StringFilter getRemark() {
        return remark;
    }

    public void setRemark(StringFilter remark) {
        this.remark = remark;
    }

    public LongFilter getShareId() {
        return shareId;
    }

    public void setShareId(LongFilter shareId) {
        this.shareId = shareId;
    }

    public LongFilter getAssetId() {
        return assetId;
    }

    public void setAssetId(LongFilter assetId) {
        this.assetId = assetId;
    }

    public LongFilter getCustomerId() {
        return customerId;
    }

    public void setCustomerId(LongFilter customerId) {
        this.customerId = customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MembershipCriteria that = (MembershipCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(admissionDocumentDate, that.admissionDocumentDate) &&
                Objects.equals(cancellationDocumentDate, that.cancellationDocumentDate) &&
                Objects.equals(memberFromDate, that.memberFromDate) &&
                Objects.equals(memberUntilDate, that.memberUntilDate) &&
                Objects.equals(remark, that.remark) &&
                Objects.equals(shareId, that.shareId) &&
                Objects.equals(assetId, that.assetId) &&
                Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                admissionDocumentDate,
                cancellationDocumentDate,
                memberFromDate,
                memberUntilDate,
                remark,
                shareId,
                assetId,
                customerId);
    }

    @Override
    public String toString() {
        return "MembershipCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (admissionDocumentDate != null ? "admissionDocumentDate=" + admissionDocumentDate + ", " : "") +
                (cancellationDocumentDate != null ? "cancellationDocumentDate=" + cancellationDocumentDate + ", " : "") +
                (memberFromDate != null ? "memberFromDate=" + memberFromDate + ", " : "") +
                (memberUntilDate != null ? "memberUntilDate=" + memberUntilDate + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (shareId != null ? "shareId=" + shareId + ", " : "") +
                (assetId != null ? "assetId=" + assetId + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
                "}";
    }

}
