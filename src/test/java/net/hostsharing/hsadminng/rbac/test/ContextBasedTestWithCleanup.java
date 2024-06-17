package net.hostsharing.hsadminng.rbac.test;

import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantEntity;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantsDiagramService;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import jakarta.persistence.*;
import java.util.*;

import static java.lang.System.out;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;
import static org.assertj.core.api.Assertions.assertThat;

// TODO.impl: cleanup the whole class
public abstract class ContextBasedTestWithCleanup extends ContextBasedTest {

    private static final boolean DETAILED_BUT_SLOW_CHECK = true;
    @PersistenceContext
    protected EntityManager em;

    @Autowired
    RbacGrantRepository rbacGrantRepo;

    @Autowired
    RbacRoleRepository rbacRoleRepo;

    @Autowired
    RbacObjectRepository rbacObjectRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    private TreeMap<UUID, Class<? extends RbacObject>> entitiesToCleanup = new TreeMap<>();

    private static Long latestIntialTestDataSerialId;
    private static boolean countersInitialized = false;
    private static boolean initialTestDataValidated = false;
    private static Long initialRbacObjectCount = null;
    private static Long initialRbacRoleCount = null;
    private static Long initialRbacGrantCount = null;
    private Set<String> initialRbacObjects;
    private Set<String> initialRbacRoles;
    private Set<String> initialRbacGrants;

    private TestInfo testInfo;

    public <T extends RbacObject> T refresh(final T entity) {
        final var merged = em.merge(entity);
        em.refresh(merged);
        return merged;
    }

    public UUID toCleanup(final Class<? extends RbacObject> entityClass, final UUID uuidToCleanup) {
        out.println("toCleanup(" + entityClass.getSimpleName() + ", " + uuidToCleanup + ")");
        entitiesToCleanup.put(uuidToCleanup, entityClass);
        return uuidToCleanup;
    }

    public <E extends RbacObject> E toCleanup(final E entity) {
        out.println("toCleanup(" + entity.getClass() + ", " + entity.getUuid());
        if ( entity.getUuid() == null ) {
            throw new IllegalArgumentException("only persisted entities with valid uuid allowed");
        }
        entitiesToCleanup.put(entity.getUuid(), entity.getClass());
        return entity;
    }

    protected void cleanupAllNew(final Class<? extends RbacObject> entityClass) {
        if (initialRbacObjects == null) {
            out.println("skipping cleanupAllNew: " + entityClass.getSimpleName());
            return; // TODO: seems @AfterEach is called without any @BeforeEach
        }

        out.println("executing cleanupAllNew: " + entityClass.getSimpleName());

        final var tableName = entityClass.getAnnotation(Table.class).name();
        final var rvTableName = tableName.endsWith("_rv")
                ? tableName.substring(0, tableName.length() - "_rv".length())
                : tableName;

        allRbacObjects().stream()
                .filter(o -> o.startsWith(rvTableName + ":"))
                .filter(o -> !initialRbacObjects.contains(o))
                .forEach(o -> {
                    final UUID uuid = UUID.fromString(o.split(":")[1]);

                    final var exception = jpaAttempt.transacted(() -> {
                        context.define("superuser-alex@hostsharing.net", null);
                        em.remove(em.getReference(entityClass, uuid));
                        out.println("DELETING new " + entityClass.getSimpleName() + "#" + uuid + " SUCCEEDED");
                    }).caughtException();

                    if (exception != null) {
                        out.println("DELETING new " + entityClass.getSimpleName() + "#" + uuid + " FAILED: " + exception);
                    }
                });
    }

