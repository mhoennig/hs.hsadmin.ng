package net.hostsharing.hsadminng.config;

import lombok.SneakyThrows;

import jakarta.servlet.http.HttpServletRequest;

public class FakeCasAuthenticator implements CasAuthenticator {

    @Override
    @SneakyThrows
    public String authenticate(final HttpServletRequest httpRequest) {
        return httpRequest.getHeader("Authorization").replaceAll("^Bearer ", "");
    }
}
