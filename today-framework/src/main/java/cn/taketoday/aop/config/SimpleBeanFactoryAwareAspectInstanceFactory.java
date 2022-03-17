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

package cn.taketoday.aop.config;

import cn.taketoday.aop.aspectj.AspectInstanceFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Implementation of {@link AspectInstanceFactory} that locates the aspect from the
 * {@link cn.taketoday.beans.factory.BeanFactory} using a configured bean name.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleBeanFactoryAwareAspectInstanceFactory implements AspectInstanceFactory, BeanFactoryAware {

  @Nullable
  private String aspectBeanName;

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Set the name of the aspect bean. This is the bean that is returned when calling
   * {@link #getAspectInstance()}.
   */
  public void setAspectBeanName(String aspectBeanName) {
    this.aspectBeanName = aspectBeanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    Assert.notNull(this.aspectBeanName, "'aspectBeanName' is required");
  }

  /**
   * Look up the aspect bean from the {@link BeanFactory} and returns it.
   *
   * @see #setAspectBeanName
   */
  @Override
  public Object getAspectInstance() {
    Assert.state(this.beanFactory != null, "No BeanFactory set");
    Assert.state(this.aspectBeanName != null, "No 'aspectBeanName' set");
    return this.beanFactory.getBean(this.aspectBeanName);
  }

  @Override
  @Nullable
  public ClassLoader getAspectClassLoader() {
    if (this.beanFactory instanceof ConfigurableBeanFactory) {
      return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
    }
    else {
      return ClassUtils.getDefaultClassLoader();
    }
  }

  @Override
  public int getOrder() {
    if (this.beanFactory != null && this.aspectBeanName != null &&
            this.beanFactory.isSingleton(this.aspectBeanName) &&
            this.beanFactory.isTypeMatch(this.aspectBeanName, Ordered.class)) {
      return ((Ordered) this.beanFactory.getBean(this.aspectBeanName)).getOrder();
    }
    return Ordered.LOWEST_PRECEDENCE;
  }

}
