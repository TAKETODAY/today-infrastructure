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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import infra.cache.Cache;
import infra.lang.Nullable;
import infra.util.function.SingletonSupplier;

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
   * Execute {@link Cache#get(Object, Callable)} on the specified
   * {@link Cache} and invoke the error handler if an exception occurs.
   * Invokes the {@code valueLoader} if the handler does not throw any
   * exception, which simulates a cache read-through in case of error.
   *
   * @see Cache#get(Object, Callable)
   * @since 5.0
   */
  @Nullable
  protected <T> T doGet(Cache cache, Object key, Callable<T> valueLoader) {
    try {
      return cache.get(key, valueLoader);
    }
    catch (Cache.ValueRetrievalException ex) {
      throw ex;
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheGetError(ex, cache, key);
      try {
        return valueLoader.call();
      }
      catch (Exception ex2) {
        throw new Cache.ValueRetrievalException(key, valueLoader, ex);
      }
    }
  }

  /**
   * Execute {@link Cache#retrieve(Object)} on the specified {@link Cache}
   * and invoke the error handler if an exception occurs.
   * Returns {@code null} if the handler does not throw any exception, which
   * simulates a cache miss in case of error.
   *
   * @see Cache#retrieve(Object)
   * @since 5.0
   */
  @Nullable
  protected CompletableFuture<?> doRetrieve(Cache cache, Object key) {
    try {
      return cache.retrieve(key);
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheGetError(ex, cache, key);
      return null;
    }
  }

  /**
   * Execute {@link Cache#retrieve(Object, Supplier)} on the specified
   * {@link Cache} and invoke the error handler if an exception occurs.
   * Invokes the {@code valueLoader} if the handler does not throw any
   * exception, which simulates a cache read-through in case of error.
   *
   * @see Cache#retrieve(Object, Supplier)
   * @since 5.0
   */
  protected <T> CompletableFuture<T> doRetrieve(Cache cache, Object key, Supplier<CompletableFuture<T>> valueLoader) {
    try {
      return cache.retrieve(key, valueLoader);
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCacheGetError(ex, cache, key);
      return valueLoader.get();
    }
  }

  /**
   * Execute {@link Cache#put(Object, Object)} on the specified {@link Cache}
   * and invoke the error handler if an exception occurs.
   */
  protected void doPut(Cache cache, Object key, @Nullable Object value) {
    try {
      cache.put(key, value);
    }
    catch (RuntimeException ex) {
      getErrorHandler().handleCachePutError(ex, cache, key, value);
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
