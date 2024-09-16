package net.hostsharing.hsadminng.rbac.test;

import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.object.BaseEntity;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantEntity;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.lang.System.out;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;
import static org.assertj.core.api.Assertions.assertThat;

// TODO.impl: cleanup the whole class
public abstract class ContextBasedTestWithCleanup extends ContextBasedTest {

    private static final boolean DETAILED_BUT_SLOW_CHECK = false;

    @PersistenceContext
    protected EntityManager em;

    @Autowired
    private PlatformTransactionManager tm;

    @Autowired
    RbacGrantRepository rbacGrantRepo;

    @Autowired
    RbacRoleRepository rbacRoleRepo;

    @Autowired
    RbacObjectRepository rbacObjectRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    private TreeMap<UUID, Class<? extends BaseEntity>> entitiesToCleanup = new TreeMap<>();

    private static Long latestIntialTestDataSerialId;
    private static boolean countersInitialized = false;
    private static boolean initialTestDataValidated = false;
    static private Long previousRbacObjectCount;
    private Long initialRbacObjectCount = null;
    private Long initialRbacRoleCount = null;
    private Long initialRbacGrantCount = null;
    private Set<String> initialRbacObjects;
    private Set<String> initialRbacRoles;
    private Set<String> initialRbacGrants;

    private TestInfo testInfo;

    public <T extends BaseEntity> T refresh(final T entity) {
        final var merged = em.merge(entity);
        em.refresh(merged);
        return merged;
    }

    public UUID toCleanup(final Class<? extends BaseEntity> entityClass, final UUID uuidToCleanup) {
        out.println("toCleanup(" + entityClass.getSimpleName() + ", " + uuidToCleanup + ")");
        entitiesToCleanup.put(uuidToCleanup, entityClass);
        return uuidToCleanup;
    }

    public <E extends BaseEntity> E toCleanup(final E entity) {
        out.println("toCleanup(" + entity.getClass() + ", " + entity.getUuid());
        if ( entity.getUuid() == null ) {
            throw new IllegalArgumentException("only persisted entities with valid uuid allowed");
        }
        entitiesToCleanup.put(entity.getUuid(), entity.getClass());
        return entity;
    }

    protected void cleanupAllNew(final Class<? extends BaseEntity> entityClass) {
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
        this.testInfo = testInfo;
        out.println(ContextBasedTestWithCleanup.class.getSimpleName() + ".retrieveInitialTestData");

        if (latestIntialTestDataSerialId == null ) {
            latestIntialTestDataSerialId = rbacObjectRepo.findLatestSerialId();
        }

        if (initialRbacObjects != null){
            assertNoNewRbacObjectsRolesAndGrantsLeaked("before");
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

        out.println(testInfo.getDisplayName() + ": TOTAL OBJECT COUNT (initial): " + previousRbacObjectCount + " -> " + initialRbacObjectCount);
        if (previousRbacObjectCount != null) {
            assertThat(initialRbacObjectCount).as("TOTAL OBJECT COUNT changed from " + previousRbacObjectCount + " to " + initialRbacObjectCount).isEqualTo(previousRbacObjectCount);
        }
    }

    private Long assumeSameInitialCount(final Long countBefore, final long currentCount, final String name) {
        assertThat(currentCount)
                .as("not all " + name + " got cleaned up by the previous tests")
                .isEqualTo(countBefore);
        return currentCount;
    }

    @AfterEach
    void cleanupAndCheckCleanup(final TestInfo testInfo) {
        this.testInfo = testInfo;

        out.println(ContextBasedTestWithCleanup.class.getSimpleName() + ".cleanupAndCheckCleanup");
        cleanupTemporaryTestData();
        repeatUntilTrue(3, this::deleteLeakedRbacObjects);

        assertNoNewRbacObjectsRolesAndGrantsLeaked("after");
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

        // ... and in case of foreign key violations, we rely on the rbac.object cleanup.
        if (exception != null) {
            System.err.println(exception);
        }
    }

    private void assertNoNewRbacObjectsRolesAndGrantsLeaked(final String event) {
        long rbacObjectCount = jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            assertEqual(initialRbacObjects, allRbacObjects());
            if (DETAILED_BUT_SLOW_CHECK) {
                assertEqual(initialRbacRoles, allRbacRoles());
                assertEqual(initialRbacGrants, allRbacGrants());
            }

            // The detailed check works with sets, thus it cannot determine duplicates.
            // Therefore, we always compare the counts as well.
            long count = rbacObjectRepo.count();
            out.println(testInfo.getDisplayName() + ": TOTAL OBJECT COUNT (" + event+ "): " + previousRbacObjectCount+  " -> " + count);
            assertThat(count).as("not all business objects got cleaned up (by current test)")
                    .isEqualTo(initialRbacObjectCount);
            assertThat(rbacRoleRepo.count()).as("not all rbac roles got cleaned up (by current test)")
                    .isEqualTo(initialRbacRoleCount);
            assertThat(rbacGrantRepo.count()).as("not all rbac grants got cleaned up (by current test)")
                    .isEqualTo(initialRbacGrantCount);
            return count;
        }).assertSuccessful().returnedValue();

