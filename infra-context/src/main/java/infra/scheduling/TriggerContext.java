/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling;

import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;

/**
 * Context object encapsulating last execution times and last completion time
 * of a given task.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface TriggerContext {

  /**
   * Return the clock to use for trigger calculation.
   *
   * @see TaskScheduler#getClock()
   * @see Clock#systemDefaultZone()
   */
  default Clock getClock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Return the last <i>scheduled</i> execution time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Instant lastScheduledExecution();

  /**
   * Return the last <i>actual</i> execution time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Instant lastActualExecution();

  /**
   * Return the last completion time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Instant lastCompletion();

}
