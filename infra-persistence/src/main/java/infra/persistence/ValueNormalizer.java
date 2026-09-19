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

import infra.persistence.annotation.Trim;

/**
 * Normalizes an entity property value before it is turned into a query condition.
 *
 * <p>A normalizer inspects the {@link EntityProperty} to decide whether and how
 * to transform the raw value. For example, {@link #DEFAULT} trims the string
 * value of a property annotated with {@link Trim @Trim}. A normalizer is a global
 * singleton shared across properties and strategies, so it never allocates a new
 * instance per property.
 *
 * <p>A normalizer is supplied to
 * {@link PropertyConditionStrategy#resolve(boolean, EntityProperty, Object, ValueNormalizer)}
 * and used by the strategy when it needs the normalized value. The supplied
 * normalizer is never {@code null}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyConditionStrategy
 * @see Trim
 * @since 5.0
 */
@FunctionalInterface
public interface ValueNormalizer {

  /**
   * Normalize the given property value.
   *
   * <p>The value is never {@code null}; a normalizer that does not apply to the
   * value returns it unchanged rather than {@code null}.
   *
   * @param entityProperty the mapped entity property
   * @param value the raw property value, never {@code null}
   * @return the normalized value, never {@code null}
   */
  Object normalize(EntityProperty entityProperty, Object value);

  /**
   * The shared default normalizer. It trims the string value of a property
   * annotated with {@link Trim @Trim} and leaves every other value unchanged.
   */
  ValueNormalizer DEFAULT = (entityProperty, value) -> {
    if (value instanceof String string && entityProperty.isPresent(Trim.class)) {
      return string.trim();
    }
    return value;
  };

}
