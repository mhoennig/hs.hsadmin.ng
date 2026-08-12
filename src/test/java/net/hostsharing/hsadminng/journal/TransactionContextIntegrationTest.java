package net.hostsharing.hsadminng.journal;

import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerEntity;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NEVER;

@RbacDataJpaTest
@Tag("generalIntegrationTest")
class TransactionContextIntegrationTest extends ContextBasedTestWithCleanup {

    // one writer per connection of the test datasource, see maximum-pool-size in application.yml
    private static final int CONCURRENT_WRITERS = 3;
    private static final int WRITE_TO_COMMIT_MILLIS = 1000;
    private static final int BARRIER_TIMEOUT_SECONDS = 30;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    private TestCustomerRepository repository;

    @Test
    @Transactional(propagation = NEVER)
    void testConcurrentCommitOrder() {

        // determine initial row count
        final var rowCount = jpaAttempt.transacted(() -> {
            context("hsh-alex_superuser");
            return em.createQuery("SELECT e FROM TestCustomerEntity e", TestCustomerEntity.class).getResultList();
        }).assertSuccessful().returnedValue().size();

        // when 3 transactions with different runtime run concurrently
        runThreads(
                // starts first, ends last (because it's slow)
                createTransactionThread("t01", 91001, 500),

                // starts second, ends first (because it's faster than the one that got started first)
                createTransactionThread("t02", 91002, 0),

                // starts third, ends second
                createTransactionThread("t03", 91003, 100)
        );

        // then all 3 threads did insert one row each
        jpaAttempt.transacted(() -> {
            context("hsh-alex_superuser");
            var all = em.createQuery("SELECT e FROM TestCustomerEntity e", TestCustomerEntity.class).getResultList();
            assertThat(all).hasSize(rowCount + 3);
        }).assertSuccessful();

        // and seqTxId is in commit order, because JPA writes at the flush of the commit and not earlier
        final var txContextsX = em.createNativeQuery(
                "select concat(c.txId, ':', c.currentTask) from base.tx_context c order by c.seqTxId"
            ).getResultList();
        final var txContextTasks = last(3, txContextsX).stream().map(Object::toString).toList();
        assertThat(txContextTasks.get(0)).endsWith(
                ":TestCustomerEntity(uuid=null, version=0, prefix=t02, reference=91002, adminUserName=null)");
        assertThat(txContextTasks.get(1)).endsWith(
                "TestCustomerEntity(uuid=null, version=0, prefix=t03, reference=91003, adminUserName=null)");
        assertThat(txContextTasks.get(2)).endsWith(
                "TestCustomerEntity(uuid=null, version=0, prefix=t01, reference=91001, adminUserName=null)");
    }

    @Test
    @Transactional(propagation = NEVER)
    void concurrentWritersNeitherDeadlockNorWaitForEachOther() {
        val writeSimultaneously = new CyclicBarrier(CONCURRENT_WRITERS);
        val failures = synchronizedList(new ArrayList<Throwable>());

        // when all transactions write at the same moment and then stay open until their commit
        val startedAt = currentTimeMillis();
        runThreads(
                0,
                range(0, CONCURRENT_WRITERS)
                        .mapToObj(i -> createWritingThread("c%02d".formatted(i), 92000 + i, writeSimultaneously, failures))
                        .toArray(Thread[]::new));
        val elapsedMillis = currentTimeMillis() - startedAt;

        // then none of them was aborted by a deadlock
        assertThat(failures).isEmpty();

        // and they overlapped, thus took less time than if even a single one had had to wait
        assertThat(elapsedMillis).isLessThan((CONCURRENT_WRITERS - 1) * WRITE_TO_COMMIT_MILLIS);
    }

    private @NotNull Thread createWritingThread(
            final String prefix,
            final int reference,
            final CyclicBarrier writeSimultaneously,
            final List<Throwable> failures) {
        val thread = new Thread(() -> jpaAttempt.transacted(() -> {
            val customer = new TestCustomerEntity();
            customer.setPrefix(prefix);
            customer.setReference(reference);

            context.define(customer.toString(), null, "hsh-alex_superuser", null);
            repository.save(customer);
            await(writeSimultaneously);
            em.flush(); // without this, JPA would write and commit in one go, and nothing would overlap
            sleep(WRITE_TO_COMMIT_MILLIS);
        }).assertSuccessful());
        thread.setUncaughtExceptionHandler((ignored, throwable) -> failures.add(throwable));
        return thread;
    }

    @SneakyThrows
    private void await(final CyclicBarrier barrier) {
        barrier.await(BARRIER_TIMEOUT_SECONDS, SECONDS); // a plain await would hang the whole build
    }

    private @NotNull Thread createTransactionThread(final String t01, final int reference, final int millis) {
        return new Thread(() -> {
            jpaAttempt.transacted(() -> {
                final var entity1 = new TestCustomerEntity();
                entity1.setPrefix(t01);
                entity1.setReference(reference);

                context.define(entity1.toString(), null, "hsh-alex_superuser", null);
                entity1.setReference(80000 + toInt(em.createNativeQuery("SELECT txid_current()").getSingleResult()));
                repository.save(entity1);
                sleep(millis); // simulate a delay
            }).assertSuccessful();
        });
    }

    private int toInt(final Object singleResult) {
        return ((Long)singleResult).intValue();
    }

    @SneakyThrows
    private void sleep(final int millis) {
        Thread.sleep(millis);
    }

    @SneakyThrows
    private void runThreads(final Thread... threads) {
        runThreads(100, threads);
    }

    @SneakyThrows
    private void runThreads(final int staggeredStartMillis, final Thread... threads) {
        for (final Thread thread : threads) {
            thread.start();
            sleep(staggeredStartMillis);
        }
        for (final Thread thread : threads) {
            thread.join();
        }

    }
    private List<?> last(final int n, final List<?> list) {
        return list.subList(Math.max(list.size() - n, 0), list.size());
    }
}
