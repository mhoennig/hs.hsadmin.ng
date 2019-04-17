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

    private LocalDateFilter date;

    private AssetActionFilter action;

    private BigDecimalFilter amount;

    private StringFilter comment;

    private LongFilter membershipId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public LocalDateFilter getDate() {
        return date;
    }

    public void setDate(LocalDateFilter date) {
        this.date = date;
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

    public StringFilter getComment() {
        return comment;
    }

    public void setComment(StringFilter comment) {
        this.comment = comment;
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
            Objects.equals(date, that.date) &&
            Objects.equals(action, that.action) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(membershipId, that.membershipId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        date,
        action,
        amount,
        comment,
        membershipId
        );
    }

    @Override
    public String toString() {
        return "AssetCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (date != null ? "date=" + date + ", " : "") +
                (action != null ? "action=" + action + ", " : "") +
                (amount != null ? "amount=" + amount + ", " : "") +
                (comment != null ? "comment=" + comment + ", " : "") +
                (membershipId != null ? "membershipId=" + membershipId + ", " : "") +
            "}";
    }

}
