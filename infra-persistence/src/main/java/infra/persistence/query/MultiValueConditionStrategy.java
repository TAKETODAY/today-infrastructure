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

package infra.persistence.query;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.annotation.Between;
import infra.persistence.annotation.In;
import infra.persistence.annotation.NotBetween;
import infra.persistence.annotation.NotIn;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.Restrictions;

/**
 * A {@link PropertyConditionStrategy} that turns an example property annotated
 * with {@link In @In}, {@link NotIn @NotIn}, {@link Between @Between} or
 * {@link NotBetween @NotBetween} into a multi-value predicate.
 *
 * <ul>
 *   <li>{@code @In} — renders {@code column IN (?, ?, ...)}</li>
 *   <li>{@code @NotIn} — renders {@code column NOT IN (?, ?, ...)}</li>
 *   <li>{@code @Between} — renders {@code column BETWEEN ? AND ?}</li>
 *   <li>{@code @NotBetween} — renders {@code column NOT BETWEEN ? AND ?}</li>
 * </ul>
 *
 * <p>The target column is the property's mapped column, resolved through
 * {@link EntityProperty#getColumnName()} — which honours the
 * {@link infra.persistence.annotation.Column @Column} meta-annotation
 * declared on {@code @In} / {@code @Between}.
 *
 * <p>An empty {@code IN} list, a {@link Between} value with fewer than two
 * elements, or a range with a {@code null} bound, contributes no predicate.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see In
 * @see Between
 * @see Range
 * @since 5.0
 */
public class MultiValueConditionStrategy implements PropertyConditionStrategy {

  @Override
  public @Nullable Condition resolve(EntityMetadata metadata, EntityProperty property,
          Object value, ValueNormalizer valueNormalizer) {
    if (property.isPresent(In.class)) {
      return resolveIn(property, value);
    }
    if (property.isPresent(NotIn.class)) {
      return resolveNotIn(property, value);
    }
    if (property.isPresent(Between.class)) {
      return resolveBetween(property, value);
    }
    if (property.isPresent(NotBetween.class)) {
      return resolveNotBetween(property, value);
    }
    return null;
  }

  private @Nullable Condition resolveIn(EntityProperty property, Object value) {
    List<Object> values = toList(value);
    if (values.isEmpty()) {
      return null;
    }
    return new MultiValueCondition(property, Restrictions.in(property.getColumnName(), values.size()), values);
  }

  private @Nullable Condition resolveNotIn(EntityProperty property, Object value) {
    List<Object> values = toList(value);
    if (values.isEmpty()) {
      return null;
    }
    return new MultiValueCondition(property, Restrictions.notIn(property.getColumnName(), values.size()), values);
  }

  private @Nullable Condition resolveBetween(EntityProperty property, Object value) {
    return resolveBetween(property, value, false);
  }

  private @Nullable Condition resolveNotBetween(EntityProperty property, Object value) {
    return resolveBetween(property, value, true);
  }

  private @Nullable Condition resolveBetween(EntityProperty property, Object value, boolean negative) {
    Object lower;
    Object upper;
    if (value instanceof Range range) {
      lower = range.lower();
      upper = range.upper();
    }
    else {
      List<Object> bounds = toList(value);
      if (bounds.size() != 2) {
        return null;
      }
      lower = bounds.get(0);
      upper = bounds.get(1);
    }
    if (lower == null || upper == null) {
      return null;
    }
    Restriction restriction = negative
            ? Restrictions.notBetween(property.getColumnName())
            : Restrictions.between(property.getColumnName());
    return new MultiValueCondition(property, restriction, List.of(lower, upper));
  }

  /**
   * Convert an {@link Iterable} or array value into a list.
   */
  private static List<Object> toList(Object value) {
    if (value instanceof Collection<?> collection) {
      return new ArrayList<>(collection);
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> result = new ArrayList<>();
      for (Object element : iterable) {
        result.add(element);
      }
      return result;
    }
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      List<Object> result = new ArrayList<>(length);
      for (int i = 0; i < length; i++) {
        result.add(Array.get(value, i));
      }
      return result;
    }
    return new ArrayList<>(Arrays.asList(value));
  }

}