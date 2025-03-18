package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class RealCasAuthenticator implements CasAuthenticator {

    @Value("${hsadminng.cas.server}")
    private String casServerUrl;

    @Value("${hsadminng.cas.service}")
    private String serviceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SneakyThrows
    @Timed("app.cas.authenticate")
    public String authenticate(final HttpServletRequest httpRequest) {
        final var ticket = httpRequest.getHeader("authorization").replaceAll("^Bearer ", "");
        final var serviceTicket = ticket.startsWith("TGT-")
                ? fetchServiceTicket(ticket)
                : ticket;
        final var userName = extractUserName(verifyServiceTicket(serviceTicket));
        System.err.println("CAS-user: " + userName);
        return userName;
    }

    private String fetchServiceTicket(final String ticketGrantingTicket) {
        final var tgtUrl = casServerUrl + "/cas/v1/tickets/" + ticketGrantingTicket;

        final var restTemplate = new RestTemplate();
        final var formData = new LinkedMultiValueMap<String, String>();
        formData.add("service", serviceUrl);

        return restTemplate.postForObject(tgtUrl, formData, String.class);
    }

    private Document verifyServiceTicket(final String serviceTicket) throws SAXException, IOException, ParserConfigurationException {
        if ( !serviceTicket.startsWith("ST-") ) {
            throwBadCredentialsException("Invalid authorization ticket");
        }

        final var url = casServerUrl + "/cas/p3/serviceValidate" +
                "?service=" + serviceUrl +
                "&ticket=" + serviceTicket;

        final var response = restTemplate.getForObject(url, String.class);

        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(response.getBytes()));

    }

    private String extractUserName(final Document verification) {

        if (verification.getElementsByTagName("cas:authenticationSuccess").getLength() == 0) {
            System.err.println("CAS service ticket could not be validated");
            System.err.println(verification);
            throwBadCredentialsException("CAS service ticket could not be validated");
        }
        return verification.getElementsByTagName("cas:user").item(0).getTextContent();
    }

    private void throwBadCredentialsException(final String message) {
        throw new BadCredentialsException(message);
    }
}
