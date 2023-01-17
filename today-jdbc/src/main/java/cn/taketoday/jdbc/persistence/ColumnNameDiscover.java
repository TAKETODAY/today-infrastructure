/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence;

import java.lang.annotation.Annotation;
import java.util.List;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.Property;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:03
 */
public interface ColumnNameDiscover {

  /**
   * determine that {@code property} 's column name
   *
   * @param property candidate
   * @return get column name
   */
  @Nullable
  String getColumnName(BeanProperty property);

  // static

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default ColumnNameDiscover and(ColumnNameDiscover next) {
    return property -> {
      String columnName = getColumnName(property);
      if (columnName == null) {
        return next.getColumnName(property);
      }
      return columnName;
    };
  }

  static ColumnNameDiscover composite(ColumnNameDiscover... discovers) {
    Assert.notNull(discovers, "ColumnNameDiscover is required");
    return composite(List.of(discovers));
  }

  static ColumnNameDiscover composite(List<ColumnNameDiscover> discovers) {
    Assert.notNull(discovers, "ColumnNameDiscover is required");
    return property -> {

      for (ColumnNameDiscover discover : discovers) {
        String columnName = discover.getColumnName(property);
        if (columnName != null) {
          return columnName;
        }
      }

      return null;
    };
  }

  /**
   * just use the property-name as the column-name
   */
  static ColumnNameDiscover forPropertyName() {
    return Property::getName;
  }

  /**
   * property-name to underscore (camelCase To Underscore)
   *
   * @see StringUtils#camelCaseToUnderscore(String)
   */
  static ColumnNameDiscover camelCaseToUnderscore() {
    return property -> {
      String propertyName = property.getName();
      return StringUtils.camelCaseToUnderscore(propertyName);
    };
  }

  /**
   * use {@link Column#name()}
   */
  static ColumnNameDiscover forColumnAnnotation() {
    return forAnnotation(Column.class);
  }

  /**
   * Use input {@code annotationType} to resolve column name
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link ColumnNameDiscover}
   * @see MergedAnnotation#getString(String)
   */
  static ColumnNameDiscover forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Use input {@code annotationType} and {@code attributeName} to resolve column name
   *
   * @param annotationType Annotation type
   * @param attributeName the attribute name
   * @return Annotation based {@link ColumnNameDiscover}
   * @see MergedAnnotation#getString(String)
   */
  static ColumnNameDiscover forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    return property -> {
      var annotation = MergedAnnotations.from(
              property, property.getAnnotations()).get(annotationType);

      if (annotation.isPresent()) {
        String columnName = annotation.getString(attributeName);
        if (StringUtils.hasText(columnName)) {
          return columnName;
        }
      }

      return null;
    };
  }

}
