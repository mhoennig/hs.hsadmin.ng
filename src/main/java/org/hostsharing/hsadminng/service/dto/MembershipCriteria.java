package org.hostsharing.hsadminng.service.dto;

import java.io.Serializable;
import java.util.Objects;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;
import io.github.jhipster.service.filter.LocalDateFilter;

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

    private LocalDateFilter documentDate;

    private LocalDateFilter memberFrom;

    private LocalDateFilter memberUntil;

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

    public LocalDateFilter getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDateFilter documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDateFilter getMemberFrom() {
        return memberFrom;
    }

    public void setMemberFrom(LocalDateFilter memberFrom) {
        this.memberFrom = memberFrom;
    }

    public LocalDateFilter getMemberUntil() {
        return memberUntil;
    }

    public void setMemberUntil(LocalDateFilter memberUntil) {
        this.memberUntil = memberUntil;
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
        return
            Objects.equals(id, that.id) &&
            Objects.equals(documentDate, that.documentDate) &&
            Objects.equals(memberFrom, that.memberFrom) &&
            Objects.equals(memberUntil, that.memberUntil) &&
            Objects.equals(remark, that.remark) &&
            Objects.equals(shareId, that.shareId) &&
            Objects.equals(assetId, that.assetId) &&
            Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        documentDate,
        memberFrom,
        memberUntil,
        remark,
        shareId,
        assetId,
        customerId
        );
    }

    @Override
    public String toString() {
        return "MembershipCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (documentDate != null ? "documentDate=" + documentDate + ", " : "") +
                (memberFrom != null ? "memberFrom=" + memberFrom + ", " : "") +
                (memberUntil != null ? "memberUntil=" + memberUntil + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (shareId != null ? "shareId=" + shareId + ", " : "") +
                (assetId != null ? "assetId=" + assetId + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
            "}";
    }

}
