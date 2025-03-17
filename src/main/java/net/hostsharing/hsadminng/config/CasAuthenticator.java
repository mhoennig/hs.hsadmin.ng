package net.hostsharing.hsadminng.config;

import jakarta.servlet.http.HttpServletRequest;

public interface CasAuthenticator {

    String authenticate(final HttpServletRequest httpRequest);
}
