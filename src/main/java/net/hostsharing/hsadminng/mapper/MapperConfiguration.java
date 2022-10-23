package net.hostsharing.hsadminng.mapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

    @Bean
    public Mapper modelMapper() {
        return new Mapper();
    }
}