    @BeforeEach
        //@Transactional -- TODO: check why this does not work but jpaAttempt.transacted does work
    void retrieveInitialTestData(final TestInfo testInfo) {
        out.println(ContextBasedTestWithCleanup.class.getSimpleName() + ".retrieveInitialTestData");

        if (latestIntialTestDataSerialId == null ) {
            latestIntialTestDataSerialId = rbacObjectRepo.findLatestSerialId();
        }

        if (initialRbacObjects != null){
            assertNoNewRbacObjectsRolesAndGrantsLeaked();
        }

        initialTestDataValidated = false;

        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            if (initialRbacObjects == null) {

                initialRbacObjects = allRbacObjects();
                initialRbacRoles = allRbacRoles();
                initialRbacGrants = allRbacGrants();

                initialRbacObjectCount = rbacObjectRepo.count();
                initialRbacRoleCount = rbacRoleRepo.count();
                initialRbacGrantCount = rbacGrantRepo.count();

                countersInitialized = true;
                initialTestDataValidated = true;
            } else {
                initialRbacObjectCount = assumeSameInitialCount(initialRbacObjectCount, rbacObjectRepo.count(), "business objects");
                initialRbacRoleCount = assumeSameInitialCount(initialRbacRoleCount, rbacRoleRepo.count(), "rbac roles");
                initialRbacGrantCount = assumeSameInitialCount(initialRbacGrantCount, rbacGrantRepo.count(), "rbac grants");
                initialTestDataValidated = true;
            }
        }).reThrowException();

        assertThat(countersInitialized).as("error while retrieving initial test data").isTrue();
        assertThat(initialTestDataValidated).as("check previous test for leaked test data").isTrue();

        out.println("TOTAL OBJECT COUNT (before): " + initialRbacObjectCount);
    }

    private Long assumeSameInitialCount(final Long countBefore, final long currentCount, final String name) {
        assertThat(currentCount)
                .as("not all " + name + " got cleaned up by the previous tests")
                .isEqualTo(countBefore);
        return currentCount;
    }

    @BeforeEach
    void keepTestInfo(final TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @AfterEach
    void cleanupAndCheckCleanup(final TestInfo testInfo) {
        out.println(ContextBasedTestWithCleanup.class.getSimpleName() + ".cleanupAndCheckCleanup");
        cleanupTemporaryTestData();
        deleteLeakedRbacObjects();
        long rbacObjectCount = assertNoNewRbacObjectsRolesAndGrantsLeaked();

        out.println("TOTAL OBJECT COUNT (after): " + rbacObjectCount);
    }

    private void cleanupTemporaryTestData() {
        // For better performance in a single transaction ...
        final var exception = jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            entitiesToCleanup.reversed().forEach((uuid, entityClass) -> {
                final var rvTableName = entityClass.getAnnotation(Table.class).name();
                if ( !rvTableName.endsWith("_rv") ) {
                    throw new IllegalStateException();
                }
                final var rawTableName = rvTableName.substring(0, rvTableName.length() - "_rv".length());
                final var deletedRows = em.createNativeQuery("DELETE FROM " + rawTableName + " WHERE uuid=:uuid")
                        .setParameter("uuid", uuid).executeUpdate();
                out.println("DELETING temporary " + entityClass.getSimpleName() + "#" + uuid + " deleted " + deletedRows + " rows");
            });
        }).caughtException();

        // ... and in case of foreign key violations, we rely on the RbacObject cleanup.
        if (exception != null) {
            System.err.println(exception);
        }
    }

    private long assertNoNewRbacObjectsRolesAndGrantsLeaked() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            assertEqual(initialRbacObjects, allRbacObjects());
            if (DETAILED_BUT_SLOW_CHECK) {
                assertEqual(initialRbacRoles, allRbacRoles());
                assertEqual(initialRbacGrants, allRbacGrants());
            }

            // The detailed check works with sets, thus it cannot determine duplicates.
            // Therefore, we always compare the counts as well.
            long rbacObjectCount = 0;
            assertThat(rbacObjectCount = rbacObjectRepo.count()).as("not all business objects got cleaned up (by current test)")
                    .isEqualTo(initialRbacObjectCount);
            assertThat(rbacRoleRepo.count()).as("not all rbac roles got cleaned up (by current test)")
                    .isEqualTo(initialRbacRoleCount);
            assertThat(rbacGrantRepo.count()).as("not all rbac grants got cleaned up (by current test)")
                    .isEqualTo(initialRbacGrantCount);
            return rbacObjectCount;
        }).assertSuccessful().returnedValue();
    }

    private void deleteLeakedRbacObjects() {
        rbacObjectRepo.findAll().stream()
            .filter(o -> o.serialId > latestIntialTestDataSerialId)
            .sorted(comparing(o -> o.serialId))
            .forEach(o -> {
                final var exception = jpaAttempt.transacted(() -> {
                    context.define("superuser-alex@hostsharing.net", null);

                    em.createNativeQuery("DELETE FROM " + o.objectTable + " WHERE uuid=:uuid")
                            .setParameter("uuid", o.uuid)
                            .executeUpdate();

                    out.println("DELETING leaked " + o.objectTable + "#" + o.uuid + " SUCCEEDED");
                }).caughtException();

                if (exception != null) {
                    out.println("DELETING leaked " + o.objectTable + "#" + o.uuid + " FAILED " + exception);
                }
            });
    }

    private void assertEqual(final Set<String> before, final Set<String> after) {
        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        assertThat(difference(before, after)).as("missing entities (deleted initial test data)").isEmpty();
        assertThat(difference(after, before)).as("spurious entities (test data not cleaned up by this test)").isEmpty();
    }

    @NotNull
    private Set<String> allRbacGrants() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            return rbacGrantRepo.findAll().stream()
                    .map(RbacGrantEntity::toDisplay)
                    .collect(toSet());
        }).assertSuccessful().returnedValue();
    }

    @NotNull
    private Set<String> allRbacRoles() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            return rbacRoleRepo.findAll().stream()
                    .map(RbacRoleEntity::getRoleName)
                    .collect(toSet());
        }).assertSuccessful().returnedValue();
    }

    @NotNull
    private Set<String> allRbacObjects() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            return rbacObjectRepo.findAll().stream()
                    .map(RbacObjectEntity::toString)
                    .collect(toSet());
        }).assertSuccessful().returnedValue();
    }

    /**
     * Generates a diagram of the RBAC-Grants to the current subjects (user or assumed roles).
     */
    protected void generateRbacDiagramForCurrentSubjects(final EnumSet<RbacGrantsDiagramService.Include> include, final String name) {
        RbacGrantsDiagramService.writeToFile(
                name,
                diagramService.allGrantsToCurrentUser(include),
                "doc/temp/" + name + ".md"
        );
    }

    /**
     * Generates a diagram of the RBAC-Grants for the given object and permission.
     */
    protected void generateRbacDiagramForObjectPermission(final UUID targetObject, final String rbacOp, final String name) {
        RbacGrantsDiagramService.writeToFile(
                name,
                diagramService.allGrantsFrom(targetObject, rbacOp, RbacGrantsDiagramService.Include.ALL),
                "doc/temp/" + name + ".md"
        );
    }
}

interface RbacObjectRepository extends Repository<RbacObjectEntity, UUID> {

    long count();

    List<RbacObjectEntity> findAll();

    @Query("SELECT max(r.serialId) FROM RbacObjectEntity r")
    Long findLatestSerialId();
}

@Entity
@Table(name = "rbacobject")
class RbacObjectEntity {

    @Id
    @GeneratedValue
    UUID uuid;

    @Column(name = "serialid")
    long serialId;

    @Column(name = "objecttable")
    String objectTable;

    @Override
    public String toString() {
        return objectTable + ":" + uuid + ":" + serialId;
    }
}
