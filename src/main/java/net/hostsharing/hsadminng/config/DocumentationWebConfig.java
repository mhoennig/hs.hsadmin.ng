package net.hostsharing.hsadminng.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DocumentationWebConfig implements WebMvcConfigurer {

    // Spring resolves a welcome page only for the context root, not for the /doc/ sub-directory
    // of the documentation bundled into the jar.
    @Override
    public void addViewControllers(@NonNull final ViewControllerRegistry registry) {
        registry.addViewController("/doc").setViewName("redirect:/doc/index.html");
        registry.addViewController("/doc/").setViewName("forward:/doc/index.html");
    }

    // Swagger UI resolves the $refs between these files itself, thus they are served as the tree
    // they are written as, rather than bundled into one file per module.
    @Override
    public void addResourceHandlers(@NonNull final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc/api/spec/**")
                .addResourceLocations("classpath:/api-definition/");
    }
}
