/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.aspectj.annotation;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.aop.aspectj.AspectInstanceFactory;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.annotation.OrderUtils;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * {@link AspectInstanceFactory} implementation
 * backed by a Framework {@link BeanFactory}.
 *
 * <p>Note that this may instantiate multiple times if using a prototype,
 * which probably won't give the semantics you expect.
 * Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory
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
    else {
      // No singleton guarantees from the factory -> let's lock locally.
      return this;
    }
  }

  /**
   * Determine the order for this factory's target aspect, either
   * an instance-specific order expressed through implementing the
   * {@link Ordered} interface (only
   * checked for singleton beans), or an order expressed through the
   * {@link Order} annotation
   * at the class level.
   *
   * @see Ordered
   * @see Order
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
