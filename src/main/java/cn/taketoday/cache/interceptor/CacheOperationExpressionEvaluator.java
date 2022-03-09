/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.cache.Cache;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.CachedExpressionEvaluator;
import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.lang.Nullable;

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

  private final Map<ExpressionKey, ValueExpression> keyCache = new ConcurrentHashMap<>(64);

  private final Map<ExpressionKey, ValueExpression> conditionCache = new ConcurrentHashMap<>(64);

  private final Map<ExpressionKey, ValueExpression> unlessCache = new ConcurrentHashMap<>(64);

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
  public CacheEvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
          Method method, Object[] args, Object target, Class<?> targetClass, Method targetMethod,
          @Nullable Object result, @Nullable BeanFactory beanFactory) {

    CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
            caches, method, args, target, targetClass);

    CacheEvaluationContext context = new CacheEvaluationContext(
            rootObject, targetMethod, args, getParameterNameDiscoverer());
    if (result == RESULT_UNAVAILABLE) {
      context.addUnavailableVariable(RESULT_VARIABLE);
    }
    else if (result != NO_RESULT) {
      context.setVariable(RESULT_VARIABLE, result);
    }
    if (beanFactory != null) {
      context.addResolver(new BeanNameExpressionResolver(beanFactory));
    }
    return context;
  }

  @Nullable
  public Object key(String keyExpression, AnnotatedElementKey methodKey, CacheEvaluationContext evalContext) {
    return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
  }

  public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, CacheEvaluationContext evalContext) {
    return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
            evalContext, Boolean.class)));
  }

  public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, CacheEvaluationContext evalContext) {
    return (Boolean.TRUE.equals(getExpression(this.unlessCache, methodKey, unlessExpression).getValue(
            evalContext, Boolean.class)));
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
