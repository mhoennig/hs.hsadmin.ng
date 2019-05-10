// Licensed under Apache-2.0
package org.hostsharing.hsadminng.config;

import io.github.jhipster.config.JHipsterProperties;

import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.Cache.Ehcache ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        Object.class,
                        Object.class,
                        ResourcePoolsBuilder.heap(ehcache.getMaxEntries()))
                        .withExpiry(
                                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                        .build());
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            cm.createCache(org.hostsharing.hsadminng.repository.UserRepository.USERS_BY_LOGIN_CACHE, jcacheConfiguration);
            cm.createCache(org.hostsharing.hsadminng.repository.UserRepository.USERS_BY_EMAIL_CACHE, jcacheConfiguration);
            // jhipster-needle-ehcache-add-entry

            cm.createCache(
                    org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository.CURRENT_USER_ROLE_ASSIGNMENTS_CACHE,
                    jcacheConfiguration);
        };
    }
}
