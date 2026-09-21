package com.heroclock.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkQueueTest {
    @Test void deadlinesCoalesceAndPreserveOrder() {
        List<Integer> output = new ArrayList<>();
        WorkQueue queue = new WorkQueue(3, fail -> fail(fail), () -> 0);
        queue.submit("player", "sync", 20, () -> output.add(1));
        queue.submit("player", "sync", 5, () -> output.add(2));
        queue.submit("player", "cleanup", 5, () -> output.add(3));
        assertEquals(0, queue.drain(4, 10, 100));
        assertEquals(2, queue.drain(5, 10, 100));
        assertEquals(List.of(2, 3), output);
        assertEquals(0, queue.size());
        assertEquals(0, queue.drain(20, 10, 100));
    }

    @Test void boundsBacklogAndTickWork() {
        WorkQueue queue = new WorkQueue(2, fail -> fail(fail), () -> 0);
        assertTrue(queue.submit("a", "a", 0, () -> false));
        assertTrue(queue.submit("b", "b", 0, () -> true));
        assertFalse(queue.submit("c", "c", 0, () -> true));
        assertEquals(1, queue.rejected());
        assertEquals(1, queue.drain(0, 1, 100));
        assertEquals(1, queue.drain(0, 5, 100));
        assertEquals(1, queue.size());
        assertEquals(0, queue.drain(0, 5, 100));
        assertEquals(1, queue.drain(1, 5, 100));
    }

    @Test void cancellationReleasesFutureWork() {
        WorkQueue queue = new WorkQueue(3, fail -> fail(fail), () -> 0);
        queue.submit("a", "first", Long.MAX_VALUE, () -> fail("cancelled"));
        queue.submit("a", "second", Long.MAX_VALUE, () -> fail("cancelled"));
        queue.submit("b", "first", 10, () -> true);
        queue.cancelOwner("a");
        assertEquals(1, queue.size());
        assertTrue(queue.cancel("b", "first"));
        assertEquals(0, queue.size());
    }

    @Test void failureDoesNotLoseOtherJobs() {
        List<RuntimeException> failures = new ArrayList<>();
        WorkQueue queue = new WorkQueue(3, failures::add, () -> 0);
        queue.submit("a", "bad", 0, () -> { throw new IllegalStateException("test"); });
        queue.submit("a", "good", 0, () -> true);
        assertEquals(2, queue.drain(0, 10, 100));
        assertEquals(1, failures.size());
        assertEquals(1, queue.failed());
        assertEquals(0, queue.size());
    }

    @Test void timeBudgetStopsStartingMoreSteps() {
        AtomicLong clock = new AtomicLong();
        WorkQueue queue = new WorkQueue(3, fail -> fail(fail), clock::get);
        queue.submit("a", "one", 0, () -> { clock.addAndGet(10); return true; });
        queue.submit("a", "two", 0, () -> true);
        assertEquals(1, queue.drain(0, 128, 5));
        assertEquals(1, queue.size());
    }

    @Test void callbackReplacementWinsOverContinuation() {
        WorkQueue queue = new WorkQueue(3, fail -> fail(fail), () -> 0);
        queue.submit("a", "job", 0, () -> {
            queue.submit("a", "job", 20, () -> true);
            return false;
        });
        assertEquals(1, queue.drain(0, 10, 100));
        assertEquals(0, queue.drain(1, 10, 100));
        assertEquals(1, queue.drain(20, 10, 100));
    }

    @Test void rejectsCrossThreadUse() throws InterruptedException {
        WorkQueue queue = new WorkQueue(3, fail -> fail(fail), () -> 0);
        List<Throwable> failures = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try { queue.submit("a", "a", 0, () -> true); }
            catch (Throwable failure) { failures.add(failure); }
        });
        thread.start(); thread.join();
        assertInstanceOf(IllegalStateException.class, failures.get(0));
    }
}
