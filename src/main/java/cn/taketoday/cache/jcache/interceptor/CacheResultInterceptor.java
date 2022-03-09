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

package cn.taketoday.cache.jcache.interceptor;

import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionTypeFilter;
import cn.taketoday.util.SerializationUtils;

/**
 * Intercept methods annotated with {@link CacheResult}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
class CacheResultInterceptor extends AbstractKeyCacheInterceptor<CacheResultOperation, CacheResult> {

  public CacheResultInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  @Override
  @Nullable
  protected Object invoke(
          CacheOperationInvocationContext<CacheResultOperation> context, CacheOperationInvoker invoker) {

    CacheResultOperation operation = context.getOperation();
    Object cacheKey = generateKey(context);

    Cache cache = resolveCache(context);
    Cache exceptionCache = resolveExceptionCache(context);

    if (!operation.isAlwaysInvoked()) {
      Cache.ValueWrapper cachedValue = doGet(cache, cacheKey);
      if (cachedValue != null) {
        return cachedValue.get();
      }
      checkForCachedException(exceptionCache, cacheKey);
    }

    try {
      Object invocationResult = invoker.invoke();
      doPut(cache, cacheKey, invocationResult);
      return invocationResult;
    }
    catch (CacheOperationInvoker.ThrowableWrapper ex) {
      Throwable original = ex.getOriginal();
      cacheException(exceptionCache, operation.getExceptionTypeFilter(), cacheKey, original);
      throw ex;
    }
  }

  /**
   * Check for a cached exception. If the exception is found, throw it directly.
   */
  protected void checkForCachedException(@Nullable Cache exceptionCache, Object cacheKey) {
    if (exceptionCache == null) {
      return;
    }
    Cache.ValueWrapper result = doGet(exceptionCache, cacheKey);
    if (result != null) {
      Throwable ex = (Throwable) result.get();
      Assert.state(ex != null, "No exception in cache");
      throw rewriteCallStack(ex, getClass().getName(), "invoke");
    }
  }

  protected void cacheException(@Nullable Cache exceptionCache, ExceptionTypeFilter filter, Object cacheKey, Throwable ex) {
    if (exceptionCache == null) {
      return;
    }
    if (filter.match(ex.getClass())) {
      doPut(exceptionCache, cacheKey, ex);
    }
  }

  @Nullable
  private Cache resolveExceptionCache(CacheOperationInvocationContext<CacheResultOperation> context) {
    CacheResolver exceptionCacheResolver = context.getOperation().getExceptionCacheResolver();
    if (exceptionCacheResolver != null) {
      return extractFrom(context.getOperation().getExceptionCacheResolver().resolveCaches(context));
    }
    return null;
  }

  /**
   * Rewrite the call stack of the specified {@code exception} so that it matches
   * the current call stack up to (included) the specified method invocation.
   * <p>Clone the specified exception. If the exception is not {@code serializable},
   * the original exception is returned. If no common ancestor can be found, returns
   * the original exception.
   * <p>Used to make sure that a cached exception has a valid invocation context.
   *
   * @param exception the exception to merge with the current call stack
   * @param className the class name of the common ancestor
   * @param methodName the method name of the common ancestor
   * @return a clone exception with a rewritten call stack composed of the current call
   * stack up to (included) the common ancestor specified by the {@code className} and
   * {@code methodName} arguments, followed by stack trace elements of the specified
   * {@code exception} after the common ancestor.
   */
  private static CacheOperationInvoker.ThrowableWrapper rewriteCallStack(
          Throwable exception, String className, String methodName) {

    Throwable clone = cloneException(exception);
    if (clone == null) {
      return new CacheOperationInvoker.ThrowableWrapper(exception);
    }

    StackTraceElement[] callStack = new Exception().getStackTrace();
    StackTraceElement[] cachedCallStack = exception.getStackTrace();

    int index = findCommonAncestorIndex(callStack, className, methodName);
    int cachedIndex = findCommonAncestorIndex(cachedCallStack, className, methodName);
    if (index == -1 || cachedIndex == -1) {
      return new CacheOperationInvoker.ThrowableWrapper(exception); // Cannot find common ancestor
    }
    StackTraceElement[] result = new StackTraceElement[cachedIndex + callStack.length - index];
    System.arraycopy(cachedCallStack, 0, result, 0, cachedIndex);
    System.arraycopy(callStack, index, result, cachedIndex, callStack.length - index);

    clone.setStackTrace(result);
    return new CacheOperationInvoker.ThrowableWrapper(clone);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static <T extends Throwable> T cloneException(T exception) {
    try {
      return (T) SerializationUtils.deserialize(SerializationUtils.serialize(exception));
    }
    catch (Exception ex) {
      return null;  // exception parameter cannot be cloned
    }
  }

  private static int findCommonAncestorIndex(StackTraceElement[] callStack, String className, String methodName) {
    for (int i = 0; i < callStack.length; i++) {
      StackTraceElement element = callStack[i];
      if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
        return i;
      }
    }
    return -1;
  }

}
