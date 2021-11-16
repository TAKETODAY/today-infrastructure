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

import cn.taketoday.beans.dependency.BeanReferenceDependencySetter;
import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;
import jakarta.annotation.Resource;

/**
 * <p>Example usage:
 *
 * <pre>
 *   public class Car {
 *     &#064;Resource(name = "driver") Seat driverSeat;
 *     &#064;Resource(name = "passenger") Seat passengerSeat;
 *     ...
 *   }</pre>
 *
 * @author TODAY 2021/10/31 19:14
 * @see Resource
 * @since 4.0
 */
public class JSR250ResourcePropertyValueResolver implements PropertyValueResolver {

  @Nullable
  @Override
  public DependencySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotations annotations = MergedAnnotations.from(property);
    // @Resource
    MergedAnnotation<? extends Annotation> resource = annotations.get(Resource.class);
    if (resource.isPresent()) {
      String referenceName = resource.getString("name");
      return new BeanReferenceDependencySetter(
              referenceName, AutowiredPropertyResolver.isRequired(property, null), property);
    }
    return null;
  }
}
