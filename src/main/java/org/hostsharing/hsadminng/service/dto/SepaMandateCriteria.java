// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.LocalDateFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;

import java.io.Serializable;
import java.util.Objects;

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

    private LocalDateFilter grantingDocumentDate;

    private LocalDateFilter revokationDocumentDate;

    private LocalDateFilter validFromDate;

    private LocalDateFilter validUntilDate;

    private LocalDateFilter lastUsedDate;

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

    public LocalDateFilter getGrantingDocumentDate() {
        return grantingDocumentDate;
    }

    public void setGrantingDocumentDate(LocalDateFilter grantingDocumentDate) {
        this.grantingDocumentDate = grantingDocumentDate;
    }

    public LocalDateFilter getRevokationDocumentDate() {
        return revokationDocumentDate;
    }

    public void setRevokationDocumentDate(LocalDateFilter revokationDocumentDate) {
        this.revokationDocumentDate = revokationDocumentDate;
    }

    public LocalDateFilter getValidFromDate() {
        return validFromDate;
    }

    public void setValidFromDate(LocalDateFilter validFromDate) {
        this.validFromDate = validFromDate;
    }

    public LocalDateFilter getValidUntilDate() {
        return validUntilDate;
    }

    public void setValidUntilDate(LocalDateFilter validUntilDate) {
        this.validUntilDate = validUntilDate;
    }

    public LocalDateFilter getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(LocalDateFilter lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
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
        return Objects.equals(id, that.id) &&
                Objects.equals(reference, that.reference) &&
                Objects.equals(iban, that.iban) &&
                Objects.equals(bic, that.bic) &&
                Objects.equals(grantingDocumentDate, that.grantingDocumentDate) &&
                Objects.equals(revokationDocumentDate, that.revokationDocumentDate) &&
                Objects.equals(validFromDate, that.validFromDate) &&
                Objects.equals(validUntilDate, that.validUntilDate) &&
                Objects.equals(lastUsedDate, that.lastUsedDate) &&
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
                grantingDocumentDate,
                revokationDocumentDate,
                validFromDate,
                validUntilDate,
                lastUsedDate,
                remark,
                customerId);
    }

    @Override
    public String toString() {
        return "SepaMandateCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (reference != null ? "reference=" + reference + ", " : "") +
                (iban != null ? "iban=" + iban + ", " : "") +
                (bic != null ? "bic=" + bic + ", " : "") +
                (grantingDocumentDate != null ? "grantingDocumentDate=" + grantingDocumentDate + ", " : "") +
                (revokationDocumentDate != null ? "revokationDocumentDate=" + revokationDocumentDate + ", " : "") +
                (validFromDate != null ? "validFromDate=" + validFromDate + ", " : "") +
                (validUntilDate != null ? "validUntilDate=" + validUntilDate + ", " : "") +
                (lastUsedDate != null ? "lastUsedDate=" + lastUsedDate + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (customerId != null ? "customerId=" + customerId + ", " : "") +
                "}";
    }

}
