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

package cn.taketoday.context.annotation;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.AbstractArgumentsResolvingStrategy;
import cn.taketoday.beans.ArgumentsNotSupportedException;
import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.Env;
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Value;
import cn.taketoday.context.annotation.autowire.ArrayArgumentsResolver;
import cn.taketoday.context.annotation.autowire.CollectionArgumentsResolver;
import cn.taketoday.context.annotation.autowire.MapArgumentsResolver;
import cn.taketoday.context.annotation.autowire.ObjectSupplierArgumentsResolver;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/10/3 22:38
 * @since 4.0
 */
public class ArgumentsResolvingComposite implements ArgumentsResolvingStrategy {
  private static final Logger log = LoggerFactory.getLogger(ArgumentsResolvingComposite.class);

  private final StrategiesDetector strategiesDetector;
  private final ArrayList<ArgumentsResolvingStrategy> resolvingStrategies = new ArrayList<>();

  public ArgumentsResolvingComposite() {
    this.strategiesDetector = TodayStrategies.getDetector();
  }

  public ArgumentsResolvingComposite(@Nullable StrategiesDetector strategiesDetector) {
    Assert.notNull(strategiesDetector, "StrategiesDetector must not be null");
    this.strategiesDetector = strategiesDetector;
  }

  @Override
  public Object resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    Object argument = findProvidedArgument(parameter, resolvingContext.getProvidedArgs());
    if (argument == null) {
      for (ArgumentsResolvingStrategy resolver : resolvingStrategies(resolvingContext)) {
        argument = resolver.resolveArgument(parameter, resolvingContext);
        if (argument != null) {
          return argument;
        }
      }
      throw new ArgumentsNotSupportedException(
              "Target parameter:[" + parameter + "] not supports in this context: " + resolvingContext);
    }
    return argument;
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

  public ArrayList<ArgumentsResolvingStrategy> resolvingStrategies(
          ArgumentsResolvingContext resolvingContext) {
    if (resolvingStrategies.isEmpty()) {
      log.debug("initialize arguments-resolving-strategies");
      BeanFactory beanFactory = resolvingContext.getBeanFactory();
      List<ArgumentsResolvingStrategy> strategies = getStrategies(strategiesDetector, beanFactory);
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
    return resolvingStrategies;
  }

  private static List<ArgumentsResolvingStrategy> getStrategies(
          StrategiesDetector strategiesDetector, @Nullable BeanFactory beanFactory) {
    if (beanFactory == null) {
      return strategiesDetector.getStrategies(ArgumentsResolvingStrategy.class);
    }
    return strategiesDetector.getStrategies(ArgumentsResolvingStrategy.class, beanFactory);
  }

  public ArrayList<ArgumentsResolvingStrategy> getResolvingStrategies() {
    return resolvingStrategies;
  }

  public void setResolvingStrategies(ArgumentsResolvingStrategy... strategies) {
    resolvingStrategies.clear();
    addResolvingStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void setResolvingStrategies(List<ArgumentsResolvingStrategy> strategies) {
    resolvingStrategies.clear();
    addResolvingStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void addResolvingStrategies(ArgumentsResolvingStrategy... strategies) {
    if (ObjectUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

  /**
   * @since 4.0
   */
  public void addResolvingStrategies(List<ArgumentsResolvingStrategy> strategies) {
    if (CollectionUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

  //---------------------------------------------------------------------
  // Implementations of ArgumentsResolvingStrategy interface
  //---------------------------------------------------------------------

  private static final class EnvExecutableArgumentsResolver extends AbstractArgumentsResolvingStrategy {
    private final ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.getSharedInstance();

    @Override
    protected boolean supportsArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      return parameter.isAnnotationPresent(Env.class);
    }

    @Override
    protected Object resolveInternal(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      return expressionEvaluator.evaluate(parameter.getAnnotation(Env.class), parameter.getType());
    }
  }

  private static final class ValueExecutableArgumentsResolver extends AbstractArgumentsResolvingStrategy {
    private final ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.getSharedInstance();

    @Override
    protected boolean supportsArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      return parameter.isAnnotationPresent(Value.class);
    }

    @Override
    protected Object resolveInternal(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      return expressionEvaluator.evaluate(parameter.getAnnotation(Value.class), parameter.getType());
    }

  }
}
