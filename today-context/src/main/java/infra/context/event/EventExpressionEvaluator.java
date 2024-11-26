/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.context.expression.AnnotatedElementKey;
import infra.context.expression.CachedExpressionEvaluator;
import infra.context.expression.MethodBasedEvaluationContext;
import infra.expression.Expression;
import infra.expression.spel.support.StandardEvaluationContext;

/**
 * Utility class for handling SpEL expression parsing for application events.
 * <p>Meant to be used as a reusable, thread-safe component.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CachedExpressionEvaluator
 * @since 4.0
 */
class EventExpressionEvaluator extends CachedExpressionEvaluator {

  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

  private final StandardEvaluationContext shared;

  EventExpressionEvaluator(StandardEvaluationContext shared) {
    this.shared = shared;
  }

  /**
   * Determine if the condition defined by the specified expression evaluates
   * to {@code true}.
   */
  public boolean condition(String conditionExpression, Object event,
          Method targetMethod, AnnotatedElementKey methodKey, Object[] args) {

    var root = new EventExpressionRootObject(event, args);
    var evaluationContext = new MethodBasedEvaluationContext(root, targetMethod, args, parameterNameDiscoverer, shared);

    return Boolean.TRUE.equals(
            getExpression(conditionCache, methodKey, conditionExpression)
                    .getValue(evaluationContext, Boolean.class)
    );
  }

}
