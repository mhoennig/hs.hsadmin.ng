package org.hostsharing.hsadminng.service.dto;

import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.DoubleFilter;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.FloatFilter;
import io.github.jhipster.service.filter.IntegerFilter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;
import io.github.jhipster.service.filter.LocalDateFilter;

/**
 * Criteria class for the Customer entity. This class is used in CustomerResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * <code> /customers?id.greaterThan=5&amp;attr1.contains=something&amp;attr2.specified=false</code>
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
public class CustomerCriteria implements Serializable {
    /**
     * Class for filtering CustomerKind
     */
    public static class CustomerKindFilter extends Filter<CustomerKind> {
    }
    /**
     * Class for filtering VatRegion
     */
    public static class VatRegionFilter extends Filter<VatRegion> {
    }

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private IntegerFilter reference;

    private StringFilter prefix;

    private StringFilter name;

    private CustomerKindFilter kind;

    private LocalDateFilter birthDate;

    private StringFilter birthPlace;

    private StringFilter registrationCourt;

    private StringFilter registrationNumber;

    private VatRegionFilter vatRegion;

    private StringFilter vatNumber;

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

    public CustomerKindFilter getKind() {
        return kind;
    }

    public void setKind(CustomerKindFilter kind) {
        this.kind = kind;
    }

    public LocalDateFilter getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDateFilter birthDate) {
        this.birthDate = birthDate;
    }

    public StringFilter getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(StringFilter birthPlace) {
        this.birthPlace = birthPlace;
    }

    public StringFilter getRegistrationCourt() {
        return registrationCourt;
    }

    public void setRegistrationCourt(StringFilter registrationCourt) {
        this.registrationCourt = registrationCourt;
    }

    public StringFilter getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(StringFilter registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public VatRegionFilter getVatRegion() {
        return vatRegion;
    }

    public void setVatRegion(VatRegionFilter vatRegion) {
        this.vatRegion = vatRegion;
    }

    public StringFilter getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(StringFilter vatNumber) {
        this.vatNumber = vatNumber;
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
            Objects.equals(kind, that.kind) &&
            Objects.equals(birthDate, that.birthDate) &&
            Objects.equals(birthPlace, that.birthPlace) &&
            Objects.equals(registrationCourt, that.registrationCourt) &&
            Objects.equals(registrationNumber, that.registrationNumber) &&
            Objects.equals(vatRegion, that.vatRegion) &&
            Objects.equals(vatNumber, that.vatNumber) &&
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
        kind,
        birthDate,
        birthPlace,
        registrationCourt,
        registrationNumber,
        vatRegion,
        vatNumber,
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
                (kind != null ? "kind=" + kind + ", " : "") +
                (birthDate != null ? "birthDate=" + birthDate + ", " : "") +
                (birthPlace != null ? "birthPlace=" + birthPlace + ", " : "") +
                (registrationCourt != null ? "registrationCourt=" + registrationCourt + ", " : "") +
                (registrationNumber != null ? "registrationNumber=" + registrationNumber + ", " : "") +
                (vatRegion != null ? "vatRegion=" + vatRegion + ", " : "") +
                (vatNumber != null ? "vatNumber=" + vatNumber + ", " : "") +
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
