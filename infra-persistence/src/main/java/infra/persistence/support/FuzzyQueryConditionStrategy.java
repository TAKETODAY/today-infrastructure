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
import infra.persistence.Condition;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.PrefixLike;
import infra.persistence.annotation.SuffixLike;
import infra.persistence.sql.Restrictions;
import infra.util.StringUtils;

/**
 * A {@link PropertyConditionStrategy} that turns an entity property annotated
 * with {@link Like @Like} into a SQL {@code LIKE} condition.
 *
 * <p>Only a non-empty {@link String} value participates in the query. The value
 * is {@linkplain ValueNormalizer normalized} first, then wrapped with the
 * {@code %}-wildcards matching the annotation combination:
 * <ul>
 *   <li>{@link PrefixLike @PrefixLike} produces {@code "value%"} (prefix match);</li>
 *   <li>{@link SuffixLike @SuffixLike} produces {@code "%value"} (suffix match);</li>
 *   <li>otherwise {@code "%value%"} (substring match).</li>
 * </ul>
 *
 * <p>The predicate targets the column declared by the {@code Like} annotation,
 * falling back to the property's mapped column when none is given. A property
 * without a {@code @Like} annotation, or with a blank or non-string value, is
 * declined by returning {@code null}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Like
 * @see PrefixLike
 * @see SuffixLike
 * @since 4.0 2024/2/28 22:48
 */
public class FuzzyQueryConditionStrategy implements PropertyConditionStrategy {

  @Override
  public @Nullable Condition resolve(EntityProperty entityProperty, Object value, ValueNormalizer valueNormalizer) {
    // handle string
    if (value instanceof String string && StringUtils.hasText(string)) {
      MergedAnnotation<Like> annotation = entityProperty.getAnnotation(Like.class);
      if (annotation.isPresent()) {
        string = (String) valueNormalizer.normalize(entityProperty, value);
        if (entityProperty.isPresent(PrefixLike.class)) {
          string = string + '%';
        }
        else if (entityProperty.isPresent(SuffixLike.class)) {
          string = '%' + string;
        }
        else {
          string = '%' + string + '%';
        }

        // get column name
        return new PropertyCondition(
                string, Restrictions.like(entityProperty.getColumnName()), entityProperty);
      }
    }
    return null;
  }

}
