// Licensed under Apache-2.0
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

    @EntityTypeId("test.GivenCustomer")
    static class GivenCustomerDto implements FluentBuilder<GivenCustomerDto> {

        @SelfId(resolver = GivenService.class)
        @AccessFor(read = Anybody.class)
        Long id;

        @AccessFor(update = Ignored.class, read = Anybody.class)
        String displayLabel;

    }

    static abstract class GivenCustomerService implements IdToDtoResolver<GivenCustomerDto> {
    }

    @EntityTypeId("test.Given")
    static class GivenDto implements AccessMappings, FluentBuilder<GivenDto> {

        @SelfId(resolver = GivenService.class)
        @AccessFor(read = Anybody.class)
        Long id;

        @ParentId(resolver = GivenCustomerService.class)
        @AccessFor(init = AnyCustomerUser.class, update = AnyCustomerUser.class, read = AnyCustomerUser.class)
        Long customerId;

        @AccessFor(
                init = { CustomerTechnicalContact.class, CustomerFinancialContact.class },
                update = { CustomerTechnicalContact.class, CustomerFinancialContact.class },
                read = { CustomerTechnicalContact.class, CustomerFinancialContact.class })
        String restrictedField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        String openStringField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        Integer openIntegerField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        int openPrimitiveIntField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        Long openLongField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        long openPrimitiveLongField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        Boolean openBooleanField;

        @AccessFor(read = Anybody.class)
        boolean openPrimitiveBooleanField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        LocalDate openLocalDateField;
        transient String openLocalDateFieldAsString;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        LocalDate openLocalDateField2;
        transient String openLocalDateField2AsString;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        TestEnum openEnumField;
        transient String openEnumFieldAsString;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        BigDecimal openBigDecimalField;

        @AccessFor(init = Supporter.class, update = Supporter.class, read = Supporter.class)
        BigDecimal restrictedBigDecimalField;

        @AccessFor(init = Anybody.class, update = Anybody.class, read = Anybody.class)
        int[] openArrayField;

        @AccessFor(init = Ignored.class, update = Ignored.class, read = Anybody.class)
        String displayLabel;

        @Override
        public Long getId() {
            return id;
        }
    }

    static abstract class GivenService implements IdToDtoResolver<GivenDto> {
    }

    enum TestEnum {
        BLUE,
        GREEN
    }

    static abstract class GivenChildService implements IdToDtoResolver<GivenChildDto> {
    }

    public static class GivenChildDto implements AccessMappings, FluentBuilder<GivenChildDto> {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = AnyCustomerUser.class)
        Long id;

        @AccessFor(
                init = CustomerContractualContact.class,
                update = CustomerContractualContact.class,
                read = AnyCustomerUser.class)
        @ParentId(resolver = GivenService.class)
        Long parentId;

        @AccessFor(
                init = { CustomerTechnicalContact.class, CustomerFinancialContact.class },
                update = {
                        CustomerTechnicalContact.class,
                        CustomerFinancialContact.class })
        String restrictedField;

        @Override
        public Long getId() {
            return id;
        }
    }

    public static class GivenDtoWithMultipleSelfId implements AccessMappings {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = AnyCustomerUser.class)
        Long id;

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = AnyCustomerUser.class)
        Long id2;

        @Override
        public Long getId() {
            return id;
        }
    }

    public static class GivenDtoWithUnknownFieldType implements AccessMappings {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Anybody.class)
        Long id;

        @AccessFor(init = Anybody.class, read = Anybody.class)
        Arbitrary unknown;

        @Override
        public Long getId() {
            return id;
        }
    }

    static class Arbitrary {
    }

    @EntityTypeId("givenParent")
    public static class GivenParent implements AccessMappings, FluentBuilder<GivenParent> {

        @SelfId(resolver = GivenParentService.class)
        @AccessFor(read = AnyCustomerUser.class)
        Long id;

        @Override
        public Long getId() {
            return id;
        }

        public GivenParent id(final long id) {
            this.id = id;
            return this;
        }
    }

    public static class GivenChild implements AccessMappings, FluentBuilder<GivenChild> {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = AnyCustomerUser.class)
        Long id;

        @AccessFor(
                init = CustomerContractualContact.class,
                update = CustomerContractualContact.class,
                read = AnyCustomerUser.class)
        @ParentId(resolver = GivenParentService.class)
        GivenParent parent;

        @AccessFor(
                init = { CustomerTechnicalContact.class, CustomerFinancialContact.class },
                update = {
                        CustomerTechnicalContact.class,
                        CustomerFinancialContact.class })
        String restrictedField;

        @Override
        public Long getId() {
            return id;
        }
    }

    static abstract class GivenParentService implements IdToDtoResolver<GivenParent> {
    }

}
