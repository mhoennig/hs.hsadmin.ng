package net.hostsharing.hsadminng.hs.office.contact;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_contact_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("Contact")
public class HsOfficeContactEntity implements Stringifyable, RbacObject {

    private static Stringify<HsOfficeContactEntity> toString = stringify(HsOfficeContactEntity.class, "contact")
            .withProp(Fields.caption, HsOfficeContactEntity::getCaption)
            .withProp(Fields.emailAddresses, HsOfficeContactEntity::getEmailAddresses);

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "caption")
    private String caption;

    @Column(name = "postaladdress")
    private String postalAddress; // multiline free-format text

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

    public PatchableMapWrapper<String> getEmailAddresses() {
        return PatchableMapWrapper.of(emailAddressesWrapper, (newWrapper) -> {emailAddressesWrapper = newWrapper; }, emailAddresses );
    }

    public void putEmailAddresses(Map<String, String> newEmailAddresses) {
        getEmailAddresses().assign(newEmailAddresses);
    }

    public PatchableMapWrapper<String> getPhoneNumbers() {
        return PatchableMapWrapper.of(phoneNumbersWrapper, (newWrapper) -> {phoneNumbersWrapper = newWrapper; }, phoneNumbers );
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

    public static RbacView rbac() {
        return rbacViewFor("contact", HsOfficeContactEntity.class)
                .withIdentityView(SQL.projection("caption"))
                .withUpdatableColumns("caption", "postalAddress", "emailAddresses", "phoneNumbers")
                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(REFERRER, (with) -> {
                    with.permission(SELECT);
                })
                .toRole(GLOBAL, GUEST).grantPermission(INSERT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/501-contact/5013-hs-office-contact-rbac");
    }
}
