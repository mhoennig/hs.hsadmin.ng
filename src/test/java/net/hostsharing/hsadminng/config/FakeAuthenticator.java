package net.hostsharing.hsadminng.config;

import lombok.SneakyThrows;

import jakarta.servlet.http.HttpServletRequest;

public class FakeAuthenticator implements Authenticator {

    @Override
    @SneakyThrows
    public String authenticate(final HttpServletRequest httpRequest) {
        return httpRequest.getHeader("current-subject");
    }
}
