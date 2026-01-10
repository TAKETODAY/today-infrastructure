/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
