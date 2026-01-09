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

import java.util.concurrent.atomic.AtomicInteger;

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

  public SpinSingleThreadAwaiter() {
    super(false);
  }

  public SpinSingleThreadAwaiter(boolean parkNanosEnabled) {
    super(parkNanosEnabled);
  }

  @Override
  public void await() {
    Thread currentThread = Thread.currentThread();
    int currentSpinCount = calculateSpinCount();

    for (; ; ) {
      Object current = this.parkedThread.get();
      if (current == READY) {
        successiveSpins.incrementAndGet();
        break;
      }

      if (current != null && current != currentThread) {
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

      if (this.parkedThread.compareAndSet(null, currentThread)) {
        parkThread();
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
