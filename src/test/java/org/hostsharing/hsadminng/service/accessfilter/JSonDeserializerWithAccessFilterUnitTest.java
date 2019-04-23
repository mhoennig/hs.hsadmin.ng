package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
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
import static org.mockito.BDDMockito.given;

@SuppressWarnings("ALL")
public class JSonDeserializerWithAccessFilterUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public JsonParser jsonParser;

    @Mock
    public ObjectCodec codec;

    @Mock
    public TreeNode treeNode;

    @Before
    public void init() {
        givenLoginUserWithRole(Role.ANY_CUSTOMER_USER);

        given(jsonParser.getCodec()).willReturn(codec);
    }

    @Test
    public void shouldDeserializeStringField() throws IOException {
        // given
        givenJSonTree(asJSon(ImmutablePair.of("openStringField", "String Value")));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openStringField).isEqualTo("String Value");
    }

    @Test
    public void shouldDeserializeIntegerField() throws IOException {
        // given
        givenJSonTree(asJSon(ImmutablePair.of("openIntegerField", 1234)));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openIntegerField).isEqualTo(1234);
    }

    @Test
    public void shouldDeserializeLongField() throws IOException {
        // given
        givenJSonTree(asJSon(ImmutablePair.of("openLongField", 1234L)));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openLongField).isEqualTo(1234L);
    }

    @Test
    public void shouldDeserializeStringFieldIfRequiredRoleIsCoveredByUser() throws IOException {
        // given
        givenLoginUserWithRole(Role.FINANCIAL_CONTACT);
        givenJSonTree(asJSon(ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedField).isEqualTo("Restricted String Value");
    }

    @Test
    public void shouldInitializeFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenLoginUserWithRole(Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("initializationProhibited");
        });
    }

    @Test
    public void shouldUpdateFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenLoginUserWithRole(Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("updateProhibited");
        });
    }

    @Test
    public void should() throws IOException {
        // given
        givenJSonTree(asJSon(ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(jsonParser, null, GivenDtoWithMultipleSelfId.class).deserialize());

        // then
        assertThat(exception).isInstanceOf(AssertionError.class).hasMessageContaining("xx");
    }

    // --- only fixture code below ---

    @SafeVarargs
    private final String asJSon(final ImmutablePair<String, Object>... properties) {
        final StringBuilder json = new StringBuilder();
        for (ImmutablePair<String, Object> prop : properties) {
            json.append(inQuotes(prop.left));
            json.append(": ");
            if (prop.right instanceof Number) {
                json.append(prop.right);
            } else {
                json.append(inQuotes(prop.right));
            }
            json.append(",\n");
        }
        return "{\n" + json.substring(0, json.length() - 2) + "\n}";
    }

    private void givenJSonTree(String givenJSon) throws IOException {
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

    private String inQuotes(Object value) {
        return "\"" + value.toString() + "\"";
    }

    public static class GivenDto {

        @SelfId
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @AccessFor(init = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT}, update = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        String restrictedField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        String openStringField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        Integer openIntegerField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        Long openLongField;
    }


    public static class GivenDtoWithMultipleSelfId {

        @SelfId
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @SelfId
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id2;

    }
}
