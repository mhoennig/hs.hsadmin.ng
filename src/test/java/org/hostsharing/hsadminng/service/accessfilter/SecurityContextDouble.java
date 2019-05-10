// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assumptions.assumeThat;

import org.hostsharing.hsadminng.security.SecurityUtils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;

abstract class SecurityContextDouble<T extends SecurityContextDouble> {

    private final Collection<GrantedAuthority> authorities = new ArrayList<>();

    protected SecurityContextDouble() {
    }

    protected SecurityContextDouble withAuthenticatedUser(final String login) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(login, "dummyPassword") {

            @Override
            public Collection<GrantedAuthority> getAuthorities() {
                return authorities;
            }
        });
        SecurityContextHolder.setContext(securityContext);
        assumeThat(SecurityUtils.getCurrentUserLogin()).hasValue(login);
        return this;
    }

    public T withAuthority(final String authority) {
        authorities.add((GrantedAuthority) () -> authority);
        return (T) this;
    }

    private static class FakePrincipal {

        private final String username;

        public FakePrincipal(final String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return username;
        }
    }

}
