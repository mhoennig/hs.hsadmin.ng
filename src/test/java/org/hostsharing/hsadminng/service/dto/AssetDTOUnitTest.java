package org.hostsharing.hsadminng.service.dto;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetDTOUnitTest extends AccessMappingsUnitTestBase {

    @Test
    public void shouldHaveProperAccessForAdmin() {
        initAccesFor(AssetDTO.class, Role.ADMIN).shouldBeExactlyFor(
            "membershipId", "documentDate", "amount", "action", "valueDate", "remark");
        updateAccesFor(AssetDTO.class, Role.ADMIN).shouldBeExactlyFor("remark");
        readAccesFor(AssetDTO.class, Role.ADMIN).shouldBeForAllFields();
    }

    @Test
    public void shouldHaveProperAccessForContractualContact() {
        initAccesFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        updateAccesFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeForNothing();
        readAccesFor(AssetDTO.class, Role.CONTRACTUAL_CONTACT).shouldBeExactlyFor(
            "id", "membershipId", "documentDate", "amount", "action", "valueDate", "membershipDisplayLabel");
    }

    @Test
    public void shouldHaveNoAccessForTechnicalContact() {
        initAccesFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        updateAccesFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
        readAccesFor(AssetDTO.class, Role.TECHNICAL_CONTACT).shouldBeForNothing();
    }

    @Test
    public void shouldHaveNoAccessForNormalUsersWithinCustomerRealm() {
        initAccesFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        updateAccesFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
        readAccesFor(AssetDTO.class, Role.ANY_CUSTOMER_USER).shouldBeForNothing();
    }

    @Test
    public void shouldConvertToString() {
        final AssetDTO dto = createDto(1234L);
        assertThat(dto.toString()).isEqualTo("AssetDTO{id=1234, documentDate='2000-12-07', valueDate='2000-12-18', action='PAYMENT', amount=512.01, remark='Some Remark', membership=888, membershipDisplayLabel='Some Membership'}");
    }

    @Test
    public void shouldImplementEqualsJustUsingClassAndId() {
        final AssetDTO dto = createDto(1234L);
        assertThat(dto.equals(dto)).isTrue();

        final AssetDTO dtoWithSameId = createRandomDto(1234L);
        assertThat(dto.equals(dtoWithSameId)).isTrue();

        final AssetDTO dtoWithAnotherId = createRandomDto(RandomUtils.nextLong(2000, 9999));
        assertThat(dtoWithAnotherId.equals(dtoWithSameId)).isFalse();

        final AssetDTO dtoWithoutId = createRandomDto(null);
        assertThat(dto.equals(dtoWithoutId)).isFalse();
        assertThat(dtoWithoutId.equals(dto)).isFalse();

        assertThat(dto.equals(null)).isFalse();
        assertThat(dto.equals("")).isFalse();
    }

    // --- only test fixture below ---

    private AssetDTO createDto(final Long id) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        dto.setDocumentDate(LocalDate.parse("2000-12-07"));
        dto.setAmount(new BigDecimal("512.01"));
        dto.setAction(AssetAction.PAYMENT);
        dto.setRemark("Some Remark");
        dto.setValueDate(LocalDate.parse("2000-12-18"));
        dto.setMembershipId(888L);
        dto.setMembershipDisplayLabel("Some Membership");
        return dto;
    }


    private AssetDTO createRandomDto(final Long id) {
        final AssetDTO dto = new AssetDTO();
        dto.setId(id);
        final LocalDate randomDate = LocalDate.parse("2000-12-07").plusDays(RandomUtils.nextInt(1, 999));
        dto.setDocumentDate(randomDate);
        dto.setAmount(new BigDecimal("512.01"));
        dto.setAction(AssetAction.PAYMENT);
        dto.setRemark("Some Remark");
        dto.setValueDate(randomDate.plusDays(RandomUtils.nextInt(1, 99)));
        dto.setMembershipId(RandomUtils.nextLong());
        dto.setMembershipDisplayLabel(RandomStringUtils.randomAlphabetic(20));
        return dto;
    }

}
