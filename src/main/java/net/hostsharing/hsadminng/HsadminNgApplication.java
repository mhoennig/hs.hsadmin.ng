package net.hostsharing.hsadminng;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@OpenAPIDefinition
public class HsadminNgApplication {

    public static void main(String[] args) {
        SpringApplication.run(HsadminNgApplication.class, args);
    }

    @Bean
        public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {

               @Override
               public void addCorsMappings(CorsRegistry registry) {
            	   // TODO: to enable testing, we should use Spring config
                   String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
                   if (allowedOrigins == null || allowedOrigins.length() <= 1) {
                       allowedOrigins = "*";
                   }
                   registry.addMapping("/api/**").allowedOrigins(allowedOrigins);
               }
           };
       }

}
