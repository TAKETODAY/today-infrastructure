/**
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
package cn.taketoday.cache.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheExpressionContext;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.DefaultCacheKey;
import cn.taketoday.cache.NoSuchCacheException;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.util.ConcurrentCache;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-02-27 19:03
 */
public abstract class AbstractCacheInterceptor
        extends CacheOperations implements MethodInterceptor, Ordered {

  private CacheManager cacheManager;
  private final OrderedSupport ordered = new OrderedSupport();

  public AbstractCacheInterceptor() { }

  public AbstractCacheInterceptor(CacheManager cacheManager) {
    setCacheManager(cacheManager);
  }

  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public final CacheManager getCacheManager() {
    return cacheManager;
  }

  @Override
  public int getOrder() {
    return ordered.getOrder();
  }

  public void setOrder(final int order) {
    ordered.setOrder(order);
  }

  /**
   * Prepare {@link Cache} name
   *
   * @param method
   *         Target method
   * @param cacheName
   *         {@link CacheConfig#cacheName()}
   *
   * @return A not empty cache name
   */
  protected String prepareCacheName(final Method method, final String cacheName) {
    // if cache name is empty use declaring class full name
    if (StringUtils.isEmpty(cacheName)) {
      return method.getDeclaringClass().getName();
    }
    return cacheName;
  }

  protected Cache getCache(final String name, final CacheConfig cacheConfig) {
    return getCacheManager().getCache(name, cacheConfig);
  }

  /**
   * Obtain a Target method's {@link Cache} object
   *
   * @param method
   *         Target method
   * @param cacheConfig
   *         {@link CacheConfig}
   *
   * @return {@link Cache}
   *
   * @throws NoSuchCacheException
   *         If there isn't a {@link Cache}
   */
  protected final Cache obtainCache(final Method method, final CacheConfig cacheConfig) {
    final String name = prepareCacheName(method, cacheConfig.cacheName());
    final Cache cache = getCache(name, cacheConfig);
    if (cache == null) {
      throw new NoSuchCacheException(name);
    }
    return cache;
  }

  /**
   * @see cn.taketoday.cache.annotation.ProxyCachingConfiguration
   */
  @Autowired
  public void initCacheInterceptor(ApplicationContext context) {
    if (getCacheManager() == null) {
      setCacheManager(context.getBean(CacheManager.class));
    }
    if (getExceptionResolver() == null) {
      setExceptionResolver(context.getBean(CacheExceptionResolver.class));
    }

    Assert.state(getCacheManager() != null, "You must provide a 'CacheManager'");
    Assert.state(getExceptionResolver() != null, "You must provide a 'CacheExceptionResolver'");
  }

  // ExpressionOperations
  //-----------------------------------------------------

  abstract static class Operations {
    // @since 4.0
    static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    static final StandardExpressionContext SHARED_EL_CONTEXT;
    static final ExpressionFactory EXPRESSION_FACTORY = ExpressionFactory.getSharedInstance();
    static final ConcurrentCache<MethodKey, String[]> ARGS_NAMES_CACHE = new ConcurrentCache<>(512);
    static final ConcurrentCache<MethodKey, CacheConfiguration> CACHE_OPERATION = new ConcurrentCache<>(512);
    static final Function<MethodKey, String[]> ARGS_NAMES_FUNCTION =
            target -> parameterNameDiscoverer.getParameterNames(target.targetMethod);

    static final Function<MethodKey, CacheConfiguration> CACHE_OPERATION_FUNCTION = target -> {

      final Method method = target.targetMethod;
      final Class<? extends Annotation> annClass = target.annotationClass;

      // Find target method [annClass] AnnotationAttributes
      AnnotationAttributes attributes = AnnotationUtils.getAttributes(annClass, method);
      final Class<?> declaringClass = method.getDeclaringClass();
      if (attributes == null) {
        attributes = AnnotationUtils.getAttributes(annClass, declaringClass);
        if (attributes == null) {
          throw new IllegalStateException("Unexpected exception has occurred, may be it's a bug");
        }
      }

      final CacheConfiguration configuration = //
              AnnotationUtils.injectAttributes(attributes, annClass, new CacheConfiguration(annClass));

      final CacheConfig cacheConfig = AnnotationUtils.getAnnotation(CacheConfig.class, declaringClass);
      if (cacheConfig != null) {
        configuration.mergeCacheConfigAttributes(cacheConfig);
      }
      return configuration;
    };

    static {
      final ApplicationContext lastStartupContext = ContextUtils.getLastStartupContext();
      if (lastStartupContext != null) {
        SHARED_EL_CONTEXT = lastStartupContext
                .getEnvironment()
                .getExpressionProcessor()
                .getManager()
                .getContext();
      }
      else {
        SHARED_EL_CONTEXT = new StandardExpressionContext(EXPRESSION_FACTORY);
      }
    }

    // methods
    //------------------------------------------

    /**
     * Resolve {@link Annotation} from given {@link Annotation} {@link Class}
     *
     * @return {@link Annotation} instance
     */
    public static CacheConfiguration prepareAnnotation(final MethodKey methodKey) {
      return CACHE_OPERATION.get(methodKey, CACHE_OPERATION_FUNCTION);
    }

    /**
     * Create a key for the target method
     *
     * @param key
     *         Key expression
     * @param ctx
     *         Cache el ctx
     * @param invocation
     *         Target Method Invocation
     *
     * @return Cache key
     */
    static Object createKey(final String key,
                            final CacheExpressionContext ctx,
                            final MethodInvocation invocation) {

      return key.isEmpty()
             ? new DefaultCacheKey(invocation.getArguments())
             : EXPRESSION_FACTORY.createValueExpression(ctx, key, Object.class).getValue(ctx);
    }

    /**
     * Test condition Expression
     *
     * @param condition
     *         condition expression
     * @param context
     *         Cache EL Context
     *
     * @return returns If pass the condition
     */
    static boolean isConditionPassing(final String condition, final CacheExpressionContext context) {
      return StringUtils.isEmpty(condition) || //if its empty returns true
              (Boolean) EXPRESSION_FACTORY.createValueExpression(context, condition, Boolean.class)
                      .getValue(context);
    }

    /**
     * Test unless Expression
     *
     * @param unless
     *         unless express
     * @param result
     *         method return value
     * @param context
     *         Cache el context
     */
    static boolean allowPutCache(final String unless, final Object result, final CacheExpressionContext context) {

      if (StringUtils.isNotEmpty(unless)) {
        context.putBean(Constant.KEY_RESULT, result);
        return !(Boolean) EXPRESSION_FACTORY.createValueExpression(context, unless, Boolean.class).getValue(context);
      }
      return true;
    }

    /**
     * Prepare parameter names
     *
     * @param beans
     *         The mapping
     * @param arguments
     *         Target {@link Method} parameters
     */
    static void prepareParameterNames(final MethodKey methodKey,
                                      final Object[] arguments,
                                      final Map<String, Object> beans) //
    {
      final String[] names = ARGS_NAMES_CACHE.get(methodKey, ARGS_NAMES_FUNCTION);
      for (int i = 0; i < names.length; i++) {
        beans.put(names[i], arguments[i]);
      }
    }

    static CacheExpressionContext prepareELContext(final MethodKey methodKey,
                                                   final MethodInvocation invocation) {
      final HashMap<String, Object> beans = new HashMap<>();
      prepareParameterNames(methodKey, invocation.getArguments(), beans);
      beans.put(Constant.KEY_ROOT, invocation);// ${root.target} for target instance ${root.method}
      return new CacheExpressionContext(SHARED_EL_CONTEXT, beans);
    }

  }

  // MethodKey
  // -----------------------------

  static final class MethodKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int hash;
    private final transient Method targetMethod;
    private final Class<? extends Annotation> annotationClass;

    public MethodKey(Method targetMethod, Class<? extends Annotation> annotationClass) {
      this.targetMethod = targetMethod;
      this.hash = targetMethod.hashCode();
      this.annotationClass = annotationClass;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof MethodKey))
        return false;
      final MethodKey methodKey = (MethodKey) o;
      return hash == methodKey.hash
              && Objects.equals(targetMethod, methodKey.targetMethod)
              && Objects.equals(annotationClass, methodKey.annotationClass);
    }

    @Override
    public int hashCode() {
      return this.hash;
    }
  }

}
