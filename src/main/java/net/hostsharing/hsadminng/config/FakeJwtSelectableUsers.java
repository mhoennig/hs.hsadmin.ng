package net.hostsharing.hsadminng.config;

import java.util.List;

/**
 * Supplies the user names selectable in the fake OIDC login form; implemented by the rbac subject
 * repository, so that the config package does not depend on the rbac package (package cycle).
 */
public interface FakeJwtSelectableUsers {

    List<String> findAllActiveUserSubjectNames();
}
