// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerTechnicalContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.Supporter;

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
        initAccessFor(MembershipDTO.class, Admin.ROLE).shouldBeExactlyFor(
                "admissionDocumentDate",
                "cancellationDocumentDate",
                "memberFromDate",
                "memberUntilDate",
                "customerId",
                "remark");
        updateAccessFor(MembershipDTO.class, Admin.ROLE).shouldBeExactlyFor(
                "cancellationDocumentDate",
                "memberUntilDate",
                "remark");
        readAccessFor(MembershipDTO.class, Admin.ROLE).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForSupporter() {
        initAccessFor(MembershipDTO.class, Supporter.ROLE).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Supporter.ROLE).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Supporter.ROLE).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(MembershipDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, CustomerContractualContact.ROLE).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, CustomerContractualContact.ROLE).shouldBeExactlyFor(
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
        initAccessFor(MembershipDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, CustomerTechnicalContact.ROLE).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(MembershipDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        updateAccessFor(MembershipDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
        readAccessFor(MembershipDTO.class, Role.AnyCustomerUser.ROLE).shouldBeForNothing();
    }

    // --- only test fixture below ---

    static MembershipDTO createSampleDTO(final Long id, final Long parentId) {
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
