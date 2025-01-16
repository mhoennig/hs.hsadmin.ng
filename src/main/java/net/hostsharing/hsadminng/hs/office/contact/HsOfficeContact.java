package net.hostsharing.hsadminng.hs.office.contact;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.role.WithRoleId;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
@FieldNameConstants
@DisplayAs("Contact")
public class HsOfficeContact implements Stringifyable, BaseEntity<HsOfficeContact>, WithRoleId {

    private static Stringify<HsOfficeContact> toString = stringify(HsOfficeContact.class, "contact")
            .withProp(Fields.caption, HsOfficeContact::getCaption)
            .withProp(Fields.emailAddresses, HsOfficeContact::getEmailAddresses);

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(name = "postaladdress")
    private Map<String, String> postalAddress = new HashMap<>();

    @Transient
    private PatchableMapWrapper<String> postalAddressWrapper;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(name = "emailaddresses")
    private Map<String, String> emailAddresses = new HashMap<>();

    @Transient
    private PatchableMapWrapper<String> emailAddressesWrapper;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(name = "phonenumbers")
    private Map<String, String> phoneNumbers = new HashMap<>();

    @Transient
    private PatchableMapWrapper<String> phoneNumbersWrapper;

    public PatchableMapWrapper<String> getPostalAddress() {
        return PatchableMapWrapper.of(
                postalAddressWrapper,
                (newWrapper) -> {postalAddressWrapper = newWrapper;},
                postalAddress);
    }

    public void putPostalAddress(Map<String, String> newPostalAddress) {
        getPostalAddress().assign(newPostalAddress);
    }

    public PatchableMapWrapper<String> getEmailAddresses() {
        return PatchableMapWrapper.of(
                emailAddressesWrapper,
                (newWrapper) -> {emailAddressesWrapper = newWrapper;},
                emailAddresses);
    }

    public void putEmailAddresses(Map<String, String> newEmailAddresses) {
        getEmailAddresses().assign(newEmailAddresses);
    }

    public PatchableMapWrapper<String> getPhoneNumbers() {
        return PatchableMapWrapper.of(phoneNumbersWrapper, (newWrapper) -> {phoneNumbersWrapper = newWrapper;}, phoneNumbers);
    }

    public void putPhoneNumbers(Map<String, String> newPhoneNumbers) {
        getPhoneNumbers().assign(newPhoneNumbers);
    }

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return caption;
    }
}
