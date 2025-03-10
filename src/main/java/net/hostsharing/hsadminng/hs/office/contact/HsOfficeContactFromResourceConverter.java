package net.hostsharing.hsadminng.hs.office.contact;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactInsertResource;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.stereotype.Component;

import static net.hostsharing.hsadminng.mapper.KeyValueMap.from;

// HOWTO: implement a ModelMapper converter which converts from a (JSON) resource instance to a generic entity instance (RBAC vs. REAL)
@Component
public class HsOfficeContactFromResourceConverter<E extends HsOfficeContact>
        implements Converter<HsOfficeContactInsertResource, E> {

    @Override
    @SneakyThrows
    public E convert(final MappingContext<HsOfficeContactInsertResource, E> context) {
        final var resource = context.getSource();
        final var entity = context.getDestinationType().getDeclaredConstructor().newInstance();
        entity.setCaption(resource.getCaption());
        entity.putPostalAddress(from(resource.getPostalAddress()));
        entity.putEmailAddresses(from(resource.getEmailAddresses()));
        entity.putPhoneNumbers(from(resource.getPhoneNumbers()));
        return entity;
    }
}
