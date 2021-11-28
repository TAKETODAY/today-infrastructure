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

package cn.taketoday.cache.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.cache.CacheExpressionContext;
import cn.taketoday.cache.DefaultCacheKey;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/23 13:39</a>
 * @since 4.0
 */
public class CacheExpressionOperations {

  // @since 4.0
  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  private ExpressionContext expressionContext;
  private ExpressionFactory expressionFactory = ExpressionFactory.getSharedInstance();

  static final ConcurrentReferenceHashMap<MethodKey, String[]> ARGS_NAMES_CACHE = new ConcurrentReferenceHashMap<>(128);
  static final ConcurrentReferenceHashMap<MethodKey, CacheConfiguration> CACHE_OPERATION = new ConcurrentReferenceHashMap<>(128);

  static final Function<MethodKey, CacheConfiguration> CACHE_OPERATION_FUNCTION = target -> {

    Method method = target.targetMethod;
    Class<? extends Annotation> annClass = target.annotationClass;

    // Find target method [annClass] AnnotationAttributes
    Class<?> declaringClass = method.getDeclaringClass();
    MergedAnnotation<? extends Annotation> annotation = MergedAnnotations.from(method).get(annClass);
    if (!annotation.isPresent()) {
      annotation = MergedAnnotations.from(declaringClass).get(annClass);
      if (!annotation.isPresent()) {
        throw new IllegalStateException("Unexpected exception has occurred, may be it's a bug");
      }
    }

    CacheConfiguration configuration =
            AnnotationUtils.injectAttributes(annotation, annClass, new CacheConfiguration(annClass));

    CacheConfig cacheConfig = AnnotationUtils.getAnnotation(declaringClass, CacheConfig.class);
    if (cacheConfig != null) {
      configuration.mergeCacheConfigAttributes(cacheConfig);
    }
    return configuration;
  };

  public CacheExpressionOperations() {
    this(new StandardExpressionContext(ExpressionFactory.getSharedInstance()));
  }

  public CacheExpressionOperations(ExpressionContext expressionContext) {
    this.expressionContext = expressionContext;
  }

  /**
   * Resolve {@link Annotation} from given {@link Annotation} {@link Class}
   *
   * @return {@link Annotation} instance
   */
  public CacheConfiguration getConfig(MethodKey methodKey) {
    return CACHE_OPERATION.computeIfAbsent(methodKey, CACHE_OPERATION_FUNCTION);
  }

  /**
   * Create a key for the target method
   *
   * @param key Key expression
   * @param ctx Cache el ctx
   * @param invocation Target Method Invocation
   * @return Cache key
   */
  public Object createKey(@Nullable String key, CacheExpressionContext ctx, MethodInvocation invocation) {
    return StringUtils.isEmpty(key)
           ? new DefaultCacheKey(invocation.getArguments())
           : expressionFactory.createValueExpression(ctx, key, Object.class).getValue(ctx);
  }

  /**
   * Test condition Expression
   *
   * @param condition condition expression
   * @param context Cache EL Context
   * @return returns If pass the condition
   */
  public boolean passCondition(@Nullable String condition, CacheExpressionContext context) {
    return StringUtils.isEmpty(condition) || //if its empty returns true
            (Boolean) expressionFactory.createValueExpression(context, condition, Boolean.class)
                    .getValue(context);
  }

  /**
   * Test unless Expression
   *
   * @param unless unless express
   * @param result method return value
   * @param context Cache el context
   */
  public boolean allowPutCache(@Nullable String unless, Object result, CacheExpressionContext context) {
    if (StringUtils.isNotEmpty(unless)) {
      context.putBean(Constant.KEY_RESULT, result);
      return !(Boolean) expressionFactory.createValueExpression(context, unless, Boolean.class).getValue(context);
    }
    return true;
  }

  /**
   * Prepare parameter names
   *
   * @param beans The mapping
   * @param arguments Target {@link Method} parameters
   */
  public void prepareParameterNames(
          MethodKey methodKey, Object[] arguments, Map<String, Object> beans) {
    String[] names = ARGS_NAMES_CACHE.computeIfAbsent(methodKey, this::getParameterNames);
    for (int i = 0; i < names.length; i++) {
      beans.put(names[i], arguments[i]);
    }
  }

  private String[] getParameterNames(MethodKey target) {
    return parameterNameDiscoverer.getParameterNames(target.targetMethod);
  }

  public CacheExpressionContext prepareContext(
          MethodKey methodKey, MethodInvocation invocation) {
    HashMap<String, Object> beans = new HashMap<>();
    prepareParameterNames(methodKey, invocation.getArguments(), beans);
    beans.put(Constant.KEY_ROOT, invocation);// ${root.target} for target instance ${root.method}
    return new CacheExpressionContext(expressionContext, beans);
  }

  //

  public void setExpressionFactory(ExpressionFactory expressionFactory) {
    Assert.notNull(expressionFactory, "'expressionFactory' is required");
    this.expressionFactory = expressionFactory;
  }

  public ExpressionFactory getExpressionFactory() {
    return expressionFactory;
  }

  public void setExpressionContext(ExpressionContext expressionContext) {
    Assert.notNull(expressionContext, "shared ExpressionContext is required");
    this.expressionContext = expressionContext;
  }

  public ExpressionContext getExpressionContext() {
    return expressionContext;
  }

  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parameterNameDiscoverer, "ParameterNameDiscoverer is required");
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return parameterNameDiscoverer;
  }
}
