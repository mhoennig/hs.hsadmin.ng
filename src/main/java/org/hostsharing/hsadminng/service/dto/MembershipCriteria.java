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

    private LocalDateFilter sinceDate;

    private LocalDateFilter untilDate;

    private LongFilter shareId;

    private LongFilter assetId;

    private LongFilter customerId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public LocalDateFilter getSinceDate() {
        return sinceDate;
    }

    public void setSinceDate(LocalDateFilter sinceDate) {
        this.sinceDate = sinceDate;
    }

    public LocalDateFilter getUntilDate() {
        return untilDate;
    }

    public void setUntilDate(LocalDateFilter untilDate) {
        this.untilDate = untilDate;
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
            Objects.equals(sinceDate, that.sinceDate) &&
            Objects.equals(untilDate, that.untilDate) &&
            Objects.equals(shareId, that.shareId) &&
            Objects.equals(assetId, that.assetId) &&
            Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        sinceDate,
        untilDate,
        shareId,
        assetId,
        customerId
        );
    }

    @Override
    public String toString() {
        return "MembershipCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (sinceDate != null ? "sinceDate=" + sinceDate + ", " : "") +
                (untilDate != null ? "untilDate=" + untilDate + ", " : "") +
                (shareId != null ? "shareId=" + shareId + ", " : "") +
                (assetId != null ? "assetId=" + assetId + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
            "}";
    }

}
