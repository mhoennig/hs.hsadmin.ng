package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.*;
import lombok.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity; // Assuming BaseEntity exists
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
// import net.hostsharing.hsadminng.rbac.RbacSubjectEntity; // Assuming RbacSubjectEntity exists for the FK relationship

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REFRESH;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_accounts", name = "credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsCredentialsEntity implements BaseEntity<HsCredentialsEntity>, Stringifyable {

    protected static Stringify<HsCredentialsEntity> stringify = stringify(HsCredentialsEntity.class, "credentials")
            .withProp(HsCredentialsEntity::isActive)
            .withProp(HsCredentialsEntity::getEmailAddress)
            .withProp(HsCredentialsEntity::getTotpSecrets)
            .withProp(HsCredentialsEntity::getPhonePassword)
            .withProp(HsCredentialsEntity::getSmsNumber)
            .quotedValues(false);

    @Id
    private UUID uuid;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    private RbacSubjectEntity subject;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_uuid", nullable = false, updatable = false, referencedColumnName = "uuid")
    private HsOfficePersonRbacEntity person;

    @Version
    private int version;

    @Column
    private LocalDateTime lastUsed;

    @Column
    private boolean active;

    @Column
    private Integer globalUid;

    @Column
    private Integer globalGid;

    @Column
    private String onboardingToken;

    @Column
    private List<String> totpSecrets;

    @Column
    private String phonePassword;

    @Column
    private String emailAddress;

    @Column
    private String smsNumber;

    @OneToMany(fetch = FetchType.LAZY, cascade = { MERGE, REFRESH }, orphanRemoval = true)
    @JoinTable(
            name = "context_mapping", schema = "hs_accounts",
            joinColumns = @JoinColumn(name = "credentials_uuid", referencedColumnName = "uuid"),
            inverseJoinColumns = @JoinColumn(name = "context_uuid", referencedColumnName = "uuid")
    )
    private Set<HsCredentialsContextRealEntity> loginContexts;

    public Set<HsCredentialsContextRealEntity> getLoginContexts() {
        if ( loginContexts == null ) {
            loginContexts = new HashSet<>();
        }
        return loginContexts;
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
