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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanInstantiatorFactory;
import cn.taketoday.beans.support.ReflectiveInstantiatorFactory;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * bean-factory aware instantiator
 *
 * @author TODAY 2021/10/4 22:26
 * @see DependencyInjector
 * @since 4.0
 */
@Experimental
public class BeanFactoryAwareInstantiator {
  public static final String BEAN_NAME = "beanFactoryAwareInstantiator";

  private final BeanFactory beanFactory;
  private BeanInstantiatorFactory instantiatorFactory = ReflectiveInstantiatorFactory.INSTANCE;

  public BeanFactoryAwareInstantiator(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "dependencyInjector is required");
    this.beanFactory = beanFactory;
  }

  public <T> T instantiate(Class<T> beanClass) {
    return instantiate(beanClass, (Object[]) null);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param providedArgs User provided arguments
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @throws ConstructorNotFoundException If beanClass has no suitable constructor
   * @see BeanUtils#obtainConstructor(Class)
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> T instantiate(Class<T> beanClass, @Nullable Object[] providedArgs) {
    if (beanFactory instanceof AutowireCapableBeanFactory acb) {
      return acb.createBean(beanClass);
    }
    Constructor<T> constructor = BeanUtils.obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return (T) instantiatorFactory.newInstantiator(constructor).instantiate();
    }
    Object[] args = beanFactory.getInjector().resolveArguments(constructor, providedArgs);
    BeanInstantiator beanInstantiator = instantiatorFactory.newInstantiator(constructor);
    return (T) beanInstantiator.instantiate(args);
  }

  public void setInstantiatorFactory(@Nullable BeanInstantiatorFactory instantiatorFactory) {
    if (instantiatorFactory == null) {
      instantiatorFactory = ReflectiveInstantiatorFactory.INSTANCE;
    }
    this.instantiatorFactory = instantiatorFactory;
  }

  public BeanInstantiatorFactory getInstantiatorFactory() {
    return instantiatorFactory;
  }

  // static factory-method

  /**
   * for {@code Function<Class<T>, T> }
   */
  public static <T> Function<Class<T>, T> forFunction(BeanFactory beanFactory) {
    if (beanFactory instanceof AutowireCapableBeanFactory acb) {
      return acb::createBean;
    }
    return forFunction(BeanFactoryAwareInstantiator.from(beanFactory));
  }

  /**
   * for {@code Function<Class<T>, T> }
   */
  public static <T> Function<Class<T>, T> forFunction(BeanFactoryAwareInstantiator instantiator) {
    Assert.notNull(instantiator, "instantiator is required");
    return instantiator::instantiate;
  }

  public static BeanFactoryAwareInstantiator from(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    BeanFactoryAwareInstantiator instantiator = find(beanFactory);
    if (instantiator == null) {
      synchronized(beanFactory) {
        instantiator = find(beanFactory);
        if (instantiator == null) {
          instantiator = new BeanFactoryAwareInstantiator(beanFactory);
          if (beanFactory instanceof SingletonBeanRegistry singletonBeanRegistry) {
            singletonBeanRegistry.registerSingleton(BEAN_NAME, instantiator);
          }
          else if (beanFactory instanceof BeanDefinitionRegistry registry) {
            RootBeanDefinition definition = new RootBeanDefinition(BeanFactoryAwareInstantiator.class);
            definition.setSynthetic(true);
            definition.setEnableDependencyInjection(false);
            definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            definition.setInstanceSupplier(SingletonSupplier.valueOf(instantiator));

            registry.registerBeanDefinition(BEAN_NAME, definition);
          }
          else {
            throw new IllegalArgumentException("beanFactory is not a SingletonBeanRegistry nor BeanDefinitionRegistry");
          }
        }
      }
    }
    return instantiator;
  }

  @Nullable
  private static BeanFactoryAwareInstantiator find(BeanFactory beanFactory) {
    return BeanFactoryUtils.findLocal(beanFactory, BEAN_NAME, BeanFactoryAwareInstantiator.class);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param beanFactory bean factory
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see BeanUtils#obtainConstructor(Class)
   */
  public static <T> T instantiate(Class<T> beanClass, @Nullable DependencyInjectorProvider beanFactory) {
    return instantiate(beanClass, beanFactory, null);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param providedArgs User provided arguments
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see BeanUtils#obtainConstructor(Class)
   */
  public static <T> T instantiate(Class<T> beanClass,
          @Nullable DependencyInjectorProvider injectorProvider, @Nullable Object[] providedArgs) {
    Constructor<T> constructor = BeanUtils.obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return BeanUtils.newInstance(constructor);
    }
    Assert.notNull(injectorProvider, "resolverProvider is required");
    return injectorProvider.getInjector().inject(constructor, providedArgs);
  }

}
