package net.hostsharing.hsadminng.config;

import jakarta.servlet.http.HttpServletRequest;

public interface Authenticator {

    String authenticate(final HttpServletRequest httpRequest);
}
