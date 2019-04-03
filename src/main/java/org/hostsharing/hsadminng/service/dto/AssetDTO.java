package org.hostsharing.hsadminng.service.dto;
import java.time.LocalDate;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;

/**
 * A DTO for the Asset entity.
 */
public class AssetDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate date;

    @NotNull
    private AssetAction action;

    @NotNull
    private BigDecimal amount;

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

    public AssetAction getAction() {
        return action;
    }

    public void setAction(AssetAction action) {
        this.action = action;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

        AssetDTO assetDTO = (AssetDTO) o;
        if (assetDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), assetDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "AssetDTO{" +
            "id=" + getId() +
            ", date='" + getDate() + "'" +
            ", action='" + getAction() + "'" +
            ", amount=" + getAmount() +
            ", comment='" + getComment() + "'" +
            ", member=" + getMemberId() +
            "}";
    }
}
