package net.hostsharing.hsadminng.hs.office.person;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class HsOfficePersonTypeConverter implements AttributeConverter<HsOfficePersonType, String> {

    @Override
    public String convertToDatabaseColumn(HsOfficePersonType category) {
        if (category == null) {
            return null;
        }
        return category.shortName;
    }

    @Override
    public HsOfficePersonType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }

        return Stream.of(HsOfficePersonType.values())
                .filter(c -> c.shortName.equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
