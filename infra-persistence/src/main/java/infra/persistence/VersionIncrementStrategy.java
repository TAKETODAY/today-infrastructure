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
 * Strategy for computing the next version value of a {@link Version} property for
 * optimistic locking. Implementations decide how the version is advanced when the
 * entity is updated.
 *
 * <p>A strategy returns {@code null} to indicate that it does not support the given
 * version type. Composed strategies (see {@link #and(VersionIncrementStrategy)}) use
 * this to fall back to another strategy, and the entity manager fails with an
 * exception when no strategy produced a value.
 *
 * <p>The default strategy {@link infra.persistence.support.DefaultVersionIncrementStrategy} supports numeric
 * types ({@code Integer}, {@code Long}, {@code Short}) by incrementing them, and
 * date-time types ({@link Instant}, {@code LocalDateTime}, {@code ZonedDateTime},
 * {@code OffsetDateTime}) by setting them to the current time. Custom implementations
 * can be provided to support arbitrary version types:
 *
 * <pre>{@code
 * // Custom strategy for a string-based version
 * VersionIncrementStrategy custom = currentVersion -> {
 *   if (currentVersion instanceof String v) {
 *     return v + "_updated";
 *   }
 *   return null; // unsupported type, let a fallback handle it
 * };
 *
 * entityManager.setVersionIncrementStrategy(custom);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.support.DefaultVersionIncrementStrategy
 * @since 5.0
 */
@FunctionalInterface
public interface VersionIncrementStrategy {

  /**
   * Compute the next version value based on the current version.
   *
   * @param currentVersion the current version value (never {@code null} when
   * invoked by the entity manager)
   * @return the next version value, or {@code null} if the given version type is
   * not supported by this strategy
   */
  @Nullable
  Object nextVersion(Object currentVersion);

  /**
   * Return a composed strategy that tries this strategy first and falls back to the
   * given strategy when this one returns {@code null} (unsupported version type).
   *
   * @param fallback the strategy to use when this one returns {@code null}
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
