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

package cn.taketoday.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.beans.dependency.DependencyResolvingContext;
import cn.taketoday.beans.dependency.DependencyResolvingStrategies;
import cn.taketoday.beans.dependency.ParameterInjectionPoint;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.lang.Value;
import cn.taketoday.util.ObjectUtils;

/**
 * BeanFactory supported Executable Arguments-Resolver
 * <p>
 * Not Thread Safe
 * </p>
 *
 * @author TODAY 2021/8/22 21:59
 * @see BeanFactory
 * @see Value
 * @see Env
 * @see DependencyResolvingStrategies
 * @see cn.taketoday.beans.dependency.DependencyResolvingStrategy
 * @since 4.0
 */
public class ArgumentsResolver {
  public static volatile ArgumentsResolver shared;

  private final StrategiesDetector strategiesDetector;

  @Nullable
  private BeanFactory beanFactory;
  private DependencyResolvingStrategies resolvingStrategies;

  public ArgumentsResolver() {
    this(TodayStrategies.getDetector());
  }

  public ArgumentsResolver(StrategiesDetector strategiesDetector) {
    this(strategiesDetector, null);
  }

  public ArgumentsResolver(@Nullable BeanFactory beanFactory) {
    this(TodayStrategies.getDetector(), beanFactory);
  }

  public ArgumentsResolver(
          StrategiesDetector strategiesDetector, @Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.strategiesDetector = strategiesDetector;
  }

  /**
   * resolving parameter using default {@link #beanFactory}
   *
   * @since 4.0
   */
  public Object[] resolve(Executable executable) {
    return resolve(executable, beanFactory);
  }

  /**
   * Resolve parameters list
   *
   * @param executable Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory Bean factory
   * @return Parameter list objects
   * @since 2.1.2
   */
  public Object[] resolve(Executable executable, @Nullable BeanFactory beanFactory) {
    return resolve(executable, beanFactory, null);
  }

  public Object[] resolve(Executable executable, @Nullable Object[] providedArgs) {
    return resolve(executable, beanFactory, providedArgs);
  }

  /**
   * Resolve parameters list
   *
   * @param executable Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory Bean factory
   * @param providedArgs provided args
   * @return Parameter list objects
   * @since 3.0
   */
  @Nullable
  public Object[] resolve(
          Executable executable, @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    Assert.notNull(executable, "Executable must not be null");
    int parameterLength = executable.getParameterCount();
    if (parameterLength != 0) {
      // parameter list
      Object[] args = new Object[parameterLength];
      int i = 0;
      for (Parameter parameter : executable.getParameters()) {
        args[i++] = resolve(parameter, beanFactory, providedArgs);
      }
      return args;
    }
    return null;
  }

  @Nullable
  public static Object findProvided(Parameter parameter, @Nullable Object[] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      Class<?> dependencyType = parameter.getType();
      for (final Object providedArg : providedArgs) {
        if (dependencyType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  /**
   * resolve just one Parameter
   */
  public Object resolve(Parameter parameter, @Nullable Object[] providedArgs) {
    return resolve(parameter, beanFactory, providedArgs);
  }

  /**
   * resolve just one Parameter
   */
  public Object resolve(
          Parameter parameter, @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    Object provided = findProvided(parameter, providedArgs);
    if (provided == null) {
      DependencyResolvingContext context =
              new DependencyResolvingContext(parameter.getDeclaringExecutable(), beanFactory);
      ParameterInjectionPoint injectionPoint = new ParameterInjectionPoint(parameter);
      resolvingStrategies().resolveDependency(injectionPoint, context);
      provided = context.getDependency() == ParameterInjectionPoint.DO_NOT_SET
                 ? null : context.getDependency();
    }
    return provided;
  }

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setStrategies(DependencyResolvingStrategies resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
  }

  public DependencyResolvingStrategies resolvingStrategies() {
    if (resolvingStrategies == null) {
      this.resolvingStrategies = new DependencyResolvingStrategies();
      resolvingStrategies.initStrategies(strategiesDetector, beanFactory);
    }
    return resolvingStrategies;
  }

  public DependencyResolvingStrategies getStrategies() {
    return resolvingStrategies;
  }

  /**
   * Get ArgumentsResolver
   *
   * @since 4.0
   */
  @NonNull
  public static ArgumentsResolver getOrShared(@Nullable BeanFactory beanFactory) {
    if (beanFactory != null) {
      return beanFactory.getArgumentsResolver();
    }
    return getSharedInstance();
  }

  public static ArgumentsResolver getSharedInstance() {
    ArgumentsResolver resolver = shared;
    if (resolver == null) {
      synchronized(ArgumentsResolver.class) {
        resolver = shared;
        if (resolver == null) {
          resolver = new ArgumentsResolver();
          shared = resolver;
        }
      }
    }
    return resolver;
  }

}
