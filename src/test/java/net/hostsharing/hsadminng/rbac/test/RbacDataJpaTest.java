package net.hostsharing.hsadminng.rbac.test;

import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The technical setup shared by all RBAC-aware repository tests.
 *
 * <p>Spring caches one application context per merged configuration, thus each deviating
 * combination of imports and bean overrides would cost another context.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import({ Context.class, JpaAttempt.class, RbacGrantsDiagramService.class })
@MockitoBean(types = HttpServletRequest.class)
public @interface RbacDataJpaTest {
}
