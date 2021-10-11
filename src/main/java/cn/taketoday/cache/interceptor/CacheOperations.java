/**
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

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheCallback;

/**
 * @author TODAY <br>
 * 2019-11-01 19:58
 */
public class CacheOperations {

  private CacheExceptionResolver exceptionResolver;

  public CacheOperations() { }

  public CacheOperations(CacheExceptionResolver exceptionResolver) {
    this.exceptionResolver = exceptionResolver;
  }

  /**
   * Execute {@link Cache#get(Object)} on the specified {@link Cache} and invoke
   * the error handler if an exception occurs. Return {@code null} if the handler
   * does not throw any exception, which simulates a cache miss in case of error.
   *
   * @see Cache#get(Object)
   */
  public Object get(final Cache cache, final Object key) {
    try {
      return cache.get(key);
    }
    catch (RuntimeException ex) {
      getExceptionResolver().resolveGetException(ex, cache, key);
      return null;
    }
  }

  /**
   * Execute {@link Cache#get(Object)} on the specified {@link Cache} and invoke
   * the error handler if an exception occurs. Return {@code null} if the handler
   * does not throw any exception, which simulates a cache miss in case of error.
   *
   * @see Cache#get(Object)
   */
  public Object get(final Cache cache, final Object key, CacheCallback<Object> valueLoader) {
    try {
      return cache.get(key, valueLoader);
    }
    catch (RuntimeException ex) {
      return getExceptionResolver().resolveGetException(ex, cache, key);
    }
  }

  /**
   * Execute {@link Cache#put(Object, Object)} on the specified {@link Cache} and
   * invoke the error handler if an exception occurs.
   */
  public void put(final Cache cache, final Object key, final Object value) {
    try {
      cache.put(key, value);
    }
    catch (RuntimeException ex) {
      getExceptionResolver().resolvePutException(ex, cache, key, ex);
    }
  }

  /**
   * Execute {@link Cache#evict(Object)} on the specified {@link Cache} and invoke
   * the error handler if an exception occurs.
   */
  public void evict(final Cache cache, final Object key) {
    try {
      cache.evict(key);
    }
    catch (RuntimeException ex) {
      getExceptionResolver().resolveEvictException(ex, cache, key);
    }
  }

  /**
   * Execute {@link Cache#clear()} on the specified {@link Cache} and invoke the
   * error handler if an exception occurs.
   */
  public void clear(final Cache cache) {
    try {
      cache.clear();
    }
    catch (RuntimeException ex) {
      getExceptionResolver().resolveClearException(ex, cache);
    }
  }

  public final CacheExceptionResolver getExceptionResolver() {
    return exceptionResolver;
  }

  public void setExceptionResolver(CacheExceptionResolver exceptionResolver) {
    this.exceptionResolver = exceptionResolver;
  }
}
