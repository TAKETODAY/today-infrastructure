/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence.support;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.jdbc.persistence.EntityProperty;
import cn.taketoday.jdbc.persistence.PropertyConditionStrategy;
import cn.taketoday.jdbc.persistence.TrimWhere;
import cn.taketoday.jdbc.persistence.Where;
import cn.taketoday.jdbc.persistence.sql.Restriction;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/25 00:02
 */
public class WhereAnnotationConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(EntityProperty entityProperty, Object propertyValue) {
    if (propertyValue instanceof String string && entityProperty.isPresent(TrimWhere.class)) {
      propertyValue = string.trim();
    }

    // render where clause
    MergedAnnotation<Where> annotation = entityProperty.getAnnotation(Where.class);
    if (annotation.isPresent()) {
      String value = annotation.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(value)) {
        return new Condition(propertyValue, Restriction.plain(value), entityProperty);
      }
      else {
        String condition = annotation.getString("condition");
        if (Constant.DEFAULT_NONE.equals(condition)) {
          return new Condition(propertyValue, Restriction.equal(entityProperty.columnName), entityProperty);
        }
      }
    }
    return null;
  }

}
