package org.hostsharing.hsadminng.service.dto;

import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;
import io.github.jhipster.service.filter.LocalDateFilter;

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

    private LocalDateFilter date;

    private ShareActionFilter action;

    private IntegerFilter quantity;

    private StringFilter comment;

    private LongFilter memberId;

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

    public StringFilter getComment() {
        return comment;
    }

    public void setComment(StringFilter comment) {
        this.comment = comment;
    }

    public LongFilter getMemberId() {
        return memberId;
    }

    public void setMemberId(LongFilter memberId) {
        this.memberId = memberId;
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
        return
            Objects.equals(id, that.id) &&
            Objects.equals(date, that.date) &&
            Objects.equals(action, that.action) &&
            Objects.equals(quantity, that.quantity) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        date,
        action,
        quantity,
        comment,
        memberId
        );
    }

    @Override
    public String toString() {
        return "ShareCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (date != null ? "date=" + date + ", " : "") +
                (action != null ? "action=" + action + ", " : "") +
                (quantity != null ? "quantity=" + quantity + ", " : "") +
                (comment != null ? "comment=" + comment + ", " : "") +
                (memberId != null ? "memberId=" + memberId + ", " : "") +
            "}";
    }

}
