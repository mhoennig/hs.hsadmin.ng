package net.hostsharing.hsadminng.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class EntityManagerWrapperFakeConfiguration {

    @Bean
    public EntityManagerWrapperFake entityManagerWrapperFake() {
        return new EntityManagerWrapperFake();
    }
}
