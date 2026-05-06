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

import java.time.Instant;

/**
 * Strategy interface for computing the next version value for optimistic locking.
 * Implementations define how the version field should be incremented or updated
 * when an entity with a {@link Version} annotation is persisted or updated.
 *
 * <p>
 * Users can provide custom implementations to support arbitrary version types
 * beyond the built-in defaults ({@code Integer}, {@code Long}, {@code Short},
 * {@link Instant}).
 *
 * <pre>{@code
 * // Custom strategy for a string-based version
 * VersionIncrementStrategy custom = currentVersion -> {
 *   if (currentVersion instanceof String v) {
 *     return v + "_updated";
 *   }
 *   return null;
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
  Object nextVersion(Object currentVersion);

  /**
   * Combine this strategy with another strategy using fallback semantics.
   * If this strategy successfully computes a version, its result is used;
   * otherwise the {@code fallback} strategy is tried.
   *
   * @param fallback the fallback strategy to use if this one fails
   * @return a composed strategy that tries this strategy first, then the fallback
   */
  default VersionIncrementStrategy and(VersionIncrementStrategy fallback) {
    return currentVersion -> {
      Object next = nextVersion(currentVersion);
      if (next == null) {
        return fallback.nextVersion(currentVersion);
      }
      return next;
    };
  }

}
