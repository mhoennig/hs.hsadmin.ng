package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class CasAuthenticator implements Authenticator {

    @Value("${hsadminng.cas.server}")
    private String casServerUrl;

    @Value("${hsadminng.cas.service}")
    private String serviceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SneakyThrows
    @Timed("app.cas.authenticate")
    public String authenticate(final HttpServletRequest httpRequest) {
        final var userName = StringUtils.isBlank(casServerUrl)
                ? bypassCurrentSubject(httpRequest)
                : casValidation(httpRequest);
        final var authentication = new UsernamePasswordAuthenticationToken(userName, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication.getName();
    }

    private static String bypassCurrentSubject(final HttpServletRequest httpRequest) {
        final var userName = httpRequest.getHeader("current-subject");
        System.err.println("CasAuthenticator.bypassCurrentSubject: " + userName);
        return userName;
    }

    private String casValidation(final HttpServletRequest httpRequest)
            throws SAXException, IOException, ParserConfigurationException {

        final var ticket = httpRequest.getHeader("Authorization");
        final var url = casServerUrl + "/p3/serviceValidate" +
                "?service=" + serviceUrl +
                "&ticket=" + ticket;

        System.err.println("CasAuthenticator.casValidation using URL: " + url);

        final var response = restTemplate.getForObject(url, String.class);

        final var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(response.getBytes()));
        if (doc.getElementsByTagName("cas:authenticationSuccess").getLength() == 0) {
            // TODO.impl: for unknown reasons, this results in a 403 FORBIDDEN
            // throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CAS service ticket could not be validated");
            System.err.println("CAS service ticket could not be validated");
            System.err.println("CAS-validation-URL: " + url);
            System.err.println(response);
            throw new BadCredentialsException("CAS service ticket could not be validated");
        }
        final var userName = doc.getElementsByTagName("cas:user").item(0).getTextContent();
        System.err.println("CAS-user: " + userName);
        return userName;
    }
}
