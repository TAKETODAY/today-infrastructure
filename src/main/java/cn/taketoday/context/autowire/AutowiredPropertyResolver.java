/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.autowire;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.beans.factory.BeanReferencePropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.util.ClassUtils;

import static cn.taketoday.core.annotation.AnnotationUtils.isPresent;

/**
 * This {@link PropertyValueResolver} supports field that annotated
 * {@link Autowired}, {@link Required}
 * <p>Example usage:
 *
 * <pre>
 * public class Car {
 *   &#064;Autowired("driver") Seat driverSeat;
 *   &#064;Autowired("passenger") Seat passengerSeat;
 *   &#064;Autowired("passenger1") &#064;Required Seat passengerSeat1; // required
 *   &#064;Autowired(value = "passenger2", required = true) Seat passengerSeat2; // required
 *   ...
 * }
 * </pre>
 *
 * @author TODAY 2018-08-04 15:56
 */
public class AutowiredPropertyResolver implements PropertyValueResolver {

  private static final Class<? extends Annotation> INJECT_CLASS = ClassUtils.load("jakarta.inject.Inject");
  private static final Class<? extends Annotation> RESOURCE_CLASS = ClassUtils.load("jakarta.annotation.Resource");

  // @since 3.0 Required
  public static boolean isRequired(AnnotatedElement element, @Nullable MergedAnnotation<Autowired> autowired) {
    return (autowired == null || !autowired.isPresent() || autowired.getBoolean("required"))
            || AnnotatedElementUtils.isAnnotated(element, Required.class);
  }

  public static boolean isInjectable(AnnotatedElement element) {
    return isPresent(element, Autowired.class)
            || isPresent(element, RESOURCE_CLASS)
            || isPresent(element, INJECT_CLASS);
  }

  @Nullable
  @Override
  public PropertySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotations annotations = MergedAnnotations.from(property);
    MergedAnnotation<Autowired> autowired = annotations.get(Autowired.class);
    if (autowired.isPresent()) {
      boolean required = isRequired(property, autowired);
      String referenceName = autowired.getString(MergedAnnotation.VALUE);
      return new BeanReferencePropertySetter(referenceName, required, property);
    }
    return null;
  }

}
