package net.hostsharing.hsadminng.config;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unitTest")
class JsonObjectMapperConfigurationUnitTest {

    @Test
    void acceptsCommentsAndPreservesDecimalPrecision() throws Exception {
        val mapper = JsonObjectMapperConfiguration.build();
        val values = mapper.readValue("/* comment */ {\"amount\": 1.234567890123456789}",
                new TypeReference<Map<String, Object>>() {});

        assertThat(values.get("amount")).isEqualTo(new BigDecimal("1.234567890123456789"));
    }

    @Test
    void rejectsUnknownProperties() {
        val mapper = JsonObjectMapperConfiguration.build();

        assertThatThrownBy(() -> mapper.readValue("{\"unknown\": true}", Payload.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void distinguishesUndefinedNullAndPresentPatchValues() throws Exception {
        val mapper = JsonObjectMapperConfiguration.build();

        assertThat(mapper.readValue("{}", Payload.class).label).isEqualTo(JsonNullable.undefined());
        assertThat(mapper.readValue("{\"label\": null}", Payload.class).label).isEqualTo(JsonNullable.of(null));
        assertThat(mapper.readValue("{\"label\": \"value\"}", Payload.class).label).isEqualTo(JsonNullable.of("value"));
    }

    @Test
    void serializesPatchValuesWithoutExposingTheNullableWrapper() {
        val mapper = JsonObjectMapperConfiguration.build();
        val payload = new Payload();

        assertThat(mapper.readTree(mapper.writeValueAsString(payload)).has("label")).isFalse();
        payload.label = JsonNullable.of(null);
        assertThat(mapper.readTree(mapper.writeValueAsString(payload)).get("label").isNull()).isTrue();
        payload.label = JsonNullable.of("value");
        assertThat(mapper.readTree(mapper.writeValueAsString(payload)).get("label").asString()).isEqualTo("value");
    }

    @Test
    void usesIsoDatesAndExcludesUnannotatedPropertiesFromViews() throws Exception {
        val mapper = JsonObjectMapperConfiguration.build();
        val payload = mapper.readValue("{\"date\": \"2026-09-25\", \"label\": \"hidden\"}", Payload.class);

        assertThat(payload.date).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(mapper.writerWithView(PublicView.class).writeValueAsString(payload))
                .isEqualTo("{\"date\":\"2026-09-25\"}");
    }

    private interface PublicView {
    }

    public static class Payload {
        @JsonView(PublicView.class)
        public LocalDate date;
        public JsonNullable<String> label = JsonNullable.undefined();
    }
}
