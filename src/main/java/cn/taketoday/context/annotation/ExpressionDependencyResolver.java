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

import java.lang.reflect.Field;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.dependency.DependencyDescriptor;
import cn.taketoday.beans.factory.dependency.DependencyResolvingContext;
import cn.taketoday.beans.factory.dependency.DependencyResolvingStrategy;
import cn.taketoday.beans.factory.support.BeanExpressionContext;
import cn.taketoday.beans.factory.support.BeanExpressionResolver;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.expression.ExpressionEvaluationException;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.StringUtils;

/**
 * for Env and Value
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/18 21:11</a>
 * @since 4.0
 */
public class ExpressionDependencyResolver implements DependencyResolvingStrategy, Ordered {

  private final BeanExpressionContext exprContext;

  @Nullable
  private final BeanExpressionResolver exprResolver;
  private final ConfigurableBeanFactory beanFactory;

  public ExpressionDependencyResolver(ConfigurableBeanFactory beanFactory) {
    this.exprContext = new BeanExpressionContext(beanFactory, null);
    this.exprResolver = beanFactory.getBeanExpressionResolver();
    this.beanFactory = beanFactory;
  }

  @Nullable
  public Object evaluate(ExpressionInfo expr) {
    String value = exprContext.getBeanFactory().resolveEmbeddedValue(expr.getExpression());
    if (!expr.isPlaceholderOnly() && exprResolver != null && value != null) {
      return this.exprResolver.evaluate(value, this.exprContext);
    }
    return value;
  }

  @Override
  public void resolveDependency(DependencyDescriptor descriptor, DependencyResolvingContext context) {
    if (!context.hasDependency()) {
      Env env = descriptor.getAnnotation(Env.class);
      if (env != null) {
        ExpressionInfo expressionInfo = new ExpressionInfo(env);
        expressionInfo.setPlaceholderOnly(true);
        Object evaluate = resolve(descriptor, expressionInfo);
        context.setDependencyResolved(evaluate);
      }
      else {
        Value annotation = descriptor.getAnnotation(Value.class);
        if (annotation != null) {
          ExpressionInfo expressionInfo = new ExpressionInfo(annotation);
          expressionInfo.setPlaceholderOnly(false);
          Object evaluate = resolve(descriptor, expressionInfo);
          context.setDependencyResolved(evaluate);
        }
      }
    }
  }

  private Object resolve(
          DependencyDescriptor injectionPoint, ExpressionInfo expr) {
    String expression = expr.getExpression();
    if (StringUtils.isEmpty(expression)) {
      if (injectionPoint.isProperty()
              && injectionPoint.getMember() instanceof Field property) {
        // use class full name and field name
        expression = PropertyPlaceholderHandler.PLACEHOLDER_PREFIX +
                property.getDeclaringClass().getName() +
                Constant.PACKAGE_SEPARATOR +
                property.getName() +
                PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX;
        expr.setPlaceholderOnly(false);
        expr.setExpression(expression);
      }
    }

    Object value = evaluate(expr);
    if (value == null && injectionPoint.isRequired()) {
      // perform @Required Annotation
      throw new ExpressionEvaluationException(
              "Can't resolve expression on injection-point: [" + injectionPoint +
                      "] with expression: [" + expr.getExpression() + "].");
    }

    ConversionService conversionService = beanFactory.getConversionService();
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService.convert(value, injectionPoint.getDependencyType());
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

}
