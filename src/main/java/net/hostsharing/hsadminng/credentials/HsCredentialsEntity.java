package net.hostsharing.hsadminng.credentials;

import jakarta.persistence.*;
import lombok.*;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity; // Assuming BaseEntity exists
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
// import net.hostsharing.hsadminng.rbac.RbacSubjectEntity; // Assuming RbacSubjectEntity exists for the FK relationship

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.REFRESH;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_credentials", name = "credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsCredentialsEntity implements BaseEntity<HsCredentialsEntity>, Stringifyable {

    protected static Stringify<HsCredentialsEntity> stringify = stringify(HsCredentialsEntity.class, "loginCredentials")
            .withProp(HsCredentialsEntity::isActive)
            .withProp(HsCredentialsEntity::getEmailAddress)
            .withProp(HsCredentialsEntity::getTwoFactorAuth)
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
    private HsOfficePersonRealEntity person;

    @Version
    private int version;

    @Column
    private boolean active;

    @Column
    private Integer globalUid;

    @Column
    private Integer globalGid;

    @Column
    private String onboardingToken;

    @Column
    private String twoFactorAuth;

    @Column
    private String phonePassword;

    @Column
    private String emailAddress;

    @Column
    private String smsNumber;

    @OneToMany(fetch = FetchType.LAZY, cascade = { MERGE, REFRESH }, orphanRemoval = true)
    @JoinTable(
            name = "context_mapping", schema = "hs_credentials",
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

    @Override
    public String toShortString() {
        return active + ":" + emailAddress + ":" + globalUid;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }
}
