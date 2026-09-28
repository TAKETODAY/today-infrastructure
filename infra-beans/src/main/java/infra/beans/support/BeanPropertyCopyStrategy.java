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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.util.Set;

import infra.beans.BeanProperty;
import infra.util.Assert;

/**
 * Decides whether a source value should be copied to a writable bean property.
 * The source may be a bean or a map. The value is read before this strategy is
 * invoked, and is {@code null} when the source property or map entry is null.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface BeanPropertyCopyStrategy {

  /**
   * Decide whether to copy a source value to a destination property.
   *
   * @param source the source bean or map
   * @param property the matching writable destination property
   * @param value the source value, possibly {@code null}
   * @return whether to copy the value
   */
  boolean shouldCopy(Object source, BeanProperty property, @Nullable Object value);

  /**
   * Combine this strategy with another, requiring both to allow copying.
   *
   * @param next the strategy to evaluate after this one
   * @return the combined strategy
   */
  default BeanPropertyCopyStrategy and(BeanPropertyCopyStrategy next) {
    Assert.notNull(next, "next strategy is required");
    return (source, property, value) -> shouldCopy(source, property, value)
            && next.shouldCopy(source, property, value);
  }

  /**
   * Combine this strategy with another, allowing either to permit copying.
   *
   * @param next the strategy to evaluate after this one
   * @return the combined strategy
   */
  default BeanPropertyCopyStrategy or(BeanPropertyCopyStrategy next) {
    Assert.notNull(next, "next strategy is required");
    return (source, property, value) -> shouldCopy(source, property, value)
            || next.shouldCopy(source, property, value);
  }

  /**
   * Return a strategy that copies every matching property, including null values.
   *
   * @return the unconditional strategy
   */
  static BeanPropertyCopyStrategy always() {
    return (source, property, value) -> true;
  }

  /**
   * Return a strategy that skips null source values.
   *
   * @return the non-null strategy
   */
  static BeanPropertyCopyStrategy nonNull() {
    return (source, property, value) -> value != null;
  }

  /**
   * Return a strategy that excludes properties with the given names.
   *
   * @param ignoreProperties the destination property names to exclude
   * @return a strategy that copies only properties outside the set
   */
  static BeanPropertyCopyStrategy ignoreProperties(Set<String> ignoreProperties) {
    Assert.notNull(ignoreProperties, "ignoreProperties is required");
    Set<String> names = Set.copyOf(ignoreProperties);
    return (source, property, value) -> !names.contains(property.getName());
  }

}
