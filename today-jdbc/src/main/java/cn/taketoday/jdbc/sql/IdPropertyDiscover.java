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
import java.util.Objects;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:48
 */
public interface IdPropertyDiscover {

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
    return beanProperty -> {
      return isIdProperty(beanProperty) || next.isIdProperty(beanProperty);
    };
  }

  static IdPropertyDiscover composite(IdPropertyDiscover... discovers) {
    Assert.notNull(discovers, "IdPropertyDiscover is required");
    return composite(List.of(discovers));
  }

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

  static IdPropertyDiscover forPropertyName(String name) {
    Assert.notNull(name, "property-name is required");
    return property -> {
      return Objects.equals(name, property.getName());
    };
  }

  /**
   * use {@link Id}
   */
  static IdPropertyDiscover forIdAnnotation() {
    return forAnnotation(Id.class);
  }

  static IdPropertyDiscover forAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType is required");
    return property -> property.isAnnotationPresent(annotationType);
  }

}
