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

import cn.taketoday.beans.factory.BeanReferencePropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

import static cn.taketoday.context.autowire.AutowiredPropertyResolver.isRequired;

/**
 * String-based {@linkplain Qualifier qualifier} PropertyValueResolver.
 *
 * <p>Example usage:
 *
 * <pre>
 *   public class Car {
 *     &#064;Inject <b>@Named("driver")</b> Seat driverSeat;
 *     &#064;Inject <b>@Named("passenger")</b> Seat passengerSeat;
 *     ...
 *   }</pre>
 *
 * @author TODAY 2021/10/31 11:53
 * @see Inject
 * @see Named
 * @since 4.0
 */
public class JSR330InjectPropertyResolver implements PropertyValueResolver {

  @Nullable
  @Override
  public PropertySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotations annotations = MergedAnnotations.from(property);
    // @Inject
    MergedAnnotation<? extends Annotation> inject = annotations.get(Inject.class);
    if (inject.isPresent()) {
      // @Named
      MergedAnnotation<? extends Annotation> named = annotations.get(Named.class);
      if (named.isPresent()) {
        // @since 3.0
        String referenceName = named.getString(MergedAnnotation.VALUE);
        return new BeanReferencePropertySetter(referenceName, isRequired(property, null), property);
      }
      return new BeanReferencePropertySetter(null, isRequired(property, null), property);
    }
    return null;
  }

}
