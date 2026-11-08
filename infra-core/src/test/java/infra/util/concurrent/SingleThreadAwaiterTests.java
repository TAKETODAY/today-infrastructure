/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.util.concurrent;

import org.junit.jupiter.api.Test;

import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/14 18:12
 */
class SingleThreadAwaiterTests {

  @Test
  void testBasicAwaitResume() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    boolean[] completed = { false };

    Thread waiter = new Thread(() -> {
      await(awaiter);
      completed[0] = true;
    });
    waiter.start();

    Thread.sleep(100); // 确保 waiter 线程进入等待状态
    assertFalse(completed[0]);

    awaiter.resume();
    waiter.join(1000);
    assertTrue(completed[0]);
  }

  @Test
  void testMultipleResume() {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    // 多次调用 resume 不应该抛出异常
    awaiter.resume();
    awaiter.resume();
    awaiter.resume();
  }

  @Test
  void testResumeBeforeAwait() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    boolean[] completed = { false };

    awaiter.resume(); // 提前调用 resume

    Thread waiter = new Thread(() -> {
      await(awaiter); // 应该立即返回
      completed[0] = true;
    });
    waiter.start();

    waiter.join(1000);
    assertTrue(completed[0]);
  }

  @Test
  void testMultipleThreadsAwait() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    Thread first = new Thread(() -> await(awaiter));
    first.start();

    Thread.sleep(100);

    // 在主线程中尝试等待应该抛出异常
    assertThrows(IllegalStateException.class, () -> await(awaiter));

    awaiter.resume();
  }

  @Test
  void testReuseAwaiter() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    for (int i = 0; i < 3; i++) {
      boolean[] completed = { false };
      Thread waiter = new Thread(() -> {
        await(awaiter);
        completed[0] = true;
      });
      waiter.start();

      Thread.sleep(100);
      assertFalse(completed[0]);

      awaiter.resume();
      waiter.join(1000);
      assertTrue(completed[0]);
    }
  }

  @Test
  void interruptBeforeAwaitThrowsInterruptedIOException() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Thread waiter = new Thread(() -> {
      Thread.currentThread().interrupt();
      try {
        awaiter.await();
      }
      catch (Throwable ex) {
        failure.set(ex);
      }
      assertTrue(Thread.currentThread().isInterrupted());
    });
    waiter.start();
    waiter.join(1000);

    assertFalse(waiter.isAlive());
    assertTrue(failure.get() instanceof InterruptedIOException);
  }

  @Test
  void interruptWhileAwaitingThrowsInterruptedIOException() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    AtomicReference<Throwable> failure = new AtomicReference<>();

    Thread waiter = new Thread(() -> {
      try {
        awaiter.await();
      }
      catch (Throwable ex) {
        failure.set(ex);
      }
    });
    waiter.start();

    Thread.sleep(100); // 确保 waiter 线程进入等待状态
    waiter.interrupt();
    waiter.join(1000);

    assertFalse(waiter.isAlive());
    assertTrue(failure.get() instanceof InterruptedIOException);
    assertTrue(waiter.isInterrupted());
  }

  @Test
  void concurrentResumeAndAwait() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    boolean[] completed = new boolean[threadCount];

    // 创建多个线程交替调用 await 和 resume
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          await(awaiter);
          completed[index] = true;
        }
        catch (IllegalStateException ignored) {
          // 忽略预期的异常
        }
      });
      threads[i].start();
      awaiter.resume();
    }

    // 等待所有线程完成
    for (Thread thread : threads) {
      thread.join(1000);
    }

    // 至少有一个线程应该成功完成
    boolean atLeastOneCompleted = false;
    for (boolean complete : completed) {
      if (complete) {
        atLeastOneCompleted = true;
        break;
      }
    }
    assertTrue(atLeastOneCompleted);
  }

  @Test
  void parkAndUnparkSameThread() {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();

    // 同一个线程先 resume 后 await
    awaiter.resume();
    await(awaiter);

    // 再次 await 应该会暂停
    Thread anotherThread = new Thread(awaiter::resume);
    anotherThread.start();
    await(awaiter);
  }

  @Test
  void testMultipleResumeCallsAreIdempotent() {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();

    // Multiple resume calls should not affect behavior
    awaiter.resume();
    awaiter.resume();

    // Next await should not block
    Thread thread = new Thread(() -> {
      await(awaiter);
      assertTrue(true); // Should reach here immediately
    });
    thread.start();

    try {
      thread.join(1000);
      assertFalse(thread.isAlive());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void testConcurrentMultipleResumesAndAwaits() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();
    int rounds = 5;

    for (int i = 0; i < rounds; i++) {
      boolean[] completed = { false };

      Thread t = new Thread(() -> {
        await(awaiter);
        completed[0] = true;
      });
      t.start();

      Thread.sleep(50); // Allow thread to park

      awaiter.resume();
      t.join(1000);

      assertTrue(completed[0], "Round " + i + " failed");
    }
  }

  @Test
  void testExceptionWhenTwoThreadsAwaitSimultaneously() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();

    // Start first thread that will await
    Thread first = new Thread(() -> await(awaiter));
    first.start();

    Thread.sleep(100); // Give time for first thread to start awaiting

    // Second thread trying to await should throw exception
    Thread second = new Thread(() -> assertThrows(IllegalStateException.class, () -> await(awaiter)));
    second.start();

    second.join(1000);

    // Clean up
    awaiter.resume();
    first.join(1000);
  }

  @Test
  void testStateResetAfterUse() throws InterruptedException {
    SingleThreadAwaiter awaiter = new SingleThreadAwaiter();

    // Use the awaiter once
    boolean[] completed1 = { false };
    Thread t1 = new Thread(() -> {
      await(awaiter);
      completed1[0] = true;
    });
    t1.start();

    Thread.sleep(50);
    awaiter.resume();
    t1.join(1000);
    assertTrue(completed1[0]);

    // Use the awaiter again - should work fine
    boolean[] completed2 = { false };
    Thread t2 = new Thread(() -> {
      await(awaiter);
      completed2[0] = true;
    });
    t2.start();

    Thread.sleep(50);
    awaiter.resume();
    t2.join(1000);
    assertTrue(completed2[0]);
  }

  private static void await(SingleThreadAwaiter awaiter) {
    try {
      awaiter.await();
    }
    catch (InterruptedIOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
