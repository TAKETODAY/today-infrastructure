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

import cn.taketoday.beans.ArgumentsNotSupportedException;
import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.autowire.ArrayArgumentsResolver;
import cn.taketoday.context.autowire.CollectionArgumentsResolver;
import cn.taketoday.context.autowire.MapArgumentsResolver;
import cn.taketoday.context.autowire.ObjectSupplierArgumentsResolver;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.lang.Value;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  /**
   * @param parameter Target method {@link Parameter}
   * @param resolvingContext resolving context never {@code null}
   * @return arg never returns {@link NullValue}
   * @throws ArgumentsNotSupportedException parameter not support, configure a ArgumentsResolvingStrategy
   */
  @Override
  public Object resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    Object argument = findProvidedArgument(parameter, resolvingContext.getProvidedArgs());
    if (argument == null) {
      for (ArgumentsResolvingStrategy resolver : resolvingStrategies(resolvingContext)) {
        // if returns a {@link NullValue} indicates that returns null object
        argument = resolver.resolveArgument(parameter, resolvingContext);
        if (argument == NullValue.INSTANCE) {
          return null;
        }
        // null indicates not supports
        if (argument != null) {
          return argument;
        }

        if (resolver instanceof ArgumentsResolvingComposite) {
          return null;
        }
      }
      throw new ArgumentsNotSupportedException(
              "Target parameter:[" + parameter + "] declaring executable: " + parameter.getDeclaringExecutable() + " not supports in this context: " + resolvingContext);
    }
    return argument;
  }

  @Nullable
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

  private static final class EnvExecutableArgumentsResolver implements ArgumentsResolvingStrategy {
    private final ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.getSharedInstance();

    @Nullable
    @Override
    public Object resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(parameter, Env.class);
      if (attributes != null) {
        ExpressionInfo expressionInfo = new ExpressionInfo(attributes, true);
        return expressionEvaluator.evaluate(expressionInfo, parameter.getType());
      }
      return null;
    }
  }

  private static final class ValueExecutableArgumentsResolver implements ArgumentsResolvingStrategy {
    private final ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.getSharedInstance();

    @Nullable
    @Override
    public Object resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
      AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(parameter, Value.class);
      if (attributes != null) {
        ExpressionInfo expressionInfo = new ExpressionInfo(attributes, false);
        return expressionEvaluator.evaluate(expressionInfo, parameter.getType());
      }
      return null; // next resolver
    }

  }
}
