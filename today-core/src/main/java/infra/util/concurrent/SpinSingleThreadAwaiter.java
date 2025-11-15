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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A spin-optimized implementation of {@link SimpleSingleThreadAwaiter} that uses
 * adaptive spinning before parking threads.
 *
 * <p>This implementation attempts to reduce latency by spinning for a while before
 * parking the thread, which can be beneficial in scenarios with short wait times.
 * The spinning behavior adapts based on the success rate of previous spins and the
 * number of available CPU cores.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Adaptive spinning based on historical success rate</li>
 *   <li>Automatically disables spinning on single-core systems</li>
 *   <li>Uses {@code Thread.onSpinWait()} to reduce CPU pressure during spinning</li>
 *   <li>Falls back to parking if spinning is unsuccessful</li>
 * </ul>
 *
 * <p>Best suited for scenarios where:</p>
 * <ul>
 *   <li>Wait times are expected to be very short</li>
 *   <li>Running on multicore systems</li>
 *   <li>Low latency is more important than CPU utilization</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/9/13 22:36
 */
public class SpinSingleThreadAwaiter extends SimpleSingleThreadAwaiter {

  private static final int SPIN_THRESHOLD = 1000;

  private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

  private static final int MAX_SPIN = CPU_CORES > 1 ? SPIN_THRESHOLD : 0;

  private final AtomicInteger successiveSpins = new AtomicInteger();

  @Override
  public void await() {
    Thread toUnpark = Thread.currentThread();
    int currentSpinCount = calculateSpinCount();

    while (true) {
      Object current = this.parkedThread.get();
      if (current == READY) {
        successiveSpins.incrementAndGet();
        break;
      }

      if (current != null && current != toUnpark) {
        throw new IllegalStateException("Only one (Virtual)Thread can await!");
      }

      // 先尝试自旋
      if (currentSpinCount > 0) {
        currentSpinCount--;
        if (currentSpinCount % 10 == 0) { // 降低 CPU 压力
          Thread.onSpinWait();
        }
        continue;
      }

      // 自旋失败，降低下次自旋次数
      successiveSpins.decrementAndGet();

      if (this.parkedThread.compareAndSet(null, toUnpark)) {
        LockSupport.park();
      }
    }

    this.parkedThread.lazySet(null);
  }

  private int calculateSpinCount() {
    if (MAX_SPIN <= 0) {
      return 0; // 单核系统直接放弃自旋
    }

    int successSpins = successiveSpins.get();
    // 根据历史成功率动态调整自旋次数
    return Math.min(MAX_SPIN, Math.max(0, SPIN_THRESHOLD + (successSpins * 10)));
  }

}
