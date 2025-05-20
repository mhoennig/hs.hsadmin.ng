package net.hostsharing.hsadminng.journal;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerEntity;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NEVER;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
@Tag("generalIntegrationTest")
class TransactionContextIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
    HttpServletRequest request;

    @Autowired
    private TestCustomerRepository repository;

    @Test
    @Transactional(propagation = NEVER)
    void testConcurrentCommitOrder() {

        // determine initial row count
        final var rowCount = jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
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
            context("superuser-alex@hostsharing.net");
            var all = em.createQuery("SELECT e FROM TestCustomerEntity e", TestCustomerEntity.class).getResultList();
            assertThat(all).hasSize(rowCount + 3);
        }).assertSuccessful();

        // and seqTxId order is in correct order
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

    private @NotNull Thread createTransactionThread(final String t01, final int reference, final int millis) {
        return new Thread(() -> {
            jpaAttempt.transacted(() -> {
                final var entity1 = new TestCustomerEntity();
                entity1.setPrefix(t01);
                entity1.setReference(reference);

                context.define(entity1.toString(), null, "superuser-alex@hostsharing.net", null);
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
        for (final Thread thread : threads) {
            thread.start();
            sleep(100);
        }
        for (final Thread thread : threads) {
            thread.join();
        }

    }
    private List<?> last(final int n, final List<?> list) {
        return list.subList(Math.max(list.size() - n, 0), list.size());
    }
}
