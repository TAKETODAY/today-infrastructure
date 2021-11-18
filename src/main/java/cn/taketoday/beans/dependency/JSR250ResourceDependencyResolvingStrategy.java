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

package cn.taketoday.beans.dependency;

import java.lang.annotation.Annotation;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.annotation.MergedAnnotation;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/18 20:37</a>
 * @see jakarta.annotation.Resource
 * @since 4.0
 */
public class JSR250ResourceDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  public void resolveDependency(DependencyInjectionPoint injectionPoint, DependencyResolvingContext context) {
    BeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory != null && !context.hasDependency()) {
      // @Resource
      MergedAnnotation<? extends Annotation> resource = injectionPoint.getAnnotation(Resource.class);
      if (resource.isPresent()) {
        String referenceName = resource.getString("name");
        Object retrieve = new BeanReferenceRetriever(referenceName, injectionPoint.isRequired(), injectionPoint.getDependencyType())
                .retrieve(beanFactory);
        context.setDependency(retrieve);
      }
    }

  }

}
