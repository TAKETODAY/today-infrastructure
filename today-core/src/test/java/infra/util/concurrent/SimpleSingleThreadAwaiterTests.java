/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/14 18:12
 */
class SimpleSingleThreadAwaiterTests {

  @Test
  void testBasicAwaitResume() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    boolean[] completed = { false };

    Thread waiter = new Thread(() -> {
      awaiter.await();
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
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    // 多次调用 resume 不应该抛出异常
    awaiter.resume();
    awaiter.resume();
    awaiter.resume();
  }

  @Test
  void testResumeBeforeAwait() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    boolean[] completed = { false };

    awaiter.resume(); // 提前调用 resume

    Thread waiter = new Thread(() -> {
      awaiter.await(); // 应该立即返回
      completed[0] = true;
    });
    waiter.start();

    waiter.join(1000);
    assertTrue(completed[0]);
  }

  @Test
  void testMultipleThreadsAwait() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    Thread first = new Thread(awaiter::await);
    first.start();

    Thread.sleep(100);

    // 在主线程中尝试等待应该抛出异常
    assertThrows(IllegalStateException.class, awaiter::await);

    awaiter.resume();
  }

  @Test
  void testReuseAwaiter() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    for (int i = 0; i < 3; i++) {
      boolean[] completed = { false };
      Thread waiter = new Thread(() -> {
        awaiter.await();
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
  void awaitAfterInterrupt() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    boolean[] completed = { false };

    Thread waiter = new Thread(() -> {
      Thread.currentThread().interrupt();
      awaiter.await(); // 即使线程被中断，await 也应该正常工作
      completed[0] = true;
    });
    waiter.start();

    Thread.sleep(100);
    assertFalse(completed[0]);

    awaiter.resume();
    waiter.join(1000);
    assertTrue(completed[0]);
  }

  @Test
  void concurrentResumeAndAwait() throws InterruptedException {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    boolean[] completed = new boolean[threadCount];

    // 创建多个线程交替调用 await 和 resume
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          awaiter.await();
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
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();

    // 同一个线程先 resume 后 await
    awaiter.resume();
    awaiter.await();

    // 再次 await 应该会暂停
    Thread anotherThread = new Thread(awaiter::resume);
    anotherThread.start();
    awaiter.await();
  }

}
