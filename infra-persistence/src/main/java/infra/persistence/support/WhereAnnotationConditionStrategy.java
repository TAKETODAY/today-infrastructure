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

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.Condition;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Where;
import infra.persistence.sql.Restrictions;

/**
 * A {@link PropertyConditionStrategy} that turns an entity property annotated
 * with {@link Where @Where} into a SQL predicate.
 *
 * <p>The property value is {@linkplain ValueNormalizer normalized} first. When
 * the {@code @Where} annotation is present, the predicate is built as follows:
 * <ul>
 *   <li>with a {@link Where#value()} SQL fragment — the fragment is rendered
 *   unchanged via {@link Restrictions#plain(CharSequence) plain()};</li>
 *   <li>otherwise with an {@link Where#operator()} — as {@code column <operator> ?}
 *   via {@link Restrictions#forOperator(String, String, String) forOperator()};</li>
 *   <li>otherwise — as {@code column = ?} via {@link Restrictions#equal(String)}.</li>
 * </ul>
 *
 * <p>A property without a {@code @Where} annotation is declined by returning
 * {@code null}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Where
 * @since 4.0 2024/2/25 00:02
 */
public class WhereAnnotationConditionStrategy implements PropertyConditionStrategy {

  /**
   * Resolve a condition for the given property and value.
   *
   * <p>The value is {@linkplain ValueNormalizer normalized} before it is bound.
   * The predicate is chosen from the {@code @Where} annotation as described in
   * the class-level documentation; a property without the annotation yields
   * {@code null}.
   *
   * @param metadata the metadata of the entity being queried, never {@code null}
   * @param property the mapped entity property
   * @param value the property value to evaluate
   * @param valueNormalizer the normalizer for the property, never {@code null}
   * @return the resolved condition, or {@code null} when the property is not
   * annotated with {@code @Where}
   */
  @Override
  public @Nullable Condition resolve(EntityMetadata metadata, EntityProperty property,
          Object value, ValueNormalizer valueNormalizer) {
    // render where clause
    MergedAnnotation<Where> annotation = property.getAnnotation(Where.class);
    if (annotation.isPresent()) {
      value = valueNormalizer.normalize(property, value);
      String restriction = annotation.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(restriction)) {
        return new PropertyCondition(value, Restrictions.plain(restriction), property);
      }
      else {
        String operator = annotation.getString("operator");
        if (Constant.DEFAULT_NONE.equals(operator)) {
          // default to equality operator
          return new PropertyCondition(value,
                  Restrictions.equal(property.getColumnName()), property);
        }
        else {
          return new PropertyCondition(value, Restrictions.forOperator(
                  property.getColumnName(), operator, "?"), property);
        }
      }
    }
    return null;
  }

}
