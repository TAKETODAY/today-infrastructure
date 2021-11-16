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

import java.util.Optional;

import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.dependency.BeanReferenceDependencySetter;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ResolvableType;

/**
 * Optional<T> The injected bean will return Optional.empty() if the container does not exist
 *
 * <p>Example usage:
 *
 * <pre>
 *   public class Car {
 *     &#064;Inject <b>@Named("driver")</b> Optional&lt;Seat&gt; driverSeat;
 *     &#064;Inject <b>@Named("passenger")</b> Optional&lt;Seat&gt; passengerSeat;
 *     ...
 *   }</pre>
 *
 * @author TODAY 2021/10/31 14:09
 * @see java.util.Optional
 * @since 4.0
 */
public class OptionalPropertyValueResolver
        extends AbstractPropertyValueResolver implements PropertyValueResolver {

  @Override
  protected boolean supportsProperty(PropertyResolvingContext context, BeanProperty property) {
    return property.getType() == Optional.class
            && AutowiredPropertyResolver.isInjectable(property);
  }

  @Override
  protected DependencySetter resolveInternal(PropertyResolvingContext context, BeanProperty property) {
    ResolvableType resolvableType = ResolvableType.fromField(property.getField());
    if (resolvableType.hasGenerics()) {
      ResolvableType generic = resolvableType.getGeneric(0);
      Class<?> resolve = generic.resolve();
      return new BeanReferenceDependencySetter("", false, property, resolve) {

        @Override
        protected Object resolveValue(ConfigurableBeanFactory beanFactory) {
          Object value = resolveBeanReference(beanFactory);
          return Optional.ofNullable(value);
        }
      };
    }
    // Usage error
    throw new PropertyException("cannot determine the bean type of '" + property + "' In -> " + property.getDeclaringClass());
  }

}
