/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.cache.interceptor;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.cache.Cache;
import infra.context.expression.AnnotatedElementKey;
import infra.context.expression.CachedExpressionEvaluator;
import infra.expression.EvaluationContext;
import infra.expression.Expression;
import infra.expression.spel.support.StandardEvaluationContext;

/**
 * Utility class handling the EL expression parsing.
 * Meant to be used as a reusable, thread-safe component.
 *
 * <p>Performs internal caching for performance reasons
 * using {@link AnnotatedElementKey}.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

  /**
   * Indicate that there is no result variable.
   */
  public static final Object NO_RESULT = new Object();

  /**
   * Indicate that the result variable cannot be used at all.
   */
  public static final Object RESULT_UNAVAILABLE = new Object();

  /**
   * The name of the variable holding the result object.
   */
  public static final String RESULT_VARIABLE = "result";

  private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

  private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<>(64);

  private final StandardEvaluationContext shared;

  CacheOperationExpressionEvaluator(StandardEvaluationContext shared) {
    this.shared = shared;
  }

  /**
   * Create an {@link EvaluationContext}.
   *
   * @param caches the current caches
   * @param method the method
   * @param args the method arguments
   * @param target the target object
   * @param targetClass the target class
   * @param result the return value (can be {@code null}) or
   * {@link #NO_RESULT} if there is no return at this time
   * @return the evaluation context
   */
  public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches, Method method,
          Object[] args, Object target, Class<?> targetClass, Method targetMethod, @Nullable Object result) {

    var rootObject = new CacheExpressionRootObject(caches, method, args, target, targetClass);
    var evaluationContext = new CacheEvaluationContext(rootObject, targetMethod, args, parameterNameDiscoverer, shared);

    if (result == RESULT_UNAVAILABLE) {
      evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
    }
    else if (result != NO_RESULT) {
      evaluationContext.setVariable(RESULT_VARIABLE, result);
    }

    return evaluationContext;
  }

  @Nullable
  public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
    return getExpression(keyCache, methodKey, keyExpression).getValue(evalContext);
  }

  public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
    return Boolean.TRUE.equals(
            getExpression(conditionCache, methodKey, conditionExpression)
                    .getValue(evalContext, Boolean.class)
    );
  }

  public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
    return Boolean.TRUE.equals(
            getExpression(unlessCache, methodKey, unlessExpression)
                    .getValue(evalContext, Boolean.class)
    );
  }

  /**
   * Clear all caches.
   */
  void clear() {
    this.keyCache.clear();
    this.conditionCache.clear();
    this.unlessCache.clear();
  }

}
