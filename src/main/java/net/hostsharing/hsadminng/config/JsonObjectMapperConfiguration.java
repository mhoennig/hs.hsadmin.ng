package net.hostsharing.hsadminng.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;


@Configuration
public class JsonObjectMapperConfiguration {

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder customObjectMapper() {
        // HOWTO: add JSON converters and specify other JSON mapping configurations
        return new Jackson2ObjectMapperBuilder()
                .modules(new JsonNullableModule(), new JavaTimeModule())
                .featuresToEnable(
                        JsonParser.Feature.ALLOW_COMMENTS,
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
                )
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
