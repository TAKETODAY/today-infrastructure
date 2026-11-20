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

package infra.web.util;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.util.Assert;

/**
 * Utilities for validating asynchronous request timeout values.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class TimeoutUtils {

  private static final Duration ONE_MILLISECOND = Duration.ofMillis(1);

  private TimeoutUtils() {
  }

  /**
   * Validate an asynchronous request timeout.
   * <p>{@code null} means that the caller should use its configured default.
   * A positive timeout must be at least one millisecond and must fit in the
   * millisecond representation required by the underlying container.
   *
   * @param timeout the timeout to validate, or {@code null}
   * @throws IllegalArgumentException if the timeout is negative, has a positive
   * sub-millisecond value, or cannot be represented in milliseconds
   */
  public static void validate(@Nullable Duration timeout) {
    if (timeout == null) {
      return;
    }
    Assert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
    Assert.isTrue(timeout.isZero() || timeout.compareTo(ONE_MILLISECOND) >= 0,
            "Timeout must be zero or at least 1 millisecond");
    try {
      timeout.toMillis();
    }
    catch (ArithmeticException ex) {
      throw new IllegalArgumentException("Timeout is too large", ex);
    }
  }

}
