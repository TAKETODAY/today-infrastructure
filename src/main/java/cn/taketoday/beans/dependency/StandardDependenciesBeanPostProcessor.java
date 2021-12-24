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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.DependencyResolvingFailedException;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * this class is a dependency collecting entrance, process every bean class
 * <p>
 * using DependencyResolvingStrategies resolving dependencies
 * </p>
 *
 * @author TODAY 2021/10/23 22:59
 * @see cn.taketoday.lang.Autowired
 * @see DependencyResolvingStrategy
 * @since 4.0
 */
public class StandardDependenciesBeanPostProcessor
        extends InjectableAnnotationsSupport implements DependenciesBeanPostProcessor, BeanFactoryAware {

  @Nullable
  private StrategiesDetector strategiesDetector;

  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private ArgumentsResolver argumentsResolver;

  @Nullable
  private DependencyResolvingStrategies resolvingStrategies;

  public StandardDependenciesBeanPostProcessor() { }

  public StandardDependenciesBeanPostProcessor(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  //---------------------------------------------------------------------
  // Implementation of DependenciesBeanPostProcessor interface
  //---------------------------------------------------------------------

  @Override
  public void processDependencies(Object bean, BeanDefinition definition) {
    Class<?> beanClass = bean.getClass();
    String beanName = definition.getName();
    HashSet<Method> processedMethods = new HashSet<>();
    ReflectionUtils.doWithFields(beanClass, field -> {
      try {
        Object property = resolveProperty(beanName, field);
        if (property != InjectionPoint.DO_NOT_SET) {
          Method writeMethod = ReflectionUtils.getWriteMethod(field);
          if (writeMethod != null) {
            ReflectionUtils.makeAccessible(writeMethod);
            ReflectionUtils.invokeMethod(writeMethod, bean, property);
            processedMethods.add(writeMethod);
          }
          else {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, property);
          }
        }
      }
      catch (DependencyResolvingFailedException e) {
        throw e;
      }
      catch (BeansException e) {
        throw new DependencyResolvingFailedException(
                "dependency resolving failed for property '" + field.getName() + "'", e);
      }
    }, ReflectionUtils.COPYABLE_FIELDS);

    // process methods
    ReflectionUtils.doWithMethods(beanClass, method -> {
      if (!processedMethods.contains(method) && isInjectable(method)) {
        try {
          Object[] args = argumentsResolver().resolve(method, beanFactory);
          ReflectionUtils.makeAccessible(method);
          ReflectionUtils.invokeMethod(method, bean, args);
        }
        catch (DependencyResolvingFailedException e) {
          throw e;
        }
        catch (BeansException e) {
          throw new DependencyResolvingFailedException(
                  "dependency resolving failed for method '" + method.getName() + "'", e);
        }
      }
    }, ReflectionUtils.USER_DECLARED_METHODS);
  }

  /**
   * Create property value
   *
   * @param beanName current bean name
   * @param property Property
   * @return A resolved dependency
   */
  @Nullable
  public Object resolveProperty(String beanName, Field property) {
    DependencyResolvingContext context = new DependencyResolvingContext(null, beanFactory);
    context.setBeanName(beanName);

    getResolvingStrategies().resolveDependency(new DependencyDescriptor(property, false), context);

    return context.getDependency();
  }

  public DependencyResolvingStrategies getResolvingStrategies() {
    if (resolvingStrategies == null) {
      resolvingStrategies = new DependencyResolvingStrategies();
      initStrategies(resolvingStrategies);
    }
    return resolvingStrategies;
  }

  private void initStrategies(DependencyResolvingStrategies resolvingStrategies) {
    resolvingStrategies.initStrategies(null, beanFactory);
  }

  public void setResolvingStrategies(@Nullable DependencyResolvingStrategies resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
  }

  @Nullable
  public StrategiesDetector getStrategiesDetector() {
    return strategiesDetector;
  }

  public void setStrategiesDetector(@Nullable StrategiesDetector strategiesDetector) {
    this.strategiesDetector = strategiesDetector;
  }

  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setArgumentsResolver(@Nullable ArgumentsResolver argumentsResolver) {
    this.argumentsResolver = argumentsResolver;
  }

  @Nullable
  public ArgumentsResolver getArgumentsResolver() {
    return argumentsResolver;
  }

  @NonNull
  private ArgumentsResolver argumentsResolver() {
    if (argumentsResolver == null) {
      argumentsResolver = beanFactory.getArgumentsResolver();
    }
    return argumentsResolver;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory, "'beanFactory' must be a ConfigurableBeanFactory");
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
  }

}
