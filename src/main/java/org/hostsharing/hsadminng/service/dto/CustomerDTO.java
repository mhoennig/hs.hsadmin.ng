// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.*;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Objects;

import static org.hostsharing.hsadminng.service.accessfilter.Role.*;

/**
 * A DTO for the Customer entity.
 */
@EntityTypeId(Customer.ENTITY_TYPE_ID)
public class CustomerDTO implements AccessMappings, FluentBuilder<CustomerDTO> {

    @SelfId(resolver = CustomerService.class)
    @AccessFor(read = AnyCustomerUser.class)
    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    @AccessFor(init = Admin.class, read = AnyCustomerUser.class)
    private Integer reference;

    @NotNull
    @Size(max = 3)
    @Pattern(regexp = "[a-z][a-z0-9]+")
    @AccessFor(init = Admin.class, read = AnyCustomerUser.class)
    private String prefix;

    @NotNull
    @Size(max = 80)
    @AccessFor(init = Admin.class, update = Admin.class, read = AnyCustomerUser.class)
    private String name;

    @NotNull
    @AccessFor(init = Admin.class, update = Admin.class, read = CustomerContractualContact.class)
    private CustomerKind kind;

    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private LocalDate birthDate;

    @Size(max = 80)
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String birthPlace;

    @Size(max = 80)
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String registrationCourt;

    @Size(max = 80)
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String registrationNumber;

    @NotNull
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private VatRegion vatRegion;

    @Size(max = 40)
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String vatNumber;

    @Size(max = 80)
    @AccessFor(init = Admin.class, update = CustomerContractualContact.class, read = CustomerContractualContact.class)
    private String contractualSalutation;

    @NotNull
    @Size(max = 400)
    @AccessFor(init = Admin.class, update = Admin.class, read = CustomerContractualContact.class)
    private String contractualAddress;

    @Size(max = 80)
    @AccessFor(
            init = Admin.class,
            update = { CustomerContractualContact.class, CustomerFinancialContact.class },
            read = CustomerContractualContact.class)
    private String billingSalutation;

    @Size(max = 400)
    @AccessFor(
            init = Admin.class,
            update = Admin.class,
            read = { CustomerContractualContact.class, CustomerFinancialContact.class })
    private String billingAddress;

    @Size(max = 160)
    @AccessFor(init = Admin.class, update = Supporter.class, read = Supporter.class)
    private String remark;

    @AccessFor(init = Anybody.class, update = Anybody.class, read = AnyCustomerUser.class)
    private String displayLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getReference() {
        return reference;
    }

    public void setReference(Integer reference) {
        this.reference = reference;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CustomerKind getKind() {
        return kind;
    }

    public void setKind(CustomerKind kind) {
        this.kind = kind;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getRegistrationCourt() {
        return registrationCourt;
    }

    public void setRegistrationCourt(String registrationCourt) {
        this.registrationCourt = registrationCourt;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public VatRegion getVatRegion() {
        return vatRegion;
    }

    public void setVatRegion(VatRegion vatRegion) {
        this.vatRegion = vatRegion;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getContractualSalutation() {
        return contractualSalutation;
    }

    public void setContractualSalutation(String contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
    }

    public String getContractualAddress() {
        return contractualAddress;
    }

    public void setContractualAddress(String contractualAddress) {
        this.contractualAddress = contractualAddress;
    }

    public String getBillingSalutation() {
        return billingSalutation;
    }

    public void setBillingSalutation(String billingSalutation) {
        this.billingSalutation = billingSalutation;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(final String displayLabel) {
        this.displayLabel = displayLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomerDTO customerDTO = (CustomerDTO) o;
        if (customerDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), customerDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "CustomerDTO{" +
                "id=" + getId() +
                ", reference=" + getReference() +
                ", prefix='" + getPrefix() + "'" +
                ", name='" + getName() + "'" +
                ", kind='" + getKind() + "'" +
                ", birthDate='" + getBirthDate() + "'" +
                ", birthPlace='" + getBirthPlace() + "'" +
                ", registrationCourt='" + getRegistrationCourt() + "'" +
                ", registrationNumber='" + getRegistrationNumber() + "'" +
                ", vatRegion='" + getVatRegion() + "'" +
                ", vatNumber='" + getVatNumber() + "'" +
                ", contractualSalutation='" + getContractualSalutation() + "'" +
                ", contractualAddress='" + getContractualAddress() + "'" +
                ", billingSalutation='" + getBillingSalutation() + "'" +
                ", billingAddress='" + getBillingAddress() + "'" +
                ", remark='" + getRemark() + "'" +
                "}";
    }

    @JsonComponent
    public static class CustomerJsonSerializer extends JsonSerializerWithAccessFilter<CustomerDTO> {

        public CustomerJsonSerializer(final ApplicationContext ctx, final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }

    @JsonComponent
    public static class CustomerJsonDeserializer extends JsonDeserializerWithAccessFilter<CustomerDTO> {

        public CustomerJsonDeserializer(
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService) {
            super(ctx, userRoleAssignmentService);
        }
    }
}
