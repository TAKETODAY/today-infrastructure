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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.core.annotation.MergedAnnotation;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.annotation.WhereIsNull;
import infra.persistence.sql.Restriction;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:43
 */
public class DefaultConditionStrategy implements PropertyConditionStrategy {

  @Override
  public @Nullable Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value) {
    if (value instanceof String string && StringUtils.isBlank(string)) {
      return null;
    }
    return new Condition(value, Restriction.equal(entityProperty.getColumnName()), entityProperty, logicalAnd);
  }

  @Override
  public @Nullable Condition resolve(boolean logicalAnd, EntityProperty entityProperty) {
    MergedAnnotation<WhereIsNull> annotation = entityProperty.getAnnotation(WhereIsNull.class);
    if (!annotation.isPresent()) {
      return null;
    }
    boolean not = annotation.getBoolean("not");
    Restriction restriction = not
            ? Restriction.isNotNull(entityProperty.getColumnName())
            : Restriction.isNull(entityProperty.getColumnName());
    return new Condition(null, restriction, entityProperty, logicalAnd) {

      @Override
      public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
        return parameterIndex;
      }
    };
  }

}
