package net.hostsharing.hsadminng.config;

import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JsonObjectMapperConfiguration {

    public static JsonMapper build() {
        return new JsonObjectMapperConfiguration().customObjectMapper();
    }

    @Bean
    @Primary
    public JsonMapper customObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JsonNullableJackson3Module())
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
    }
}
