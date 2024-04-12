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

package cn.taketoday.cache.interceptor;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * A base component for invoking {@link Cache} operations and using a
 * configurable {@link CacheErrorHandler} when an exception occurs.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CacheErrorHandler
 * @since 4.0
 */
public abstract class AbstractCacheInvoker {

  protected SingletonSupplier<CacheErrorHandler> errorHandler;

  protected AbstractCacheInvoker() {
    this.errorHandler = SingletonSupplier.from(SimpleCacheErrorHandler::new);
  }

  protected AbstractCacheInvoker(CacheErrorHandler errorHandler) {
    this.errorHandler = SingletonSupplier.valueOf(errorHandler);
  }

  /**
   * Set the {@link CacheErrorHandler} instance to use to handle errors
   * thrown by the cache provider. By default, a {@link SimpleCacheErrorHandler}
   * is used who throws any exception as is.
   */
  public void setErrorHandler(CacheErrorHandler errorHandler) {
    this.errorHandler = SingletonSupplier.valueOf(errorHandler);
  }

  /**
   * Return the {@link CacheErrorHandler} to use.
   */
  public CacheErrorHandler getErrorHandler() {
    return this.errorHandler.obtain();
  }

  /**
   * Execute {@link Cache#get(Object)} on the specified {@link Cache} and
   * invoke the error handler if an exception occurs. Return {@code null}
   * if the handler does not throw any exception, which simulates a cache
   * miss in case of error.
   *
   * @see Cache#get(Object)
   */
  @Nullable
  protected Cache.ValueWrapper doGet(Cache cache, Object key) {
    try {
      return cache.get(key);
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheGetError(ex, cache, key);
      return null;  // If the exception is handled, return a cache miss
    }
  }

  /**
   * Execute {@link Cache#put(Object, Object)} on the specified {@link Cache}
   * and invoke the error handler if an exception occurs.
   */
  protected void doPut(Cache cache, Object key, @Nullable Object result) {
    try {
      cache.put(key, result);
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCachePutError(ex, cache, key, result);
    }
  }

  /**
   * Execute {@link Cache#evict(Object)}/{@link Cache#evictIfPresent(Object)} on the
   * specified {@link Cache} and invoke the error handler if an exception occurs.
   */
  protected void doEvict(Cache cache, Object key, boolean immediate) {
    try {
      if (immediate) {
        cache.evictIfPresent(key);
      }
      else {
        cache.evict(key);
      }
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheEvictError(ex, cache, key);
    }
  }

  /**
   * Execute {@link Cache#clear()} on the specified {@link Cache} and
   * invoke the error handler if an exception occurs.
   */
  protected void doClear(Cache cache, boolean immediate) {
    try {
      if (immediate) {
        cache.invalidate();
      }
      else {
        cache.clear();
      }
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheClearError(ex, cache);
    }
  }

}
