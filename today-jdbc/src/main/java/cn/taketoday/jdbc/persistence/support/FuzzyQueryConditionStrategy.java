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
import cn.taketoday.jdbc.persistence.Like;
import cn.taketoday.jdbc.persistence.PrefixLike;
import cn.taketoday.jdbc.persistence.PropertyConditionStrategy;
import cn.taketoday.jdbc.persistence.SuffixLike;
import cn.taketoday.jdbc.persistence.sql.Restriction;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:48
 */
public class FuzzyQueryConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(EntityProperty entityProperty, Object propertyValue) {
    MergedAnnotation<Like> annotation = entityProperty.getAnnotation(Like.class);
    if (annotation.isPresent()) {
      // get column name
      String column = annotation.getStringValue();
      if (Constant.DEFAULT_NONE.equals(column)) {
        column = entityProperty.columnName;
      }

      // handle string

      if (propertyValue instanceof String string) {
        // trim
        if (annotation.getBoolean("trim")) {
          string = string.trim();
        }
        if (entityProperty.isPresent(PrefixLike.class)) {
          string = string + '%';
        }
        else if (entityProperty.isPresent(SuffixLike.class)) {
          string = '%' + string;
        }
        else {
          string = '%' + string + '%';
        }

        propertyValue = string;
      }

      return new Condition(propertyValue, new LikeRestriction(column), entityProperty);
    }

    return null;
  }

  static class LikeRestriction implements Restriction {

    final String columnName;

    LikeRestriction(String columnName) {
      this.columnName = columnName;
    }

    @Override
    public void render(StringBuilder sqlBuffer) {
      sqlBuffer.append('`')
              .append(columnName)
              .append("` like ?");
    }
  }

}
