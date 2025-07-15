package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;

import java.util.UUID;

import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(builderMethodName = "baseBuilder", toBuilder = true)
@MappedSuperclass
public abstract class HsCredentialsContext implements Stringifyable, BaseEntity<HsCredentialsContext> {

    private static Stringify<HsCredentialsContext> stringify = stringify(HsCredentialsContext.class, "loginContext")
            .withProp(HsCredentialsContext::getType)
            .withProp(HsCredentialsContext::getQualifier)
            .quotedValues(false)
            .withSeparator(":");

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @NotNull
    @Column
    private int version;

    @NotNull
    @Column(name = "type", length = 16)
    private String type;

    @Column(name = "qualifier", length = 80)
    private String qualifier;

    @Column(name = "only_for_natural_persons")
    private boolean onlyForNaturalPersons;

    @Override
    public String toShortString() {
        return toString();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }
}
