// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.util.RandomUtil;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AssetDTOUnitTest extends AccessMappingsUnitTestBase<AssetDTO> {

    public AssetDTOUnitTest() {
        super(AssetDTO.class, AssetDTOUnitTest::createSampleDTO, AssetDTOUnitTest::createRandomDTO);
    }

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(AssetDTO.class, Admin.ROLE).shouldBeExactlyFor(
                "membershipId",
                "documentDate",
                "amount",
                "action",
                "valueDate",
                "remark");
        updateAccessFor(AssetDTO.class, Admin.ROLE).shouldBeExactlyFor("remark");
        readAccessFor(AssetDTO.class, Admin.ROLE).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(AssetDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        readAccessFor(AssetDTO.class, CustomerContractualContact.ROLE).shouldBeExactlyFor(
                "id",
                "membershipId",
                "documentDate",
                "amount",
                "action",
                "valueDate",
                "membershipDisplayLabel");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccessFor(AssetDTO.class, Role.CustomerTechnicalContact.ROLE).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, Role.CustomerTechnicalContact.ROLE).shouldBeForNothing();
        readAccessFor(AssetDTO.class, Role.CustomerTechnicalContact.ROLE).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(AssetDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        readAccessFor(AssetDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
    }

    // --- only test fixture below ---

    private static AssetDTO createSampleDTO(final Long id, final Long parentId) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        dto.setDocumentDate(LocalDate.parse("2000-12-07"));
        dto.setAmount(new BigDecimal("512.01"));
        dto.setAction(AssetAction.PAYMENT);
        dto.setRemark("Some Remark");
        dto.setValueDate(LocalDate.parse("2000-12-18"));
        dto.setMembershipId(parentId);
        dto.setMembershipDisplayLabel("Some Membership");
        return dto;
    }

    private static AssetDTO createRandomDTO(final Long id, final Long parentId) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setDocumentDate(randomDate);
        dto.setAmount(new BigDecimal(RandomUtils.nextDouble()));
        dto.setAction(RandomUtil.generateEnumValue(AssetAction.class));
        dto.setRemark(RandomStringUtils.randomAlphanumeric(20));
        dto.setValueDate(randomDate.plusDays(RandomUtils.nextInt(1, 99)));
        dto.setMembershipId(parentId);
        dto.setMembershipDisplayLabel("The Membership #" + dto.getMembershipId());
        return dto;
    }
}
