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

package cn.taketoday.aop.aspectj.annotation;

import java.io.Serializable;

import cn.taketoday.aop.aspectj.AspectInstanceFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link AspectInstanceFactory} implementation
 * backed by a Framework {@link cn.taketoday.beans.factory.BeanFactory}.
 *
 * <p>Note that this may instantiate multiple times if using a prototype,
 * which probably won't give the semantics you expect.
 * Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see cn.taketoday.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

  private final BeanFactory beanFactory;

  private final String name;

  private final AspectMetadata aspectMetadata;

  /**
   * Create a BeanFactoryAspectInstanceFactory. AspectJ will be called to
   * introspect to create AJType metadata using the type returned for the
   * given bean name from the BeanFactory.
   *
   * @param beanFactory the BeanFactory to obtain instance(s) from
   * @param name the name of the bean
   */
  public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
    this(beanFactory, name, null);
  }

  /**
   * Create a BeanFactoryAspectInstanceFactory, providing a type that AspectJ should
   * introspect to create AJType metadata. Use if the BeanFactory may consider the type
   * to be a subclass (as when using CGLIB), and the information should relate to a superclass.
   *
   * @param beanFactory the BeanFactory to obtain instance(s) from
   * @param name the name of the bean
   * @param type the type that should be introspected by AspectJ
   * ({@code null} indicates resolution through {@link BeanFactory#getType} via the bean name)
   */
  public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, @Nullable Class<?> type) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    Assert.notNull(name, "Bean name is required");
    this.beanFactory = beanFactory;
    this.name = name;
    Class<?> resolvedType = type;
    if (type == null) {
      resolvedType = beanFactory.getType(name);
      Assert.notNull(resolvedType, "Unresolvable bean type - explicitly specify the aspect class");
    }
    this.aspectMetadata = new AspectMetadata(resolvedType, name);
  }

  @Override
  public Object getAspectInstance() {
    return this.beanFactory.getBean(this.name);
  }

  @Override
  @Nullable
  public ClassLoader getAspectClassLoader() {
    return (this.beanFactory instanceof ConfigurableBeanFactory ?
            ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
            ClassUtils.getDefaultClassLoader());
  }

  @Override
  public AspectMetadata getAspectMetadata() {
    return this.aspectMetadata;
  }

  @Override
  @Nullable
  public Object getAspectCreationMutex() {
    if (this.beanFactory.isSingleton(this.name)) {
      // Rely on singleton semantics provided by the factory -> no local lock.
      return null;
    }
    else if (this.beanFactory instanceof ConfigurableBeanFactory) {
      // No singleton guarantees from the factory -> let's lock locally but
      // reuse the factory's singleton lock, just in case a lazy dependency
      // of our advice bean happens to trigger the singleton lock implicitly...
      return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
    }
    else {
      return this;
    }
  }

  /**
   * Determine the order for this factory's target aspect, either
   * an instance-specific order expressed through implementing the
   * {@link cn.taketoday.core.Ordered} interface (only
   * checked for singleton beans), or an order expressed through the
   * {@link cn.taketoday.core.annotation.Order} annotation
   * at the class level.
   *
   * @see cn.taketoday.core.Ordered
   * @see cn.taketoday.core.annotation.Order
   */
  @Override
  public int getOrder() {
    Class<?> type = this.beanFactory.getType(this.name);
    if (type != null) {
      if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
        return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
      }
      return OrderUtils.getOrder(type, LOWEST_PRECEDENCE);
    }
    return LOWEST_PRECEDENCE;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": bean name '" + this.name + "'";
  }

}