        if (previousRbacObjectCount != null) {
            assertThat(rbacObjectCount).as("TOTAL OBJECT COUNT changed from " + previousRbacObjectCount + " to " + rbacObjectCount).isEqualTo(previousRbacObjectCount);
        }
        previousRbacObjectCount = rbacObjectCount;
    }

    private boolean deleteLeakedRbacObjects() {
        final var deletionSuccessful = new AtomicBoolean(true);
        jpaAttempt.transacted(() -> rbacObjectRepo.findAll()).assertSuccessful().returnedValue().stream()
            .filter(o -> latestIntialTestDataSerialId != null && o.serialId > latestIntialTestDataSerialId)
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
                    deletionSuccessful.set(false);
                }
            });
        return deletionSuccessful.get();
    }

    private void assertEqual(final Set<String> before, final Set<String> after) {
        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        final SetUtils.SetView<String> difference = difference(before, after);
        assertThat(difference).as("missing entities (deleted initial test data)").isEmpty();
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
     * @return an array of all RBAC roles matching the given pattern
     *
     * Usually unused, but for temporary debugging purposes of findind role names for new tests.
     */
    @SuppressWarnings("unused")
    protected String[] roleNames(final String sqlLikeExpression) {
        final var pattern = Pattern.compile(sqlLikeExpression);
        //noinspection unchecked
        final List<Object[]> rows = (List<Object[]>) em.createNativeQuery("select * from rbac.role_ev where roleidname like 'hs_booking_project#%'")
                .getResultList();
        return rows.stream()
                .map(row -> (row[0]).toString())
                .filter(roleName -> pattern.matcher(roleName).matches())
                .toArray(String[]::new);
    }

    /**
     * Generates a diagram of the RBAC-Grants to the current subjects (user or assumed roles).
     *
     * Usually unused, but for temporary use for debugging and other analysis.
     */
    @SuppressWarnings("unused")
    protected void generateRbacDiagramForCurrentSubjects(final EnumSet<RbacGrantsDiagramService.Include> include, final String name) {
        RbacGrantsDiagramService.writeToFile(
                name,
                diagramService.allGrantsTocurrentSubject(include),
                "doc/temp/" + name + ".md"
        );
    }

    /**
     * Generates a diagram of the RBAC-Grants for the given object and permission.
     *
     * Usually unused, but for temporary use for debugging and other analysis.
     */
    @SuppressWarnings("unused")
    protected void generateRbacDiagramForObjectPermission(final UUID targetObject, final String rbacOp, final String name) {
        RbacGrantsDiagramService.writeToFile(
                name,
                diagramService.allGrantsFrom(targetObject, rbacOp, RbacGrantsDiagramService.Include.ALL),
                "doc/temp/" + name + ".md"
        );
    }

    public static boolean repeatUntilTrue(int maxAttempts, Supplier<Boolean> method) {
        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            if (method.get()) {
                return true;
            }
        }
        return false;
    }
}

interface RbacObjectRepository extends Repository<RbacObjectEntity, UUID> {

    long count();

    List<RbacObjectEntity> findAll();

    @Query("SELECT max(r.serialId) FROM RbacObjectEntity r")
    Long findLatestSerialId();
}

@Entity
@Table(schema ="rbac", name = "object")
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
