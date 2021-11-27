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

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * <p>Example usage:
 *
 * <pre>
 * public class Car {
 *   &#064;Inject <b>@Named("driver")</b> Seat driverSeat;
 *   &#064;Inject <b>@Named("passenger")</b> Seat passengerSeat;
 *   ...
 * }
 * </pre>
 * <pre>
 * public class Car {
 *   &#064;Autowired("driver") Seat driverSeat;
 *   &#064;Autowired("passenger") Seat passengerSeat;
 *   &#064;Autowired("passenger1") @Required Seat passengerSeat1; // throw exception if not found
 *   &#064;Autowired(value = "passenger2", required = true) Seat passengerSeat2; // throw exception if not found
 *   ...
 * }
 * </pre>
 * <pre>
 * public class Car {
 *   &#064;Resource(name = "driver") Seat driverSeat;
 *   &#064;Resource(name = "passenger") Seat passengerSeat;
 *   ...
 * }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/20 22:00</a>
 * @see jakarta.inject.Inject
 * @see jakarta.annotation.Resource
 * @since 4.0
 */
public class InjectableDependencyResolvingStrategy
        extends InjectableAnnotationsSupport implements DependencyResolvingStrategy {

  private QualifierRetriever qualifierRetriever = QualifierRetrievers.shared;

  protected boolean supportsDependency(
          InjectionPoint injectionPoint, DependencyResolvingContext context) {
    BeanFactory beanFactory = context.getBeanFactory();
    return beanFactory != null && !context.hasDependency()
            && supportsInternal(injectionPoint, context);
  }

  protected boolean supportsInternal(
          InjectionPoint injectionPoint, DependencyResolvingContext context) {
    return true;
  }

  @Override
  public void resolveDependency(
          InjectionPoint injectionPoint, DependencyResolvingContext context) {
    if (supportsDependency(injectionPoint, context)) {
      BeanFactory beanFactory = context.getBeanFactory();
      if (injectionPoint.isProperty()) {
        if (isInjectable(injectionPoint)) {
          resolveInternal(injectionPoint, beanFactory, context);
        }
      }
      else {
        resolveInternal(injectionPoint, beanFactory, context);
      }
    }
  }

  protected void resolveInternal(
          InjectionPoint injectionPoint, BeanFactory beanFactory, DependencyResolvingContext context) {
    Object bean = getBean(beanFactory, injectionPoint);
    if (bean == null) {
      if (injectionPoint.isRequired()) { // if it is required
        throw new NoSuchBeanDefinitionException(
                "[" + injectionPoint + "] is required and there isn't a ["
                        + injectionPoint.getDependencyType() + "] bean", (Throwable) null);
      }
    }
    context.setDependency(bean);
  }

  protected Object getBean(BeanFactory beanFactory, InjectionPoint injectionPoint) {
    String beanName = qualifierRetriever.retrieve(injectionPoint);
    if (StringUtils.hasText(beanName)) {
      // use name and bean type to get bean
      return beanFactory.getBean(beanName, injectionPoint.getDependencyType());
    }
    else {
      return beanFactory.getBean(injectionPoint.getDependencyType());
    }
  }

  public void setQualifierRetriever(QualifierRetriever qualifierRetriever) {
    Assert.notNull(qualifierRetriever, "'qualifierRetriever' is required");
    this.qualifierRetriever = qualifierRetriever;
  }

  public QualifierRetriever getQualifierRetriever() {
    return qualifierRetriever;
  }

}
