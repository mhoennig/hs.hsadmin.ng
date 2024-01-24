package net.hostsharing.hsadminng.hs.office.person;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class HsOfficePersonTypeConverterUnitTest {

    final HsOfficePersonTypeConverter converter = new HsOfficePersonTypeConverter();

    @ParameterizedTest
    @EnumSource(HsOfficePersonType.class)
    void mapsToDatabaseValue(final HsOfficePersonType given) {
        assertThat(converter.convertToDatabaseColumn(given)).isEqualTo(given.shortName);
    }


    @ParameterizedTest
    @EnumSource(HsOfficePersonType.class)
    void mapsFromDatabaseValue(final HsOfficePersonType given) {
        assertThat(converter.convertToEntityAttribute(given.shortName)).isEqualTo(given);
    }
}
