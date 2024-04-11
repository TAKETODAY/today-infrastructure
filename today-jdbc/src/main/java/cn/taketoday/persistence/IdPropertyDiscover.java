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

package cn.taketoday.persistence;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;

/**
 * Determine ID property
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:48
 */
public interface IdPropertyDiscover {

  String DEFAULT_ID_PROPERTY = "id";

  /**
   * determine that {@code property} is an ID property
   *
   * @param property candidate
   * @return is an ID property or not
   */
  boolean isIdProperty(BeanProperty property);

  // static

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default IdPropertyDiscover and(IdPropertyDiscover next) {
    return property -> isIdProperty(property) || next.isIdProperty(property);
  }

  /**
   * composite pattern
   */
  static IdPropertyDiscover composite(IdPropertyDiscover... discovers) {
    Assert.notNull(discovers, "IdPropertyDiscover is required");
    return composite(List.of(discovers));
  }

  /**
   * composite pattern
   */
  static IdPropertyDiscover composite(List<IdPropertyDiscover> discovers) {
    Assert.notNull(discovers, "IdPropertyDiscover is required");
    return beanProperty -> {

      for (IdPropertyDiscover discover : discovers) {
        if (discover.isIdProperty(beanProperty)) {
          return true;
        }
      }

      return false;
    };
  }

  /**
   * @param name property name
   */
  static IdPropertyDiscover forPropertyName(String name) {
    Assert.notNull(name, "property-name is required");
    return property -> Objects.equals(name, property.getName());
  }

  /**
   * use {@link Id}
   * <p>
   * Can be meta present
   */
  static IdPropertyDiscover forIdAnnotation() {
    return forAnnotation(Id.class);
  }

  /**
   * Use input {@code annotationType} to determine id column
   * <p>
   * Can be meta present
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link IdPropertyDiscover}
   */
  static IdPropertyDiscover forAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType is required");
    return property -> MergedAnnotations.from(property, property.getAnnotations()).isPresent(annotationType);
  }

}
