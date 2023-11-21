/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.cache.interceptor.AbstractCacheInvoker;
import cn.taketoday.cache.interceptor.BasicOperation;
import cn.taketoday.cache.interceptor.CacheAspectSupport;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for JSR-107 caching aspects, such as the {@link JCacheInterceptor}
 * or an AspectJ aspect.
 *
 * <p>Use the Framework caching abstraction for cache-related operations. No JSR-107
 * {@link javax.cache.Cache} or {@link javax.cache.CacheManager} are required to
 * process standard JSR-107 cache annotations.
 *
 * <p>The {@link JCacheOperationSource} is used for determining caching operations
 *
 * <p>A cache aspect is serializable if its {@code JCacheOperationSource} is serializable.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CacheAspectSupport
 * @see KeyGeneratorAdapter
 * @see CacheResolverAdapter
 * @since 4.0
 */
public class JCacheAspectSupport extends AbstractCacheInvoker implements InitializingBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private JCacheOperationSource cacheOperationSource;

  @Nullable
  private CacheResultInterceptor cacheResultInterceptor;

  @Nullable
  private CachePutInterceptor cachePutInterceptor;

  @Nullable
  private CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor;

  @Nullable
  private CacheRemoveAllInterceptor cacheRemoveAllInterceptor;

  private boolean initialized = false;

  /**
   * Set the CacheOperationSource for this cache aspect.
   */
  public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
    Assert.notNull(cacheOperationSource, "JCacheOperationSource is required");
    this.cacheOperationSource = cacheOperationSource;
  }

  /**
   * Return the CacheOperationSource for this cache aspect.
   */
  public JCacheOperationSource getCacheOperationSource() {
    Assert.state(this.cacheOperationSource != null, "The 'cacheOperationSource' property is required: " +
            "If there are no cacheable methods, then don't use a cache aspect.");
    return this.cacheOperationSource;
  }

  @Override
  public void afterPropertiesSet() {
    getCacheOperationSource();

    this.cacheResultInterceptor = new CacheResultInterceptor(getErrorHandler());
    this.cachePutInterceptor = new CachePutInterceptor(getErrorHandler());
    this.cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor(getErrorHandler());
    this.cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor(getErrorHandler());

    this.initialized = true;
  }

  @Nullable
  protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
    // Check whether aspect is enabled to cope with cases where the AJ is pulled in automatically
    if (this.initialized) {
      Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
      JCacheOperation<?> operation = getCacheOperationSource().getCacheOperation(method, targetClass);
      if (operation != null) {
        CacheOperationInvocationContext<?> context =
                createCacheOperationInvocationContext(target, args, operation);
        return execute(context, invoker);
      }
    }

    return invoker.invoke();
  }

  @SuppressWarnings("unchecked")
  private CacheOperationInvocationContext<?> createCacheOperationInvocationContext(
          Object target, Object[] args, JCacheOperation<?> operation) {

    return new DefaultCacheInvocationContext<>(
            (JCacheOperation<Annotation>) operation, target, args);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private Object execute(CacheOperationInvocationContext<?> context, CacheOperationInvoker invoker) {
    CacheOperationInvoker adapter = new CacheOperationInvokerAdapter(invoker);
    BasicOperation operation = context.getOperation();

    if (operation instanceof CacheResultOperation) {
      Assert.state(this.cacheResultInterceptor != null, "No CacheResultInterceptor");
      return this.cacheResultInterceptor.invoke(
              (CacheOperationInvocationContext<CacheResultOperation>) context, adapter);
    }
    else if (operation instanceof CachePutOperation) {
      Assert.state(this.cachePutInterceptor != null, "No CachePutInterceptor");
      return this.cachePutInterceptor.invoke(
              (CacheOperationInvocationContext<CachePutOperation>) context, adapter);
    }
    else if (operation instanceof CacheRemoveOperation) {
      Assert.state(this.cacheRemoveEntryInterceptor != null, "No CacheRemoveEntryInterceptor");
      return this.cacheRemoveEntryInterceptor.invoke(
              (CacheOperationInvocationContext<CacheRemoveOperation>) context, adapter);
    }
    else if (operation instanceof CacheRemoveAllOperation) {
      Assert.state(this.cacheRemoveAllInterceptor != null, "No CacheRemoveAllInterceptor");
      return this.cacheRemoveAllInterceptor.invoke(
              (CacheOperationInvocationContext<CacheRemoveAllOperation>) context, adapter);
    }
    else {
      throw new IllegalArgumentException("Cannot handle " + operation);
    }
  }

  /**
   * Execute the underlying operation (typically in case of cache miss) and return
   * the result of the invocation. If an exception occurs it will be wrapped in
   * a {@code ThrowableWrapper}: the exception can be handled or modified but it
   * <em>must</em> be wrapped in a {@code ThrowableWrapper} as well.
   *
   * @param invoker the invoker handling the operation being cached
   * @return the result of the invocation
   * @see CacheOperationInvoker#invoke()
   */
  @Nullable
  protected Object invokeOperation(CacheOperationInvoker invoker) {
    return invoker.invoke();
  }

  private class CacheOperationInvokerAdapter implements CacheOperationInvoker {

    private final CacheOperationInvoker delegate;

    public CacheOperationInvokerAdapter(CacheOperationInvoker delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke() throws ThrowableWrapper {
      return invokeOperation(this.delegate);
    }
  }

}
