// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerTechnicalContact;
import org.hostsharing.hsadminng.service.util.RandomUtil;
import org.junit.Test;

import java.time.LocalDate;

public class ShareDTOUnitTest extends AccessMappingsUnitTestBase<ShareDTO> {

    public ShareDTOUnitTest() {
        super(ShareDTO.class, ShareDTOUnitTest::createSampleDTO, ShareDTOUnitTest::createRandomDTO);
    }

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(ShareDTO.class, Admin.ROLE).shouldBeExactlyFor(
                "membershipId",
                "documentDate",
                "quantity",
                "action",
                "valueDate",
                "remark");
        updateAccessFor(ShareDTO.class, Admin.ROLE).shouldBeExactlyFor("remark");
        readAccessFor(ShareDTO.class, Admin.ROLE).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(ShareDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        readAccessFor(ShareDTO.class, CustomerContractualContact.ROLE).shouldBeExactlyFor(
                "id",
                "membershipId",
                "documentDate",
                "quantity",
                "action",
                "valueDate",
                "membershipDisplayLabel");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccessFor(ShareDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
        readAccessFor(ShareDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(ShareDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        readAccessFor(ShareDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
    }

    // --- only test fixture below ---

    private static ShareDTO createSampleDTO(final Long id, final Long parentId) {
        final ShareDTO dto = new ShareDTO();
        dto.setId(id);
        dto.setMembershipId(parentId);
        dto.setAction(ShareAction.SUBSCRIPTION);
        dto.setQuantity(3);
        dto.setDocumentDate(LocalDate.parse("2019-04-22"));
        dto.setValueDate(LocalDate.parse("2019-04-30"));
        dto.setRemark("Some Remark");
        dto.setMembershipDisplayLabel("The Membership #888");
        return dto;
    }

    private static ShareDTO createRandomDTO(final Long id, final Long parentId) {
        final ShareDTO dto = new ShareDTO();
        dto.setId(id);
        dto.setMembershipId(parentId);
        dto.setAction(RandomUtil.generateEnumValue(ShareAction.class));
        dto.setQuantity(RandomUtils.nextInt());
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setDocumentDate(randomDate);
        dto.setValueDate(randomDate.plusDays(RandomUtils.nextInt(1, 99)));
        dto.setRemark(RandomStringUtils.randomAlphanumeric(20));
        dto.setMembershipDisplayLabel("The Membership #" + dto.getMembershipId());
        return dto;
    }
}
