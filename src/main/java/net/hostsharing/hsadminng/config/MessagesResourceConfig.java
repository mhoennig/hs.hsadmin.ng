package net.hostsharing.hsadminng.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessagesResourceConfig {
    @Bean
    public ResourceBundleMessageSource messageSource() {
        final var source = new ResourceBundleMessageSource();
        source.setBasenames("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

}
