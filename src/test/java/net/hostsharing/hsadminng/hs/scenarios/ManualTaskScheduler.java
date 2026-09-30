package net.hostsharing.hsadminng.hs.scenarios;

import lombok.val;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collects the scheduled jobs instead of running them, thus a scenario runs a job when it says so,
 * via the same registration as in production, but without waiting and in a deterministic order.
 */
public class ManualTaskScheduler implements TaskScheduler {

    private final List<Runnable> jobs = new CopyOnWriteArrayList<>();

    /** Runs the one scheduled job declared in the given class, e.g. a method annotated with @Scheduled. */
    public String run(final Class<?> jobClass) {
        // a scheduled method's Runnable names it as "fully.qualified.Class.method"
        val matchingJobs = jobs.stream().filter(job -> job.toString().startsWith(jobClass.getName() + ".")).toList();
        assertThat(matchingJobs).as("scheduled jobs of " + jobClass.getName()).hasSize(1);
        val job = matchingJobs.getFirst();
        job.run();
        return job.toString().substring(jobClass.getPackageName().length() + 1);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable task, final Trigger trigger) {
        return collected(task);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable task, final Instant startTime) {
        return collected(task);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Instant startTime, final Duration period) {
        return collected(task);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Duration period) {
        return collected(task);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable task, final Instant startTime, final Duration delay) {
        return collected(task);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable task, final Duration delay) {
        return collected(task);
    }

    private ScheduledFuture<?> collected(final Runnable task) {
        jobs.add(task);
        return new CollectedJob(task);
    }

    private class CollectedJob implements ScheduledFuture<Object> {

        private final Runnable task;

        CollectedJob(final Runnable task) {
            this.task = task;
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            return Long.MAX_VALUE;
        }

        @Override
        public int compareTo(final Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return jobs.remove(task);
        }

        @Override
        public boolean isCancelled() {
            return !jobs.contains(task);
        }

        @Override
        public boolean isDone() {
            return isCancelled();
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(final long timeout, final TimeUnit unit) {
            return null;
        }
    }
}
