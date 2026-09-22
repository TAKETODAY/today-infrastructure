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

/**
 * Strategy for turning an entity property value into a SQL restriction and its
 * bindable value.
 *
 * <p>Strategies are consulted while an example object is converted into a query.
 * A strategy may decline a property by returning {@code null}; otherwise it
 * returns a {@link Condition} that keeps SQL rendering and JDBC parameter
 * binding in the same order.
 *
 * <p>How the produced condition is joined to its siblings is assembly-time
 * metadata and not part of this contract: the caller decides the connectors.
 *
 * <p>A property whose value is {@code null} is ruled either by
 * {@link #resolve(EntityMetadata, EntityProperty)} — by default returning
 * {@code null} so that the property takes no part in the query — or by a strategy
 * willing to contribute a nullness predicate such as {@code IS NULL}.
 *
 * <p>A {@link ValueNormalizer} is supplied to
 * {@link #resolve(EntityMetadata, EntityProperty, Object, ValueNormalizer)}; the
 * strategy uses it when it needs a normalized value (for example trimming a
 * string value of a property annotated with {@code @Trim}).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/24 23:58
 */
public interface PropertyConditionStrategy {

  /**
   * Resolve a condition for the given mapped property and value.
   *
   * <p>The value is never {@code null}; a {@code null} property value is routed
   * to {@link #resolve(EntityMetadata, EntityProperty)} instead.
   *
   * @param entityMetadata the metadata of the entity being queried, never {@code null}
   * @param entityProperty the mapped entity property
   * @param value the property value to evaluate
   * @param valueNormalizer the normalizer for the property, never {@code null}
   * @return the resolved condition, or {@code null} when this strategy does not
   * apply or the value should not contribute a predicate
   * @since 5.0
   */
  @Nullable
  Condition resolve(EntityMetadata entityMetadata, EntityProperty entityProperty,
          Object value, ValueNormalizer valueNormalizer);

  /**
   * Resolve a condition for a property whose value is {@code null}.
   *
   * <p>Whether a {@code null} value contributes an {@code IS NULL} predicate is
   * a strategy decision. The default implementation declines, leaving the
   * property out of the query.
   *
   * @param entityMetadata the metadata of the entity being queried, never {@code null}
   * @param entityProperty the mapped entity property
   * @return the resolved condition, or {@code null} when the strategy does not
   * apply and the property should not contribute a predicate
   * @since 5.0
   */
  default @Nullable Condition resolve(EntityMetadata entityMetadata, EntityProperty entityProperty) {
    return null;
  }

}