package org.hostsharing.hsadminng.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A Share.
 */
@Entity
@Table(name = "share")
public class Share implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ENTITY_NAME = "share";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "jhi_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ShareAction action;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Size(max = 160)
    @Column(name = "jhi_comment", length = 160)
    private String comment;

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties("shares")
    private Membership member;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Share date(LocalDate date) {
        this.date = date;
        return this;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ShareAction getAction() {
        return action;
    }

    public Share action(ShareAction action) {
        this.action = action;
        return this;
    }

    public void setAction(ShareAction action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Share quantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getComment() {
        return comment;
    }

    public Share comment(String comment) {
        this.comment = comment;
        return this;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Membership getMember() {
        return member;
    }

    public Share member(Membership membership) {
        this.member = membership;
        return this;
    }

    public void setMember(Membership membership) {
        this.member = membership;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Share share = (Share) o;
        if (share.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), share.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Share{" +
            "id=" + getId() +
            ", date='" + getDate() + "'" +
            ", action='" + getAction() + "'" +
            ", quantity=" + getQuantity() +
            ", comment='" + getComment() + "'" +
            "}";
    }
}
