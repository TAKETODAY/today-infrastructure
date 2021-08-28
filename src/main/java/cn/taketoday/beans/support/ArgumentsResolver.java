/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.Env;
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Value;
import cn.taketoday.context.loader.StrategiesDetector;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;

/**
 * BeanFactory supported Executable Arguments-Resolver
 *
 * @author TODAY 2021/8/22 21:59
 * @see BeanFactory
 * @see Value
 * @see Env
 * @since 4.0
 */
public class ArgumentsResolver {
  public static ArgumentsResolver sharedInstance = new ArgumentsResolver();

  @Nullable
  private BeanFactory beanFactory;

  private ArgumentsResolvingStrategy[] resolvingStrategies;
  private ExpressionEvaluator expressionEvaluator = ContextUtils.getExpressionEvaluator();

  public ArgumentsResolver() {
    this(StrategiesDetector.getSharedInstance());
  }

  public ArgumentsResolver(StrategiesDetector strategiesDetector) {
    final List<ArgumentsResolvingStrategy> strategies
            = strategiesDetector.getStrategies(ArgumentsResolvingStrategy.class);

    Collections.addAll(strategies,
                       new MapArgumentsResolver(),
                       new ArrayArgumentsResolver(),
                       new CollectionArgumentsResolver(),
                       new ObjectSupplierArgumentsResolver(),
                       new EnvExecutableArgumentsResolver(),
                       new ValueExecutableArgumentsResolver(),
                       new AutowiredArgumentsResolver()
    );
    setResolvingStrategies(strategies);
  }

  /**
   * resolving parameter using default {@link #beanFactory}
   *
   * @since 4.0
   */
  public Object[] resolve(final Executable executable) {
    return resolve(executable, beanFactory);
  }

  /**
   * Resolve parameters list
   *
   * @param executable
   *         Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory
   *         Bean factory
   *
   * @return Parameter list objects
   *
   * @since 2.1.2
   */
  public Object[] resolve(final Executable executable, @Nullable final BeanFactory beanFactory) {
    return resolve(executable, beanFactory, null);
  }

  public Object[] resolve(final Executable executable, @Nullable Object[] providedArgs) {
    return resolve(executable, beanFactory, providedArgs);
  }

  /**
   * Resolve parameters list
   *
   * @param executable
   *         Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory
   *         Bean factory
   * @param providedArgs
   *         provided args
   *
   * @return Parameter list objects
   *
   * @since 3.0
   */
  public Object[] resolve(
          final Executable executable, @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    Assert.notNull(executable, "Executable must not be null");
    final int parameterLength = executable.getParameterCount();
    if (parameterLength != 0) {
      // parameter list
      final Object[] args = new Object[parameterLength];
      int i = 0;
      for (final Parameter parameter : executable.getParameters()) {
        Object argument = findProvidedArgument(parameter, providedArgs);
        if (argument == null) {
          argument = getResolver(parameter, beanFactory).resolve(parameter, beanFactory);
        }
        args[i++] = argument;
      }
      return args;
    }
    return null;
  }

  public static Object findProvidedArgument(Parameter parameter, @Nullable Object[] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      final Class<?> parameterType = parameter.getType();
      for (final Object providedArg : providedArgs) {
        if (parameterType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  /**
   * @throws ConfigurationException
   *         target parameter resolving not supports in this context
   */
  public ArgumentsResolvingStrategy getResolver(
          final Parameter parameter, @Nullable BeanFactory beanFactory) {
    for (final ArgumentsResolvingStrategy resolver : resolvingStrategies) {
      if (resolver.supports(parameter, beanFactory)) {
        return resolver;
      }
    }
    throw new ConfigurationException(
            "Target parameter:[" + parameter + "] not supports in this context.");
  }

  public void setEvaluator(ExpressionEvaluator expressionEvaluator) {
    Assert.notNull(expressionEvaluator, "expressionEvaluator must not null");
    this.expressionEvaluator = expressionEvaluator;
  }

  public ExpressionEvaluator getEvaluator() {
    return expressionEvaluator;
  }

  public ArgumentsResolvingStrategy[] getResolvingStrategies() {
    return resolvingStrategies;
  }

  public void setResolvingStrategies(ArgumentsResolvingStrategy... strategies) {
    Assert.notNull(strategies, "ArgumentsResolvingStrategies must not null");
    resolvingStrategies = OrderUtils.reversedSort(strategies);
  }

  /**
   * @since 4.0
   */
  public void setResolvingStrategies(List<ArgumentsResolvingStrategy> resolvers) {
    Assert.notNull(resolvers, "ExecutableParameterResolvers must not null");
    ArgumentsResolvingStrategy[] array = resolvers.toArray(new ArgumentsResolvingStrategy[0]);
    resolvingStrategies = OrderUtils.reversedSort(array);
  }

  public void addResolvingStrategies(ArgumentsResolvingStrategy... strategies) {
    if (ObjectUtils.isNotEmpty(strategies)) {
      if (resolvingStrategies != null) {
        List<ArgumentsResolvingStrategy> newResolvers = new ArrayList<>();
        Collections.addAll(newResolvers, resolvingStrategies);
        Collections.addAll(newResolvers, strategies);
        setResolvingStrategies(newResolvers);
      }
      else {
        setResolvingStrategies(strategies);
      }
    }
  }

  /**
   * @since 4.0
   */
  public void addResolvingStrategies(List<ArgumentsResolvingStrategy> strategies) {
    if (!CollectionUtils.isEmpty(strategies)) {
      if (resolvingStrategies != null) {
        List<ArgumentsResolvingStrategy> newStrategies = new ArrayList<>();
        Collections.addAll(newStrategies, resolvingStrategies);
        newStrategies.addAll(strategies);
        setResolvingStrategies(newStrategies);
      }
      else {
        setResolvingStrategies(strategies);
      }
    }
  }

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  // ArgumentsResolvingStrategy

  private final class EnvExecutableArgumentsResolver implements ArgumentsResolvingStrategy {

    @Override
    public boolean supports(Parameter parameter, BeanFactory beanFactory) {
      return parameter.isAnnotationPresent(Env.class);
    }

    @Override
    public Object resolve(Parameter parameter, BeanFactory beanFactory) {
      return expressionEvaluator.evaluate(parameter.getAnnotation(Env.class), parameter.getType());
    }
  }

  private final class ValueExecutableArgumentsResolver implements ArgumentsResolvingStrategy {

    @Override
    public boolean supports(Parameter parameter, BeanFactory beanFactory) {
      return parameter.isAnnotationPresent(Value.class);
    }

    @Override
    public Object resolve(Parameter parameter, BeanFactory beanFactory) {
      return expressionEvaluator.evaluate(parameter.getAnnotation(Value.class), parameter.getType());
    }
  }
}
