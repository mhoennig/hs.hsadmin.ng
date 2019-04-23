package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hostsharing.hsadminng.service.accessfilter.AccessFor;
import org.hostsharing.hsadminng.service.accessfilter.JSonDeserializerWithAccessFilter;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.springframework.boot.jackson.JsonComponent;

import javax.validation.constraints.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Customer entity.
 */
public class CustomerDTO implements Serializable {

    @AccessFor(read = Role.ANY_CUSTOMER_USER)
    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private Integer reference;

    @NotNull
    @Size(max = 3)
    @Pattern(regexp = "[a-z][a-z0-9]+")
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private String prefix;

    @NotNull
    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private String name;

    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, update = Role.CONTRACTUAL_CONTACT, read = Role.ANY_CUSTOMER_CONTACT)
    private String contractualSalutation;

    @NotNull
    @Size(max = 400)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.CONTRACTUAL_CONTACT)
    private String contractualAddress;

    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, update = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = Role.CONTRACTUAL_CONTACT)
    private String billingSalutation;

    @Size(max = 400)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.CONTRACTUAL_CONTACT)
    private String billingAddress;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, update = Role.SUPPORTER, read = Role.SUPPORTER)
    private String remark;

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
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }

    @JsonComponent
    public static class UserJsonDeserializer extends JsonDeserializer<CustomerDTO> {

        @Override
        public CustomerDTO deserialize(final JsonParser jsonParser,
                                       final DeserializationContext deserializationContext) {

           return new JSonDeserializerWithAccessFilter<>(jsonParser, deserializationContext, CustomerDTO.class).deserialize();
        }
    }
}

