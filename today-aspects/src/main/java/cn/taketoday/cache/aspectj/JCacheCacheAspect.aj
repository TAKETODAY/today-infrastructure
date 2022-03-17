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

package cn.taketoday.cache.aspectj;

import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.jcache.interceptor.JCacheAspectSupport;

/**
 * Concrete AspectJ cache aspect using JSR-107 standard annotations.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class (and/or
 * methods within that class), <i>not</i> the interface (if any) that the class
 * implements. AspectJ follows Java's rule that annotations on interfaces are <i>not</i>
 * inherited.
 *
 * <p>Any method may be annotated (regardless of visibility). Annotating non-public
 * methods directly is the only way to get caching demarcation for the execution of
 * such operations.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@RequiredTypes({ "cn.taketoday.cache.jcache.interceptor.JCacheAspectSupport", "javax.cache.annotation.CacheResult" })
public aspect JCacheCacheAspect extends JCacheAspectSupport {

  @SuppressAjWarnings("adviceDidNotMatch")
  Object around(final Object cachedObject): cacheMethodExecution(cachedObject) {
    MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
    Method method = methodSignature.getMethod();

    CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
      public Object invoke() {
        try {
          return proceed(cachedObject);
        }
        catch (Throwable ex) {
          throw new ThrowableWrapper(ex);
        }
      }

    };

    try {
      return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
    }
    catch (CacheOperationInvoker.ThrowableWrapper th) {
      AnyThrow.throwUnchecked(th.getOriginal());
      return null; // never reached
    }
  }

  /**
   * Definition of pointcut: matched join points will have JSR-107
   * cache management applied.
   */
  protected pointcut cacheMethodExecution(Object cachedObject):
          (executionOfCacheResultMethod()
                  || executionOfCachePutMethod()
                  || executionOfCacheRemoveMethod()
                  || executionOfCacheRemoveAllMethod())
                  && this(cachedObject);

  /**
   * Matches the execution of any method with the @{@link CacheResult} annotation.
   */
  private pointcut executionOfCacheResultMethod():
          execution(@CacheResult * *(..));

  /**
   * Matches the execution of any method with the @{@link CachePut} annotation.
   */
  private pointcut executionOfCachePutMethod():
          execution(@CachePut * *(..));

  /**
   * Matches the execution of any method with the @{@link CacheRemove} annotation.
   */
  private pointcut executionOfCacheRemoveMethod():
          execution(@CacheRemove * *(..));

  /**
   * Matches the execution of any method with the @{@link CacheRemoveAll} annotation.
   */
  private pointcut executionOfCacheRemoveAllMethod():
          execution(@CacheRemoveAll * *(..));


}
