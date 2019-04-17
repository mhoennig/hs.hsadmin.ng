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
 * Criteria class for the SepaMandate entity. This class is used in SepaMandateResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /sepa-mandates?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class SepaMandateCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter reference;

    private StringFilter iban;

    private StringFilter bic;

    private LocalDateFilter documentDate;

    private LocalDateFilter validFrom;

    private LocalDateFilter validUntil;

    private LocalDateFilter lastUsed;

    private LocalDateFilter cancellationDate;

    private StringFilter remark;

    private LongFilter customerId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getReference() {
        return reference;
    }

    public void setReference(StringFilter reference) {
        this.reference = reference;
    }

    public StringFilter getIban() {
        return iban;
    }

    public void setIban(StringFilter iban) {
        this.iban = iban;
    }

    public StringFilter getBic() {
        return bic;
    }

    public void setBic(StringFilter bic) {
        this.bic = bic;
    }

    public LocalDateFilter getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDateFilter documentDate) {
        this.documentDate = documentDate;
    }

    public LocalDateFilter getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateFilter validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateFilter getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateFilter validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateFilter getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateFilter lastUsed) {
        this.lastUsed = lastUsed;
    }

    public LocalDateFilter getCancellationDate() {
        return cancellationDate;
    }

    public void setCancellationDate(LocalDateFilter cancellationDate) {
        this.cancellationDate = cancellationDate;
    }

    public StringFilter getRemark() {
        return remark;
    }

    public void setRemark(StringFilter remark) {
        this.remark = remark;
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
        final SepaMandateCriteria that = (SepaMandateCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(iban, that.iban) &&
            Objects.equals(bic, that.bic) &&
            Objects.equals(documentDate, that.documentDate) &&
            Objects.equals(validFrom, that.validFrom) &&
            Objects.equals(validUntil, that.validUntil) &&
            Objects.equals(lastUsed, that.lastUsed) &&
            Objects.equals(cancellationDate, that.cancellationDate) &&
            Objects.equals(remark, that.remark) &&
            Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        reference,
        iban,
        bic,
        documentDate,
        validFrom,
        validUntil,
        lastUsed,
        cancellationDate,
        remark,
        customerId
        );
    }

    @Override
    public String toString() {
        return "SepaMandateCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (reference != null ? "reference=" + reference + ", " : "") +
                (iban != null ? "iban=" + iban + ", " : "") +
                (bic != null ? "bic=" + bic + ", " : "") +
                (documentDate != null ? "documentDate=" + documentDate + ", " : "") +
                (validFrom != null ? "validFrom=" + validFrom + ", " : "") +
                (validUntil != null ? "validUntil=" + validUntil + ", " : "") +
                (lastUsed != null ? "lastUsed=" + lastUsed + ", " : "") +
                (cancellationDate != null ? "cancellationDate=" + cancellationDate + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
            "}";
    }

}
