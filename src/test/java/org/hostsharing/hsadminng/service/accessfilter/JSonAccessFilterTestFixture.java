package org.hostsharing.hsadminng.service.accessfilter;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.service.IdToDtoResolver;
import org.hostsharing.hsadminng.service.dto.FluentBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hostsharing.hsadminng.service.accessfilter.Role.*;

public class JSonAccessFilterTestFixture {

    static GivenDto createSampleDto() {
        final GivenDto dto = new GivenDto();
        dto.customerId = 888L;
        dto.restrictedField = RandomStringUtils.randomAlphabetic(10);
        dto.openStringField = RandomStringUtils.randomAlphabetic(10);
        dto.openIntegerField = RandomUtils.nextInt();
        dto.openPrimitiveIntField = RandomUtils.nextInt();
        dto.openLongField = RandomUtils.nextLong();
        dto.openPrimitiveLongField = RandomUtils.nextLong();
        dto.openBooleanField = true;
        dto.openPrimitiveBooleanField = false;
        dto.openBigDecimalField = new BigDecimal("987654321234567890987654321234567890.09");
        dto.openLocalDateField = LocalDate.parse("2019-04-25");
        dto.openLocalDateFieldAsString = "2019-04-25";
        dto.openEnumField = TestEnum.GREEN;
        dto.openEnumFieldAsString = "GREEN";
        return dto;
    }

    static class GivenCustomerDto extends FluentBuilder<GivenCustomerDto> {
        @SelfId(resolver = GivenService.class)
        @AccessFor(read = ANYBODY)
        Long id;
    }

    static abstract class GivenCustomerService implements IdToDtoResolver<GivenCustomerDto> {
    }

    static class GivenDto extends FluentBuilder<GivenDto> {
        @SelfId(resolver = GivenService.class)
        @AccessFor(read = ANYBODY)
        Long id;

        @ParentId(resolver = GivenCustomerService.class)
        @AccessFor(init = ACTUAL_CUSTOMER_USER, update = ACTUAL_CUSTOMER_USER, read = ACTUAL_CUSTOMER_USER)
        Long customerId;

        @AccessFor(init = {TECHNICAL_CONTACT, FINANCIAL_CONTACT}, update = {TECHNICAL_CONTACT, FINANCIAL_CONTACT}, read = {TECHNICAL_CONTACT, FINANCIAL_CONTACT})
        String restrictedField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        String openStringField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        Integer openIntegerField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        int openPrimitiveIntField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        Long openLongField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        long openPrimitiveLongField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        Boolean openBooleanField;

        @AccessFor(read = ANYBODY)
        boolean openPrimitiveBooleanField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        LocalDate openLocalDateField;
        transient String openLocalDateFieldAsString;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        LocalDate openLocalDateField2;
        transient String openLocalDateField2AsString;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        TestEnum openEnumField;
        transient String openEnumFieldAsString;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        BigDecimal openBigDecimalField;

        @AccessFor(init = ANYBODY, update = ANYBODY, read = ANYBODY)
        int[] openArrayField;
    }

    static abstract class GivenService implements IdToDtoResolver<GivenDto> {
    }

    enum TestEnum {
        BLUE, GREEN
    }

    static abstract class GivenChildService implements IdToDtoResolver<GivenChildDto> {
    }

    public static class GivenChildDto extends FluentBuilder<GivenChildDto> {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @AccessFor(init = Role.CONTRACTUAL_CONTACT, update = Role.CONTRACTUAL_CONTACT, read = ACTUAL_CUSTOMER_USER)
        @ParentId(resolver = GivenService.class)
        Long parentId;

        @AccessFor(init = {TECHNICAL_CONTACT, FINANCIAL_CONTACT}, update = {TECHNICAL_CONTACT, FINANCIAL_CONTACT})
        String restrictedField;
    }

    public static class GivenDtoWithMultipleSelfId {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id2;

    }

    public static class GivenDtoWithUnknownFieldType {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANYBODY)
        Long id;

        @AccessFor(init = Role.ANYBODY, read = Role.ANYBODY)
        Arbitrary unknown;

    }

    public static class Arbitrary {
    }
}
