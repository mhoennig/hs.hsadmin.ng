package org.hostsharing.hsadminng.service.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A DTO for the Membership entity.
 */
public class MembershipDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate from;

    private LocalDate to;

    @Size(max = 160)
    private String comment;

    @NotNull
    private Long customerId;

    private String customerPrefix;

    public MembershipDTO with(
        Consumer<MembershipDTO> builderFunction) {
        builderFunction.accept(this);
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPrefix() {
        return customerPrefix;
    }

    public void setCustomerPrefix(String customerPrefix) {
        this.customerPrefix = customerPrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MembershipDTO membershipDTO = (MembershipDTO) o;
        if (membershipDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), membershipDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "MembershipDTO{" +
            "id=" + getId() +
            ", from='" + getFrom() + "'" +
            ", to='" + getTo() + "'" +
            ", comment='" + getComment() + "'" +
            ", customer=" + getCustomerId() +
            ", customer='" + getCustomerPrefix() + "'" +
            "}";
    }
}
