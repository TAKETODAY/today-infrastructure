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
import infra.persistence.EntityProperty;
import infra.persistence.Like;
import infra.persistence.PrefixLike;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.SuffixLike;
import infra.persistence.sql.Restriction;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:48
 */
public class FuzzyQueryConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value) {
    MergedAnnotation<Like> annotation = entityProperty.getAnnotation(Like.class);
    if (annotation.isPresent()) {
      // handle string

      if (value instanceof String string) {
        // trim
        if (annotation.getBoolean("trim")) {
          string = string.trim();
        }
        if (StringUtils.hasText(string)) {
          // get column name
          String column = annotation.getStringValue();
          if (Constant.DEFAULT_NONE.equals(column)) {
            column = entityProperty.columnName;
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

          value = string;
          return new Condition(value, new LikeRestriction(column), entityProperty, logicalAnd);
        }
      }
    }
    return null;
  }

  static final class LikeRestriction implements Restriction {

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
