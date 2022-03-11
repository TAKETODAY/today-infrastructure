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

package cn.taketoday.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.context.expression.CachedExpressionEvaluator;
import cn.taketoday.context.expression.MethodBasedEvaluationContext;
import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 17:31
 */
class EventExpressionEvaluator extends CachedExpressionEvaluator {

  private final Map<ExpressionKey, ValueExpression> conditionCache = new ConcurrentHashMap<>(64);

  /**
   * Determine if the condition defined by the specified expression evaluates
   * to {@code true}.
   */
  public boolean condition(
          String conditionExpression, Object event, Method targetMethod,
          AnnotatedElementKey methodKey, Object[] args, @Nullable BeanFactory beanFactory) {

    EventRootObject eventRootObject = new EventRootObject(event, args);
    StandardExpressionContext evaluationContext = new MethodBasedEvaluationContext(
            eventRootObject, targetMethod, args, parameterNameDiscoverer);

    if (beanFactory != null) {
      evaluationContext.addResolver(
              new BeanNameExpressionResolver(new BeanFactoryResolver(beanFactory)));
    }

    return Boolean.TRUE.equals(getExpression(conditionCache, methodKey, conditionExpression)
            .getValue(evaluationContext));
  }

  record EventRootObject(Object event, Object[] args) {

    public Object getEvent() {
      return event;
    }

    public Object[] getArgs() {
      return args;
    }
  }

}

