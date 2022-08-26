package net.hostsharing.hsadminng.context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Service
public class Context {

    @PersistenceContext
    private EntityManager em;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Transactional(propagation = MANDATORY)
    public void register(final String currentUser, final String assumedRoles) {
        if (request != null) {
            setCurrentTask(request.getMethod() + " " + request.getRequestURI());
        } else {

            final Optional<StackWalker.StackFrame> caller =
                    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .walk(frames ->
                                    frames.skip(1)
                                            .filter(c -> c.getDeclaringClass()
                                                    .getPackageName()
                                                    .startsWith("net.hostsharing.hsadminng"))
                                            .filter(c -> !c.getDeclaringClass().getName().contains("BySpringCGLIB$$"))
                                            .findFirst());
            final var callerName = caller.map(
                            c -> c.getDeclaringClass().getSimpleName() + "." + c.getMethodName())
                    .orElse("unknown");
            setCurrentTask(callerName);
        }
        setCurrentUser(currentUser);
        if (!StringUtils.isBlank(assumedRoles)) {
            assumeRoles(assumedRoles);
        }
    }

    @Transactional(propagation = MANDATORY)
    public void setCurrentTask(final String task) {
        final var sql = String.format(
                "set local hsadminng.currentTask = '%s';",
                shortenToMaxLength(task, 95)
        );
        em.createNativeQuery(sql).executeUpdate();
    }

    public String getCurrentTask() {
        return (String) em.createNativeQuery("select current_setting('hsadminng.currentTask');").getSingleResult();
    }

    @Transactional(propagation = MANDATORY)
    public void setCurrentUser(final String userName) {
        em.createNativeQuery(
                String.format(
                        "set local hsadminng.currentUser = '%s';",
                        userName
                )
        ).executeUpdate();
        assumeNoSpecialRole();
    }

    public String getCurrentUser() {
        return String.valueOf(em.createNativeQuery("select currentUser()").getSingleResult());
    }

    @Transactional(propagation = MANDATORY)
    public void assumeRoles(final String roles) {
        em.createNativeQuery(
                String.format(
                        "set local hsadminng.assumedRoles = '%s';",
                        roles
                )
        ).executeUpdate();
    }

    @Transactional(propagation = MANDATORY)
    public void assumeNoSpecialRole() {
        em.createNativeQuery(
                "set local hsadminng.assumedRoles = '';"
        ).executeUpdate();
    }

    public String[] getAssumedRoles() {
        return (String[]) em.createNativeQuery("select assumedRoles()").getSingleResult();
    }

    private static String shortenToMaxLength(final String task, final int maxLength) {
        return task.substring(0, Math.min(task.length(), maxLength));
    }
}
