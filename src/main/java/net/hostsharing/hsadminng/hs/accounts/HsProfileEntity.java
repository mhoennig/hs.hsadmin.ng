package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.*;
import lombok.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity; // Assuming BaseEntity exists
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
// import net.hostsharing.hsadminng.rbac.RbacSubjectEntity; // Assuming RbacSubjectEntity exists for the FK relationship

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REFRESH;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_accounts", name = "profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsProfileEntity implements BaseEntity<HsProfileEntity>, Stringifyable {

    protected static Stringify<HsProfileEntity> stringify = stringify(HsProfileEntity.class, "profile")
            .withProp(HsProfileEntity::isActive)
            .withProp(HsProfileEntity::getEmailAddress)
            .withProp(HsProfileEntity::getTotpSecrets)
            .withProp(HsProfileEntity::getPhonePassword)
            .withProp(HsProfileEntity::getSmsNumber)
            .quotedValues(false);

    @Id
    private UUID uuid;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    private RbacSubjectEntity subject;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    private HsOfficePersonRealEntity person; // TODO.impl: add RBAC-Support to ProfileEntity, see Story #

    @Version
    private int version;

    @Column
    private boolean active;

    @Column
    private Integer globalUid;

    @Column
    private Integer globalGid;

    @Column
    private List<String> totpSecrets;

    @Column
    private String phonePassword;

    @Column
    private String emailAddress;

    @Column
    private String smsNumber;

    @OneToMany(fetch = FetchType.EAGER, cascade = { MERGE, REFRESH })
    @JoinTable(
            name = "scope_mapping", schema = "hs_accounts",
            joinColumns = @JoinColumn(name = "profile_uuid", referencedColumnName = "uuid"),
            inverseJoinColumns = @JoinColumn(name = "scope_uuid", referencedColumnName = "uuid")
    )
    private Set<HsProfileScopeRealEntity> scopes;

    public Set<HsProfileScopeRealEntity> getScopes() {
        if ( scopes == null ) {
            scopes = new HashSet<>();
        }
        return scopes;
    }

    public void setSubject(final RbacSubjectEntity subject) {
        this.uuid = subject.getUuid();
        this.subject = subject;
    }

    @Override
    public String toShortString() {
        return active + ":" + emailAddress + ":" + globalUid;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

}
