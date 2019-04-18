package org.hostsharing.hsadminng.service.dto;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Customer entity.
 */
public class CustomerDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    private Integer number;

    @NotNull
    @Pattern(regexp = "[a-z][a-z0-9]+")
    private String prefix;

    @NotNull
    @Size(max = 80)
    private String name;

    @Size(max = 80)
    private String contractualSalutation;

    @NotNull
    @Size(max = 400)
    private String contractualAddress;

    @Size(max = 80)
    private String billingSalutation;

    @Size(max = 400)
    private String billingAddress;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
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
            ", number=" + getNumber() +
            ", prefix='" + getPrefix() + "'" +
            ", name='" + getName() + "'" +
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            "}";
    }
}
