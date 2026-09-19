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
import infra.persistence.Identifier;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.PrefixLike;
import infra.persistence.annotation.SuffixLike;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:48
 */
public class FuzzyQueryConditionStrategy implements PropertyConditionStrategy {

  @Override
  public @Nullable Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value,
          ValueNormalizer valueNormalizer) {
    MergedAnnotation<Like> annotation = entityProperty.getAnnotation(Like.class);
    if (annotation.isPresent()) {
      value = valueNormalizer.normalize(entityProperty, value);

      // handle string
      if (value instanceof String string) {
        if (StringUtils.hasText(string)) {
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
          // get column name
          Identifier column = getColumn(entityProperty, annotation);
          return new Condition(value, new LikeRestriction(column), entityProperty, logicalAnd);
        }
      }
    }
    return null;
  }

  private static Identifier getColumn(EntityProperty property, MergedAnnotation<Like> annotation) {
    String columnText = annotation.getStringValue();
    if (Constant.DEFAULT_NONE.equals(columnText)) {
      return property.getColumnName();
    }
    return Identifier.parse(columnText);
  }

  static final class LikeRestriction implements Restriction {

    final Identifier columnName;

    LikeRestriction(Identifier columnName) {
      this.columnName = columnName;
    }

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      sqlBuffer.append(columnName.render(platform))
              .append(" like ?");
    }

  }

}
