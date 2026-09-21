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

package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import infra.persistence.Condition;
import infra.persistence.EntityProperty;
import infra.persistence.Identifier;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.Range;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Between;
import infra.persistence.annotation.In;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.Restrictions;
import infra.util.Assert;

/**
 * A {@link PropertyConditionStrategy} that turns an example property annotated
 * with {@link In @In} or {@link Between @Between} into a multi-value predicate.
 *
 * <ul>
 *   <li>{@code @In} — the property holds an {@link Iterable} or array; renders
 *   {@code column IN (?, ?, ...)} binding every element.</li>
 *   <li>{@code @Between} — the property holds a {@link Range} or a two-element
 *   array / collection; renders {@code column BETWEEN ? AND ?} binding the
 *   lower and upper bound.</li>
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
  public @Nullable Condition resolve(EntityProperty property, Object value, ValueNormalizer valueNormalizer) {
    if (property.isPresent(In.class)) {
      return resolveIn(property, value);
    }
    if (property.isPresent(Between.class)) {
      return resolveBetween(property, value);
    }
    return null;
  }

  private @Nullable Condition resolveIn(EntityProperty property, Object value) {
    List<Object> values = toList(value);
    if (values.isEmpty()) {
      return null;
    }
    return new MultiValueCondition(property, in(property.getColumnName(), values.size()), values);
  }

  private @Nullable Condition resolveBetween(EntityProperty property, Object value) {
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
    return new MultiValueCondition(property,
            Restrictions.between(property.getColumnName()), List.of(lower, upper));
  }

  /**
   * Render a {@code column IN (?, ?, ...)} restriction with the given count of
   * placeholders.
   */
  private static Restriction in(Identifier column, int count) {
    Assert.isTrue(count > 0, "IN requires at least one placeholder");
    StringBuilder placeholders = new StringBuilder(count * 2 - 1);
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        placeholders.append(", ");
      }
      placeholders.append('?');
    }
    return new InRestriction(column, placeholders);
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

  /**
   * Renders a {@code column IN (?, ..., ?)} predicate.
   */
  private static final class InRestriction implements Restriction {

    private final Identifier column;

    private final CharSequence placeholders;

    InRestriction(Identifier column, CharSequence placeholders) {
      this.column = column;
      this.placeholders = placeholders;
    }

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      sqlBuffer.append(column.render(platform))
              .append(" IN (")
              .append(placeholders)
              .append(')');
    }
  }

}