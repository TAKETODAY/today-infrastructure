/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.NonNull;
import io.netty.util.Signal;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static java.lang.Math.max;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DefaultSettableFutureTest {
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultSettableFutureTest.class);
  private static int stackOverflowDepth;

  @BeforeAll
  public static void beforeClass() {
    try {
      findStackOverflowDepth();
      throw new IllegalStateException("Expected StackOverflowError but didn't get it?!");
    }
    catch (StackOverflowError e) {
      logger.debug("StackOverflowError depth: {}", stackOverflowDepth);
    }
  }

  @SuppressWarnings("InfiniteRecursion")
  private static void findStackOverflowDepth() {
    ++stackOverflowDepth;
    findStackOverflowDepth();
  }

  private static int stackOverflowTestDepth() {
    return max(stackOverflowDepth << 1, stackOverflowDepth);
  }

  private static class RejectingEventExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
      fail("Cannot schedule commands");
    }
  }

  @Test
  public void testCancelDoesNotScheduleWhenNoListeners() {
    Executor executor = new RejectingEventExecutor();

    SettableFuture<Void> future = new DefaultFuture<Void>(executor);
    assertTrue(future.cancel(false));
    assertTrue(future.isCancelled());
  }

  @Test
  public void testSuccessDoesNotScheduleWhenNoListeners() {
    Executor executor = new RejectingEventExecutor();

    Object value = new Object();
    SettableFuture<Object> future = new DefaultFuture<Object>(executor);
    future.setSuccess(value);
    assertSame(value, future.getNow());
  }

  @Test
  public void testFailureDoesNotScheduleWhenNoListeners() {
    Executor executor = new RejectingEventExecutor();

    Exception cause = new Exception();
    SettableFuture<Void> future = new DefaultFuture<Void>(executor);
    future.setFailure(cause);
    assertSame(cause, future.cause());
  }

  @Test
  public void testCancellationExceptionIsThrownWhenBlockingGet() {
    final SettableFuture<Void> future = new DefaultFuture<Void>();
    assertTrue(future.cancel(false));
    assertThrows(CancellationException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        future.get();
      }
    });
  }

  @Test
  public void testCancellationExceptionIsThrownWhenBlockingGetWithTimeout() {
    final SettableFuture<Void> future = new DefaultFuture<Void>();
    assertTrue(future.cancel(false));
    assertThrows(CancellationException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        future.get(1, TimeUnit.SECONDS);
      }
    });
  }

  @Test
  public void testCancellationExceptionIsReturnedAsCause() {
    final SettableFuture<Void> future = new DefaultFuture<Void>();
    assertTrue(future.cancel(false));
    assertThat(future.cause()).isInstanceOf(CancellationException.class);
  }

  @Test
  public void testStackOverflowWithImmediateEventExecutorA() throws Exception {
    testStackOverFlowChainedFuturesA(stackOverflowTestDepth(), GlobalEventExecutor.INSTANCE, true);
    testStackOverFlowChainedFuturesA(stackOverflowTestDepth(), GlobalEventExecutor.INSTANCE, false);
  }

  @Test
  public void testNoStackOverflowWithDefaultEventExecutorA() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      EventExecutor executor = new DefaultEventExecutor(executorService);
      try {
        testStackOverFlowChainedFuturesA(stackOverflowTestDepth(), executor, true);
        testStackOverFlowChainedFuturesA(stackOverflowTestDepth(), executor, false);
      }
      finally {
        executor.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
      }
    }
    finally {
      executorService.shutdown();
    }
  }

  @Test
  public void testNoStackOverflowWithDefaultEventExecutorB() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      EventExecutor executor = new DefaultEventExecutor(executorService);
      try {
        testStackOverFlowChainedFuturesB(stackOverflowTestDepth(), executor, true);
        testStackOverFlowChainedFuturesB(stackOverflowTestDepth(), executor, false);
      }
      finally {
        executor.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
      }
    }
    finally {
      executorService.shutdown();
    }
  }

  @Test
  public void testListenerNotifyOrder() throws Exception {
    Executor executor = new TestExecutor();
    final BlockingQueue<FutureListener<ListenableFuture<Void>>> listeners = new LinkedBlockingQueue<>();
    int runs = 100000;

    for (int i = 0; i < runs; i++) {
      final SettableFuture<Void> future = new DefaultFuture<Void>(executor);
      final FutureListener<ListenableFuture<Void>> listener1 = new FutureListener<>() {
        @Override
        public void operationComplete(ListenableFuture<Void> future) throws Exception {
          listeners.add(this);
        }
      };
      final FutureListener<ListenableFuture<Void>> listener2 = new FutureListener<>() {
        @Override
        public void operationComplete(ListenableFuture<Void> future) throws Exception {
          listeners.add(this);
        }
      };
      final FutureListener<ListenableFuture<Void>> listener4 = new FutureListener<>() {
        @Override
        public void operationComplete(ListenableFuture<Void> future) throws Exception {
          listeners.add(this);
        }
      };
      final FutureListener<ListenableFuture<Void>> listener3 = new FutureListener<>() {
        @Override
        public void operationComplete(ListenableFuture<Void> future) throws Exception {
          listeners.add(this);
          future.addListener(listener4);
        }
      };

      GlobalEventExecutor.INSTANCE.execute(new Runnable() {
        @Override
        public void run() {
          future.setSuccess(null);
        }
      });

      future.addListener(listener1).addListener(listener2).addListener(listener3);

      assertSame(listener1, listeners.take(), "Fail 1 during run " + i + " / " + runs);
      assertSame(listener2, listeners.take(), "Fail 2 during run " + i + " / " + runs);
      assertSame(listener3, listeners.take(), "Fail 3 during run " + i + " / " + runs);
      assertSame(listener4, listeners.take(), "Fail 4 during run " + i + " / " + runs);
      assertTrue(listeners.isEmpty(), "Fail during run " + i + " / " + runs);
    }
  }

  @Test
  public void testListenerNotifyLater() throws Exception {
    // Testing first execution path in DefaultPromise
    testListenerNotifyLater(1);

    // Testing second execution path in DefaultPromise
    testListenerNotifyLater(2);
  }

  @Test
  @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
  public void testPromiseListenerAddWhenCompleteFailure() throws Exception {
    testPromiseListenerAddWhenComplete(fakeException());
  }

  @Test
  @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
  public void testPromiseListenerAddWhenCompleteSuccess() throws Exception {
    testPromiseListenerAddWhenComplete(null);
  }

  @Test
  @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
  public void testLateListenerIsOrderedCorrectlySuccess() throws InterruptedException {
    testLateListenerIsOrderedCorrectly(null);
  }

  @Test
  @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
  public void testLateListenerIsOrderedCorrectlyFailure() throws InterruptedException {
    testLateListenerIsOrderedCorrectly(fakeException());
  }

  @Test
  public void testSignalRace() {
    final long wait = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);
    Executor executor = new TestExecutor();

    final int numberOfAttempts = 4096;
    final Map<Thread, DefaultFuture<Void>> promises = new HashMap<Thread, DefaultFuture<Void>>();
    for (int i = 0; i < numberOfAttempts; i++) {
      final DefaultFuture<Void> promise = new DefaultFuture<Void>(executor);
      final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          promise.setSuccess(null);
        }
      });
      promises.put(thread, promise);
    }

    for (final Map.Entry<Thread, DefaultFuture<Void>> promise : promises.entrySet()) {
      promise.getKey().start();
      final long start = System.nanoTime();
      promise.getValue().awaitUninterruptibly(wait, TimeUnit.NANOSECONDS);
      assertThat(System.nanoTime() - start).isLessThan(wait);
    }
  }

  @Test
  public void signalUncancellableCompletionValue() {
    final SettableFuture<Signal> future = new DefaultFuture<Signal>();
    future.setSuccess(Signal.valueOf(DefaultFuture.class, "UNCANCELLABLE"));
    assertTrue(future.isDone());
    assertTrue(future.isSuccess());
  }

  @Test
  public void signalSuccessCompletionValue() {
    final SettableFuture<Signal> future = new DefaultFuture<Signal>();
    future.setSuccess(Signal.valueOf(DefaultFuture.class, "SUCCESS"));
    assertTrue(future.isDone());
    assertTrue(future.isSuccess());
  }

  @Test
  public void setUncancellableGetNow() {
    final SettableFuture<String> future = new DefaultFuture<String>();
    assertNull(future.getNow());
    assertTrue(future.setUncancellable());
    assertNull(future.getNow());
    assertFalse(future.isDone());
    assertFalse(future.isSuccess());

    future.setSuccess("success");

    assertTrue(future.isDone());
    assertTrue(future.isSuccess());
    assertEquals("success", future.getNow());
  }

  private static void testStackOverFlowChainedFuturesA(int promiseChainLength, final Executor executor,
          boolean runTestInExecutorThread) throws InterruptedException {
    final SettableFuture<Void>[] p = new DefaultFuture[promiseChainLength];
    final CountDownLatch latch = new CountDownLatch(promiseChainLength);

    if (runTestInExecutorThread) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          testStackOverFlowChainedFuturesA(executor, p, latch);
        }
      });
    }
    else {
      testStackOverFlowChainedFuturesA(executor, p, latch);
    }

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    for (int i = 0; i < p.length; ++i) {
      assertTrue(p[i].isSuccess(), "index " + i);
    }
  }

  private static void testStackOverFlowChainedFuturesA(Executor executor, final SettableFuture<Void>[] p,
          final CountDownLatch latch) {
    for (int i = 0; i < p.length; i++) {
      final int finalI = i;
      p[i] = new DefaultFuture<Void>(executor).addListener(future -> {
        if (finalI + 1 < p.length) {
          p[finalI + 1].setSuccess(null);
        }
        latch.countDown();
      });
    }

    p[0].setSuccess(null);
  }

  private static void testStackOverFlowChainedFuturesB(int promiseChainLength,
          final Executor executor, boolean runTestInExecutorThread) throws InterruptedException {
    final SettableFuture<Void>[] p = new DefaultFuture[promiseChainLength];
    final CountDownLatch latch = new CountDownLatch(promiseChainLength);

    if (runTestInExecutorThread) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          testStackOverFlowChainedFuturesB(executor, p, latch);
        }
      });
    }
    else {
      testStackOverFlowChainedFuturesB(executor, p, latch);
    }

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    for (int i = 0; i < p.length; ++i) {
      assertTrue(p[i].isSuccess(), "index " + i);
    }
  }

  private static void testStackOverFlowChainedFuturesB(Executor executor, final SettableFuture<Void>[] p,
          final CountDownLatch latch) {
    for (int i = 0; i < p.length; i++) {
      final int finalI = i;
      p[i] = new DefaultFuture<Void>(executor);
      p[i].addListener(future -> future.addListener(future1 -> {
        if (finalI + 1 < p.length) {
          p[finalI + 1].setSuccess(null);
        }
        latch.countDown();
      }));
    }

    p[0].setSuccess(null);
  }

  /**
   * This test is mean to simulate the following sequence of events, which all take place on the I/O thread:
   * <ol>
   * <li>A write is done</li>
   * <li>The write operation completes, and the promise state is changed to done</li>
   * <li>A listener is added to the return from the write. The {@link FutureListener#operationComplete(ListenableFuture)}
   * updates state which must be invoked before the response to the previous write is read.</li>
   * <li>The write operation</li>
   * </ol>
   */
  private static void testLateListenerIsOrderedCorrectly(Throwable cause) throws InterruptedException {
    final Executor executor = new TestExecutor();
    final AtomicInteger state = new AtomicInteger();
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(2);
    final SettableFuture<Void> promise = new DefaultFuture<Void>(executor);

    // Add a listener before completion so "lateListener" is used next time we add a listener.
    promise.addListener(future -> assertTrue(state.compareAndSet(0, 1)));

    // Simulate write operation completing, which will execute listeners in another thread.
    if (cause == null) {
      promise.setSuccess(null);
    }
    else {
      promise.setFailure(cause);
    }

    // Add a "late listener"
    promise.addListener(future -> {
      assertTrue(state.compareAndSet(1, 2));
      latch1.countDown();
    });

    // Wait for the listeners and late listeners to be completed.
    latch1.await();
    assertEquals(2, state.get());

    // This is the important listener. A late listener that is added after all late listeners
    // have completed, and needs to update state before a read operation (on the same executor).
    executor.execute(() -> promise.addListener(future -> {
      assertTrue(state.compareAndSet(2, 3));
      latch2.countDown();
    }));

    // Simulate a read operation being queued up in the executor.
    executor.execute(() -> {
      // This is the key, we depend upon the state being set in the next listener.
      assertEquals(3, state.get());
      latch2.countDown();
    });

    latch2.await();
  }

  private static void testPromiseListenerAddWhenComplete(Throwable cause) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final SettableFuture<Void> promise = new DefaultFuture<>();
    promise.addListener(future -> promise.addListener(future1 -> latch.countDown()));
    if (cause == null) {
      promise.setSuccess(null);
    }
    else {
      promise.setFailure(cause);
    }
    latch.await();
  }

  private static void testListenerNotifyLater(final int numListenersBefore) throws Exception {
    Executor executor = new TestExecutor();
    int expectedCount = numListenersBefore + 2;
    final CountDownLatch latch = new CountDownLatch(expectedCount);
    final FutureListener<ListenableFuture<Void>> listener = future -> latch.countDown();
    final SettableFuture<Void> future = new DefaultFuture<Void>(executor);
    executor.execute(() -> {
      for (int i = 0; i < numListenersBefore; i++) {
        future.addListener(listener);
      }
      future.setSuccess(null);

      GlobalEventExecutor.INSTANCE.execute(() -> future.addListener(listener));
      future.addListener(listener);
    });

    assertTrue(latch.await(5, TimeUnit.SECONDS),
            "Should have notified " + expectedCount + " listeners");
  }

  private static final class TestExecutor implements Executor {

//    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void execute(@NonNull Runnable command) {
      command.run();
//      executorService.execute(command);
    }

  }

  private static RuntimeException fakeException() {
    return new RuntimeException("fake exception");
  }

}
