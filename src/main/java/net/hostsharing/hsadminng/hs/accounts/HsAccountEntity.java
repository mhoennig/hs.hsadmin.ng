package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.*;
import jakarta.validation.ValidationException;

import lombok.*;
import net.hostsharing.hsadminng.hash.LdapArgon2Hash;
import net.hostsharing.hsadminng.hash.LdapSshaHash;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity; // Assuming BaseEntity exists
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;

import java.util.UUID;

import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_accounts", name = "account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsAccountEntity implements BaseEntity<HsAccountEntity>, Stringifyable {

    protected static Stringify<HsAccountEntity> stringify = stringify(HsAccountEntity.class, "account")
            .withProp(e -> e.getSubject().getName())
            .quotedValues(false);

    @Id
    private UUID uuid;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    // Must be the real subject, not the RBAC-subject,
    // so that representative persons can access accounts+subjects of represented persons.
    // Otherwise, we would also need to allow RBAC grants to subject roles.
    // This also means that each access has to be checked explicitly (same subject or represented subject).
    private RealSubjectEntity subject;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    private HsOfficePersonRealEntity person; // TODO.spec: Do we need ReBAC-Support for AccountEntity?

    @Version
    private int version;

    @Column
    private Integer globalUid;

    @Column
    private Integer globalGid;

    public void setSubject(final RealSubjectEntity subject) {
        this.uuid = subject.getUuid();
        this.subject = subject;
    }

    @Override
    public String toShortString() {
        return subject.getName();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    private static void validatePasswordHash(final String passwordHash) {

        if (!LdapSshaHash.isValid(passwordHash) && !LdapArgon2Hash.isValid(passwordHash)) {
            throw new ValidationException("passwordHash must be SSHA or ARGON2 hash valid for LDAP");
        }
    }
}
