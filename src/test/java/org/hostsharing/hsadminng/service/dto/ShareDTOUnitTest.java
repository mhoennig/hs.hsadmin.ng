// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.util.RandomUtil;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.time.LocalDate;

public class ShareDTOUnitTest extends AccessMappingsUnitTestBase<ShareDTO> {

    public ShareDTOUnitTest() {
        super(ShareDTO.class, ShareDTOUnitTest::createSampleDTO, ShareDTOUnitTest::createRandomDTO);
    }

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(ShareDTO.class, Role.ADMIN).shouldBeExactlyFor(
                "membershipId",
                "documentDate",
                "quantity",
                "action",
                "valueDate",
                "remark");
        updateAccessFor(ShareDTO.class, Role.ADMIN).shouldBeExactlyFor("remark");
        readAccessFor(ShareDTO.class, Role.ADMIN).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(ShareDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeForNothing();
        readAccessFor(ShareDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeExactlyFor(
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
        initAccessFor(ShareDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
        readAccessFor(ShareDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(ShareDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        updateAccessFor(ShareDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        readAccessFor(ShareDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
    }

    // --- only test fixture below ---

    public static ShareDTO createSampleDTO(final Long id, final Long parentId) {
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

    public static ShareDTO createRandomDTO(final Long id, final Long parentId) {
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
