/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import java.lang.annotation.Annotation;
import java.util.List;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:19
 */
public interface TableNameGenerator {

  /**
   * Generate table name from {@code entityClass}
   *
   * @param entityClass entity-class
   * @return table-name
   */
  @Nullable
  String generateTableName(Class<?> entityClass);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default TableNameGenerator and(TableNameGenerator next) {
    return entityClass -> {
      String columnName = generateTableName(entityClass);
      if (columnName == null) {
        return next.generateTableName(entityClass);
      }
      return columnName;
    };
  }

  // static

  static TableNameGenerator composite(TableNameGenerator... discovers) {
    Assert.notNull(discovers, "TableNameGenerator is required");
    return composite(List.of(discovers));
  }

  static TableNameGenerator composite(List<TableNameGenerator> generators) {
    Assert.notNull(generators, "TableNameGenerator is required");
    return entityClass -> {
      for (TableNameGenerator generator : generators) {
        String columnName = generator.generateTableName(entityClass);
        if (columnName != null) {
          return columnName;
        }
      }
      return null;
    };
  }

  /**
   *
   */
  static TableNameGenerator entityClassName() {
    return new DefaultTableNameGenerator();
  }

  /**
   * use {@link Table#name()}
   */
  static TableNameGenerator forTableAnnotation() {
    return forAnnotation(Table.class);
  }

  /**
   * Use input {@code annotationType} to resolve table name
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link TableNameGenerator}
   * @see MergedAnnotation#getString(String)
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Use input {@code annotationType} and {@code attributeName} to resolve table name
   *
   * @param annotationType Annotation type
   * @param attributeName the attribute name
   * @return Annotation based {@link TableNameGenerator}
   * @see MergedAnnotation#getString(String)
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");
    return entityClass -> {
      var annotation = MergedAnnotations.from(entityClass).get(annotationType);
      if (annotation.isPresent()) {
        String tableName = annotation.getString(attributeName);
        if (StringUtils.hasText(tableName)) {
          return tableName;
        }
      }

      return null;
    };
  }
}
