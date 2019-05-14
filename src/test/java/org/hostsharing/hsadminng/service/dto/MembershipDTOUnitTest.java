// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.Role;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.time.LocalDate;

public class MembershipDTOUnitTest extends AccessMappingsUnitTestBase<MembershipDTO> {

    public MembershipDTOUnitTest() {
        super(MembershipDTO.class, MembershipDTOUnitTest::createSampleDTO, MembershipDTOUnitTest::createRandomDTO);
    }

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(MembershipDTO.class, Role.ADMIN).shouldBeExactlyFor(
                "admissionDocumentDate",
                "cancellationDocumentDate",
                "memberFromDate",
                "memberUntilDate",
                "customerId",
                "remark");
        updateAccessFor(MembershipDTO.class, Role.ADMIN).shouldBeExactlyFor(
                "cancellationDocumentDate",
                "memberUntilDate",
                "remark");
        readAccessFor(MembershipDTO.class, Role.ADMIN).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForSupporter() {
        initAccessFor(MembershipDTO.class, Role.SUPPORTER).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Role.SUPPORTER).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Role.SUPPORTER).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(MembershipDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeExactlyFor(
                "id",
                "admissionDocumentDate",
                "cancellationDocumentDate",
                "memberFromDate",
                "memberUntilDate",
                "customerId",
                "customerPrefix",
                "customerDisplayLabel",
                "displayLabel");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccessFor(MembershipDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(MembershipDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
    }

    // --- only test fixture below ---

    public static MembershipDTO createSampleDTO(final Long id, final Long parentId) {
        final MembershipDTO dto = new MembershipDTO();
        dto.setId(id);
        final LocalDate referenceDate = LocalDate.parse("2000-12-07");
        dto.setAdmissionDocumentDate(referenceDate);
        dto.setCancellationDocumentDate(referenceDate.plusDays(3500));
        dto.setMemberFromDate(referenceDate.plusDays(4));
        dto.setMemberUntilDate(referenceDate.plusDays(3500).plusDays(400).withDayOfYear(1).minusDays(1));
        dto.setRemark("Some Remark");
        dto.setCustomerId(parentId);
        dto.setCustomerPrefix("abc");
        dto.setCustomerDisplayLabel("ABC GmbH [abc:10001]");
        dto.setDisplayLabel("ABC GmbH [abc:10001] 2000-12-11 - 2011-12-31");
        return dto;
    }

    public static MembershipDTO createRandomDTO(final Long id, final Long parentId) {
        final MembershipDTO dto = new MembershipDTO();
        dto.setId(id);
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setAdmissionDocumentDate(randomDate);
        dto.setCancellationDocumentDate(randomDate.plusDays(3500));
        dto.setMemberFromDate(randomDate.plusDays(4));
        dto.setMemberUntilDate(randomDate.plusDays(3500).plusDays(400).withDayOfYear(1).minusDays(1));
        dto.setRemark(RandomStringUtils.randomAlphanumeric(20).toUpperCase());
        dto.setCustomerId(parentId);
        dto.setCustomerPrefix(RandomStringUtils.randomAlphabetic(3).toLowerCase());
        dto.setCustomerDisplayLabel(RandomStringUtils.randomAlphabetic(13));
        dto.setDisplayLabel(dto.getCustomerDisplayLabel() + dto.getMemberFromDate() + " - " + dto.getMemberUntilDate());
        return dto;
    }
}
