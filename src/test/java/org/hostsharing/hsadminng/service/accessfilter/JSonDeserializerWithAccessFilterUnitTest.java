package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenLoginUserWithRole;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JSonDeserializerWithAccessFilterUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public JsonGenerator jsonGenerator;

    @Before
    public void init() {
        givenLoginUserWithRole(Role.ANY_CUSTOMER_USER);
    }

    @Test
    public void shouldDeserializeStringField() throws IOException {
        // given
        final String givenJSon = asJSon(ImmutablePair.of("stringField", "String Value"));

        // when
        new JSonDeserializerWithAccessFilter<C>().deserialize(givenJSon, jsonGenerator, null);

        // then
        verify(jsonGenerator).writeStringField("openStringField", givenDTO.openStringField);
    }

    @Test
    public void shouldSerializeRestrictedFieldIfRequiredRoleIsCoveredByUser() throws IOException {

        // given
        givenLoginUserWithRole(Role.FINANCIAL_CONTACT);

        // when
        new JSonSerializerWithAccessFilter().serialize(givenDTO, jsonGenerator, null);

        // then
        verify(jsonGenerator).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldNotSerializeRestrictedFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {

        // given
        givenLoginUserWithRole(Role.ANY_CUSTOMER_USER);

        // when
        new JSonSerializerWithAccessFilter().serialize(givenDTO, jsonGenerator, null);

        // then
        verify(jsonGenerator, never()).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldThrowExceptionForUnimplementedFieldType() throws IOException {

        // given
        class Arbitrary {
        }
        class GivenDtoWithUnimplementedFieldType {
            @AccessFor(read = Role.ANYBODY)
            Arbitrary fieldWithUnimplementedType;
        }
        final GivenDtoWithUnimplementedFieldType givenDto = new GivenDtoWithUnimplementedFieldType();

        // when
        Throwable actual = catchThrowable(() -> new JSonSerializerWithAccessFilter().serialize(givenDto, jsonGenerator, null));

        // then
        assertThat(actual).isInstanceOf(NotImplementedException.class);
    }

    // --- fixture code below ---

    private String asJSon(final ImmutablePair<String, ? extends Object>... properties) {
        final StringBuilder json = new StringBuilder();
        for ( ImmutablePair<String, ? extends Object> prop: properties ) {
            json.append(inQuotes(prop.left));
            json.append(": ");
            if ( prop.right instanceof Number ) {
                json.append(prop.right);
            } else {
                json.append(inQuotes(prop.right));
            }
            json.append(",\n");
        }
        return "{\n" + json.substring(0, json.length()-2) + "\n}";
    }

    private String inQuotes(Object value) {
        return "\"" + value.toString() + "\"";
    }

    private static class GivenDto {
        @AccessFor(update = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        String restrictedField;

        @AccessFor(update = Role.ANYBODY)
        String openStringField;

        @AccessFor(update = Role.ANYBODY)
        Integer openIntegerField;

        @AccessFor(update = Role.ANYBODY)
        Long openLongField;
    }
}
