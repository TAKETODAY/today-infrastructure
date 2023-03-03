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
import java.util.Set;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;

/**
 * PropertyFilter is to determine witch property not included in
 * an entity
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:29
 */
public interface PropertyFilter {

  /**
   * @param property bean property
   * @return is property not map to a column
   */
  boolean isFiltered(BeanProperty property);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default PropertyFilter and(PropertyFilter next) {
    return beanProperty -> isFiltered(beanProperty) || next.isFiltered(beanProperty);
  }

  /**
   * filter property names
   *
   * @param filteredNames property names not mapping to database column
   */
  static PropertyFilter filteredNames(Set<String> filteredNames) {
    Assert.notEmpty(filteredNames, "filteredNames is empty");
    return property -> filteredNames.contains(property.getName());
  }

  /**
   * Accept any property
   */
  static PropertyFilter acceptAny() {
    return property -> false;
  }

  /**
   * use {@link Transient}
   */
  static PropertyFilter forTransientAnnotation() {
    return forAnnotation(Transient.class);
  }

  /**
   * Use input {@code annotationType} to filter
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link PropertyFilter}
   * @see MergedAnnotation#getString(String)
   */
  static PropertyFilter forAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType is required");
    return property -> {
      var annotation = MergedAnnotations.from(
              property, property.getAnnotations()).get(annotationType);
      return annotation.isPresent();
    };
  }

}
