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

/**
 * Criteria class for the Customer entity. This class is used in CustomerResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /customers?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class CustomerCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private IntegerFilter reference;

    private StringFilter prefix;

    private StringFilter name;

    private StringFilter contractualSalutation;

    private StringFilter contractualAddress;

    private StringFilter billingSalutation;

    private StringFilter billingAddress;

    private StringFilter remark;

    private LongFilter membershipId;

    private LongFilter sepamandateId;

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public IntegerFilter getReference() {
        return reference;
    }

    public void setReference(IntegerFilter reference) {
        this.reference = reference;
    }

    public StringFilter getPrefix() {
        return prefix;
    }

    public void setPrefix(StringFilter prefix) {
        this.prefix = prefix;
    }

    public StringFilter getName() {
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getContractualSalutation() {
        return contractualSalutation;
    }

    public void setContractualSalutation(StringFilter contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
    }

    public StringFilter getContractualAddress() {
        return contractualAddress;
    }

    public void setContractualAddress(StringFilter contractualAddress) {
        this.contractualAddress = contractualAddress;
    }

    public StringFilter getBillingSalutation() {
        return billingSalutation;
    }

    public void setBillingSalutation(StringFilter billingSalutation) {
        this.billingSalutation = billingSalutation;
    }

    public StringFilter getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(StringFilter billingAddress) {
        this.billingAddress = billingAddress;
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

    public LongFilter getSepamandateId() {
        return sepamandateId;
    }

    public void setSepamandateId(LongFilter sepamandateId) {
        this.sepamandateId = sepamandateId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CustomerCriteria that = (CustomerCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(reference, that.reference) &&
            Objects.equals(prefix, that.prefix) &&
            Objects.equals(name, that.name) &&
            Objects.equals(contractualSalutation, that.contractualSalutation) &&
            Objects.equals(contractualAddress, that.contractualAddress) &&
            Objects.equals(billingSalutation, that.billingSalutation) &&
            Objects.equals(billingAddress, that.billingAddress) &&
            Objects.equals(remark, that.remark) &&
            Objects.equals(membershipId, that.membershipId) &&
            Objects.equals(sepamandateId, that.sepamandateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
        id,
        reference,
        prefix,
        name,
        contractualSalutation,
        contractualAddress,
        billingSalutation,
        billingAddress,
        remark,
        membershipId,
        sepamandateId
        );
    }

    @Override
    public String toString() {
        return "CustomerCriteria{" +
                (id != null ? "id=" + id + ", " : "") +
                (reference != null ? "reference=" + reference + ", " : "") +
                (prefix != null ? "prefix=" + prefix + ", " : "") +
                (name != null ? "name=" + name + ", " : "") +
                (contractualSalutation != null ? "contractualSalutation=" + contractualSalutation + ", " : "") +
                (contractualAddress != null ? "contractualAddress=" + contractualAddress + ", " : "") +
                (billingSalutation != null ? "billingSalutation=" + billingSalutation + ", " : "") +
                (billingAddress != null ? "billingAddress=" + billingAddress + ", " : "") +
                (remark != null ? "remark=" + remark + ", " : "") +
                (membershipId != null ? "membershipId=" + membershipId + ", " : "") +
                (sepamandateId != null ? "sepamandateId=" + sepamandateId + ", " : "") +
            "}";
    }

}
