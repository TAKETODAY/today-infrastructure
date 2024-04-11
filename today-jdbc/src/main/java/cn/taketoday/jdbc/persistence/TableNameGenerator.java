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

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultTableNameGenerator
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
      String name = generateTableName(entityClass);
      if (name == null) {
        return next.generateTableName(entityClass);
      }
      return name;
    };
  }

  // Static Factory Methods

  /**
   *
   */
  static DefaultTableNameGenerator defaultStrategy() {
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

    class ForAnnotation implements TableNameGenerator {

      @Override
      public String generateTableName(Class<?> entityClass) {
        MergedAnnotations annotations = MergedAnnotations.from(entityClass);
        var annotation = annotations.get(annotationType);
        if (annotation.isPresent()) {
          String name = annotation.getString(attributeName);
          if (StringUtils.hasText(name)) {
            return name;
          }
        }

        var ref = annotations.get(EntityRef.class);
        if (ref.isPresent()) {
          Class<?> classValue = ref.getClassValue();
          return generateTableName(classValue);
        }
        return null;
      }
    }

    return new ForAnnotation();
  }
}
