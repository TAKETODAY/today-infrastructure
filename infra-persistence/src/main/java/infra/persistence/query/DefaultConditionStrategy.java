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

import infra.core.annotation.MergedAnnotation;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.annotation.Between;
import infra.persistence.annotation.In;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.Where;
import infra.persistence.annotation.WhereIsNull;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.Restrictions;
import infra.util.StringUtils;

/**
 * The fallback {@link PropertyConditionStrategy} that turns an example property
 * value into an equality predicate.
 *
 * <p>A blank string declines to contribute a predicate. A {@code null} value on
 * a property annotated with {@link WhereIsNull @NullQuery} produces an
 * {@code IS NULL} / {@code IS NOT NULL} predicate; a {@code null} value on any
 * other property takes no part in the query.
 *
 * <p>A property carrying a dedicated condition annotation ({@link Where @Where},
 * {@link Like @Like} family, {@link In @In} or {@link Between @Between}) is left
 * to the matching strategy and never turned into an equality here, so an
 * intentionally empty multi-value predicate keeps the property out of the query.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:43
 */
final class DefaultConditionStrategy implements PropertyConditionStrategy {

  @Override
  public @Nullable Condition resolve(EntityMetadata metadata, EntityProperty property,
          Object value, ValueNormalizer valueNormalizer) {
    if (value instanceof String string && StringUtils.isBlank(string)) {
      return null;
    }
    value = valueNormalizer.normalize(property, value);
    return new PropertyCondition(value, Restrictions.equal(property.getColumnName()), property);
  }

  @Override
  public @Nullable Condition resolve(EntityMetadata metadata, EntityProperty property) {
    MergedAnnotation<WhereIsNull> annotation = property.getAnnotation(WhereIsNull.class);
    if (!annotation.isPresent()) {
      return null;
    }
    Restriction restriction = annotation.getBoolean("not")
            ? Restrictions.isNotNull(property.getColumnName())
            : Restrictions.isNull(property.getColumnName());
    return new IsNullCondition(restriction);
  }

  /**
   * A condition that renders {@code IS (NOT) NULL} and consumes no parameter.
   */
  private static final class IsNullCondition implements Condition {

    private final Restriction restriction;

    IsNullCondition(Restriction restriction) {
      this.restriction = restriction;
    }

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      restriction.render(platform, sqlBuffer);
    }
  }

}