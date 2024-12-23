package net.hostsharing.hsadminng.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("wiremock")
public class WireMockConfig {

    private static final WireMockServer wireMockServer = new WireMockServer(8088);

    @Bean
    public WireMockServer wireMockServer() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
        return wireMockServer;
    }
}
