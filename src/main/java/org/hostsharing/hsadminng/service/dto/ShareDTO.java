package org.hostsharing.hsadminng.service.dto;
import java.time.LocalDate;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;

/**
 * A DTO for the Share entity.
 */
public class ShareDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate date;

    @NotNull
    private ShareAction action;

    @NotNull
    private Integer quantity;

    @Size(max = 160)
    private String comment;


    private Long memberId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ShareAction getAction() {
        return action;
    }

    public void setAction(ShareAction action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long membershipId) {
        this.memberId = membershipId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShareDTO shareDTO = (ShareDTO) o;
        if (shareDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), shareDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ShareDTO{" +
            "id=" + getId() +
            ", date='" + getDate() + "'" +
            ", action='" + getAction() + "'" +
            ", quantity=" + getQuantity() +
            ", comment='" + getComment() + "'" +
            ", member=" + getMemberId() +
            "}";
    }
}
