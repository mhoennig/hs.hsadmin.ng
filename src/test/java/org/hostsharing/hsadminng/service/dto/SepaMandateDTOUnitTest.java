// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.Role;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.time.LocalDate;

public class SepaMandateDTOUnitTest extends AccessMappingsUnitTestBase<SepaMandateDTO> {

    public SepaMandateDTOUnitTest() {
        super(SepaMandateDTO.class, SepaMandateDTOUnitTest::createSampleDTO, SepaMandateDTOUnitTest::createRandomDTO);
    }

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccessFor(SepaMandateDTO.class, Role.ADMIN).shouldBeExactlyFor(
                "grantingDocumentDate",
                "bic",
                "remark",
                "validUntilDate",
                "customerId",
                "validFromDate",
                "iban",
                "revokationDocumentDate",
                "lastUsedDate",
                "reference");
        updateAccessFor(SepaMandateDTO.class, Role.ADMIN).shouldBeExactlyFor(
                "remark",
                "validUntilDate",
                "revokationDocumentDate",
                "lastUsedDate");
        readAccessFor(SepaMandateDTO.class, Role.ADMIN).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForSupporter() {
        initAccessFor(SepaMandateDTO.class, Role.SUPPORTER).shouldBeExactlyFor(
                "grantingDocumentDate",
                "bic",
                "validUntilDate",
                "customerId",
                "validFromDate",
                "iban",
                "reference");
        updateAccessFor(SepaMandateDTO.class, Role.SUPPORTER).shouldBeExactlyFor(
                "remark",
                "validUntilDate",
                "revokationDocumentDate");
        readAccessFor(SepaMandateDTO.class, Role.SUPPORTER).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccessFor(SepaMandateDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeExactlyFor(
                "grantingDocumentDate",
                "bic",
                "validUntilDate",
                "customerId",
                "validFromDate",
                "iban",
                "reference");
        updateAccessFor(SepaMandateDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeExactlyFor(
                "validUntilDate",
                "revokationDocumentDate");
        readAccessFor(SepaMandateDTO.class, Role.CUSTOMER_CONTRACTUAL_CONTACT).shouldBeExactlyFor(
                "grantingDocumentDate",
                "bic",
                "id",
                "validUntilDate",
                "customerId",
                "validFromDate",
                "iban",
                "revokationDocumentDate",
                "customerDisplayLabel",
                "lastUsedDate",
                "reference");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccessFor(SepaMandateDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
        updateAccessFor(SepaMandateDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
        readAccessFor(SepaMandateDTO.class, Role.CUSTOMER_TECHNICAL_CONTACT).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccessFor(SepaMandateDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        updateAccessFor(SepaMandateDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        readAccessFor(SepaMandateDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
    }

    // --- only test fixture below ---

    public static SepaMandateDTO createSampleDTO(final Long id, final Long parentId) {
        final SepaMandateDTO dto = new SepaMandateDTO();
        dto.setId(id);
        dto.setReference("Some Reference");
        dto.setGrantingDocumentDate(LocalDate.parse("2000-12-07"));
        dto.setRevokationDocumentDate(LocalDate.parse("2019-04-27"));
        dto.setValidFromDate(LocalDate.parse("2000-12-18"));
        dto.setValidUntilDate(LocalDate.parse("2019-05-31"));
        dto.setLastUsedDate(LocalDate.parse("2019-04-04"));
        dto.setIban("DE1234IBAN");
        dto.setBic("BIC1234");
        dto.setRemark("Some Remark");
        dto.setCustomerId(parentId);
        dto.setCustomerDisplayLabel("abc");
        return dto;
    }

    public static SepaMandateDTO createRandomDTO(final Long id, final Long parentId) {
        final SepaMandateDTO dto = new SepaMandateDTO();
        dto.setId(id);
        dto.setReference(RandomStringUtils.randomAlphanumeric(10));
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setGrantingDocumentDate(randomDate);
        dto.setRevokationDocumentDate(randomDate.plusDays(RandomUtils.nextInt(1100, 2999)));
        dto.setValidFromDate(randomDate.plusDays(RandomUtils.nextInt(0, 7)));
        dto.setValidUntilDate(dto.getRevokationDocumentDate().plusDays(7));
        dto.setLastUsedDate(dto.getRevokationDocumentDate().minusDays(20));
        dto.setIban(RandomStringUtils.randomAlphanumeric(20).toUpperCase());
        dto.setBic(RandomStringUtils.randomAlphanumeric(10).toUpperCase());
        dto.setRemark(RandomStringUtils.randomAlphanumeric(20).toUpperCase());
        dto.setCustomerId(parentId);
        dto.setCustomerDisplayLabel(RandomStringUtils.randomAlphabetic(3).toLowerCase());
        return dto;
    }
}
