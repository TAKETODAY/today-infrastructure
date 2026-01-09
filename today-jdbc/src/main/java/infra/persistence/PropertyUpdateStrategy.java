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

import java.util.Objects;

/**
 * A strategy interface for determining whether a specific property of an entity
 * should be updated during an update operation. Implementations of this interface
 * define custom logic for evaluating properties based on their metadata and the
 * state of the entity.
 *
 * <p>This interface provides methods to combine multiple strategies using logical
 * operations (`and`, `or`) and includes static factory methods for common update
 * strategies such as always updating or updating only non-null properties.
 *
 * <p>Example usage:
 * <pre>{@code
 * var strategy = PropertyUpdateStrategy.noneNull().and(PropertyUpdateStrategy.always());
 * boolean shouldUpdate = strategy.shouldUpdate(entity, property);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UpdateStrategySource
 * @since 4.0 2022/12/18 22:20
 */
public interface PropertyUpdateStrategy {

  /**
   * Determines whether a specific property of an entity should be updated.
   *
   * @param entity the entity object whose property is being evaluated for updating
   * @param property the metadata of the property to be checked, encapsulating
   * details such as its name, type, and value
   * @return {@code true} if the property should be updated based on the
   * implemented strategy, {@code false} otherwise
   */
  boolean shouldUpdate(Object entity, EntityProperty property);

  /**
   * Combines this {@code PropertyUpdateStrategy} with another strategy using a logical AND operation.
   * The resulting strategy will only allow updates if both this strategy and the provided
   * {@code next} strategy determine that the property should be updated.
   *
   * @param next the subsequent {@code PropertyUpdateStrategy} to combine with this strategy
   * @return a new {@code PropertyUpdateStrategy} that evaluates to {@code true} only if both
   * this strategy and the {@code next} strategy return {@code true} for the given
   * entity and property
   */
  default PropertyUpdateStrategy and(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) && next.shouldUpdate(entity, property);
  }

  /**
   * Combines this {@code PropertyUpdateStrategy} with another strategy using a logical OR operation.
   * The resulting strategy will allow updates if either this strategy or the provided
   * {@code next} strategy determines that the property should be updated.
   *
   * @param next the subsequent {@code PropertyUpdateStrategy} to combine with this strategy
   * @return a new {@code PropertyUpdateStrategy} that evaluates to {@code true} if either
   * this strategy or the {@code next} strategy returns {@code true} for the given
   * entity and property
   */
  default PropertyUpdateStrategy or(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) || next.shouldUpdate(entity, property);
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that determines whether a property
   * should be updated based on whether its value is not {@code null}.
   * <p>
   * This strategy evaluates to {@code true} if the property's value, obtained via
   * {@link EntityProperty#getValue(Object)}, is not {@code null} for the given entity.
   * Otherwise, it evaluates to {@code false}.
   *
   * @return a {@code PropertyUpdateStrategy} instance that allows updates only if
   * the property's value is not {@code null}
   */
  static PropertyUpdateStrategy noneNull() {
    return (entity, property) -> property.getValue(entity) != null;
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that always allows updates for any property
   * of any entity. This strategy unconditionally returns {@code true} for all evaluations,
   * meaning no restrictions are applied to property updates.
   *
   * @return a {@code PropertyUpdateStrategy} instance that permits updates for all properties
   */
  static PropertyUpdateStrategy always() {
    return (entity, property) -> true;
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that never allows updates for any property.
   * This strategy unconditionally returns {@code false} for all evaluations.
   *
   * @return a {@code PropertyUpdateStrategy} instance that denies updates for all properties
   */
  static PropertyUpdateStrategy never() {
    return (entity, property) -> false;
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that determines whether a property
   * is marked as an identifier (ID) property.
   * <p>
   * This strategy evaluates to {@code true} if the property is annotated with
   * the {@link Id} annotation or is otherwise identified as an ID property within
   * the metadata. Otherwise, it evaluates to {@code false}.
   *
   * @return a {@code PropertyUpdateStrategy} instance that allows updates only if
   * the property is an ID property
   */
  static PropertyUpdateStrategy isId() {
    return (entity, property) -> property.isIdProperty;
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that determines whether a property
   * should be updated based on whether it is <b>not</b> marked as an identifier (ID) property.
   * <p>
   * This strategy evaluates to {@code true} if the property is not annotated with
   * the {@link Id} annotation or is otherwise not identified as an ID property within
   * the metadata. Otherwise, it evaluates to {@code false}.
   *
   * @return a {@code PropertyUpdateStrategy} instance that allows updates only if
   * the property is not an ID property
   */
  static PropertyUpdateStrategy notId() {
    return (entity, property) -> !property.isIdProperty;
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that determines whether a property's value
   * is equal to the specified {@code expectedValue}.
   * <p>
   * This strategy evaluates to {@code true} if the property's value, obtained via
   * {@link EntityProperty#getValue(Object)}, is equal to the {@code expectedValue}
   * for the given entity. Otherwise, it evaluates to {@code false}.
   *
   * @param expectedValue the value to compare against the property's value
   * @return a {@code PropertyUpdateStrategy} instance that allows updates only if
   * the property's value is equal to the {@code expectedValue}
   */
  static PropertyUpdateStrategy isEqual(Object expectedValue) {
    return (entity, property) -> Objects.equals(expectedValue, property.getValue(entity));
  }

  /**
   * Returns a {@code PropertyUpdateStrategy} that determines whether a property's value
   * is not equal to the specified {@code expectedValue}.
   * <p>
   * This strategy evaluates to {@code true} if the property's value, obtained via
   * {@link EntityProperty#getValue(Object)}, is not equal to the {@code expectedValue}
   * for the given entity. Otherwise, it evaluates to {@code false}.
   *
   * @param expectedValue the value to compare against the property's value
   * @return a {@code PropertyUpdateStrategy} instance that allows updates only if
   * the property's value is not equal to the {@code expectedValue}
   */
  static PropertyUpdateStrategy isNotEqual(Object expectedValue) {
    return (entity, property) -> !Objects.equals(expectedValue, property.getValue(entity));
  }

}
