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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Strategy interface for computing the next version value for optimistic locking.
 * Implementations define how the version field should be incremented or updated
 * when an entity with a {@link Version} annotation is persisted or updated.
 *
 * <p>
 * Users can provide custom implementations to support arbitrary version types
 * beyond the built-in defaults ({@code Integer}, {@code Long}, {@code Short},
 * {@link Timestamp}, {@link Instant}).
 *
 * <pre>{@code
 * // Custom strategy for a string-based version
 * VersionIncrementStrategy custom = currentVersion -> {
 *   String v = (String) currentVersion;
 *   return v + "_updated";
 * };
 *
 * entityManager.setVersionIncrementStrategy(custom);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface VersionIncrementStrategy {

  /**
   * Compute the next version value based on the current version.
   *
   * @param currentVersion the current version value, may be {@code null}
   * @return the next version value
   * @throws IllegalArgumentException if the version type is not supported by this strategy
   */
  @Nullable
  Object nextVersion(@Nullable Object currentVersion);

  /**
   * Combine this strategy with another strategy using fallback semantics.
   * If this strategy successfully computes a version, its result is used;
   * otherwise the {@code fallback} strategy is tried.
   *
   * @param fallback the fallback strategy to use if this one fails
   * @return a composed strategy that tries this strategy first, then the fallback
   */
  default VersionIncrementStrategy or(VersionIncrementStrategy fallback) {
    return currentVersion -> {
      try {
        return nextVersion(currentVersion);
      }
      catch (IllegalArgumentException ignored) {
        return fallback.nextVersion(currentVersion);
      }
    };
  }

  /**
   * Returns the default composite strategy that handles built-in version types:
   * <ul>
   *   <li>{@code null} → 1</li>
   *   <li>{@code Integer} / {@code int} → value + 1</li>
   *   <li>{@code Long} / {@code long} → value + 1</li>
   *   <li>{@code Short} / {@code short} → value + 1</li>
   *   <li>{@link Timestamp} → current time</li>
   *   <li>{@link Instant} → current time</li>
   * </ul>
   *
   * @return the default version increment strategy
   */
  static VersionIncrementStrategy defaults() {
    return forNull()
            .or(forInteger())
            .or(forLong())
            .or(forShort())
            .or(forTimestamp())
            .or(forInstant());
  }

  /**
   * Creates a strategy that returns {@code 1} for a {@code null} current version.
   */
  static VersionIncrementStrategy forNull() {
    return currentVersion -> {
      if (currentVersion == null) {
        return 1;
      }
      throw new IllegalArgumentException("Not null");
    };
  }

  /**
   * Creates a strategy that increments {@link Integer} values by 1.
   */
  static VersionIncrementStrategy forInteger() {
    return currentVersion -> {
      if (currentVersion instanceof Integer i) {
        return i + 1;
      }
      throw new IllegalArgumentException("Not an Integer: " + currentVersion.getClass());
    };
  }

  /**
   * Creates a strategy that increments {@link Long} values by 1.
   */
  static VersionIncrementStrategy forLong() {
    return currentVersion -> {
      if (currentVersion instanceof Long l) {
        return l + 1L;
      }
      throw new IllegalArgumentException("Not a Long: " + currentVersion.getClass());
    };
  }

  /**
   * Creates a strategy that increments {@link Short} values by 1.
   */
  static VersionIncrementStrategy forShort() {
    return currentVersion -> {
      if (currentVersion instanceof Short s) {
        return (short) (s + 1);
      }
      throw new IllegalArgumentException("Not a Short: " + currentVersion.getClass());
    };
  }

  /**
   * Creates a strategy that returns the current system time as a {@link Timestamp}.
   */
  static VersionIncrementStrategy forTimestamp() {
    return currentVersion -> {
      if (currentVersion instanceof Timestamp) {
        return new Timestamp(System.currentTimeMillis());
      }
      throw new IllegalArgumentException("Not a Timestamp: " + currentVersion.getClass());
    };
  }

  /**
   * Creates a strategy that returns the current system time as an {@link Instant}.
   */
  static VersionIncrementStrategy forInstant() {
    return currentVersion -> {
      if (currentVersion instanceof Instant) {
        return Instant.now();
      }
      throw new IllegalArgumentException("Not an Instant: " + currentVersion.getClass());
    };
  }

}
