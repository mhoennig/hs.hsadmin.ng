package org.hostsharing.hsadminng.service.dto;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.util.RandomUtil;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AssetDTOUnitTest extends AccessMappingsUnitTestBase<AssetDTO> {

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(AssetDTO.class, Role.ADMIN).shouldBeExactlyFor(
            "membershipId", "documentDate", "amount", "action", "valueDate", "remark");
        updateAccessFor(AssetDTO.class, Role.ADMIN).shouldBeExactlyFor("remark");
        readAccessFor(AssetDTO.class, Role.ADMIN).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        readAccessFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeExactlyFor(
            "id", "membershipId", "documentDate", "amount", "action", "valueDate", "membershipDisplayLabel");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccessFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        readAccessFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        updateAccessFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        readAccessFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
    }

    // --- only test fixture below ---

    @Override
    public AssetDTO createSampleDto(final Long id) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        dto.setDocumentDate(LocalDate.parse("2000-12-07"));
        dto.setAmount(new BigDecimal("512.01"));
        dto.setAction(AssetAction.PAYMENT);
        dto.setRemark("Some Remark");
        dto.setRemark(null);
        dto.setValueDate(LocalDate.parse("2000-12-18"));
        dto.setMembershipId(888L);
        dto.setMembershipId(null);
        dto.setMembershipDisplayLabel("Some Membership");
        return dto;
    }

    @Override
    public AssetDTO createRandomDto(final Long id) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setDocumentDate(randomDate);
        dto.setAmount(new BigDecimal(RandomUtils.nextDouble()));
        dto.setAction(RandomUtil.generateEnumValue(AssetAction.class));
        dto.setRemark(RandomStringUtils.randomAlphanumeric(20));
        dto.setValueDate(randomDate.plusDays(RandomUtils.nextInt(1, 99)));
        dto.setMembershipId(RandomUtils.nextLong());
        dto.setMembershipDisplayLabel("The Membership #" + dto.getMembershipId());
        return dto;
    }
}
