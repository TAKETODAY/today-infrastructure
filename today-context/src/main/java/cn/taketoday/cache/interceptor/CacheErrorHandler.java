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

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Nullable;

/**
 * A strategy for handling cache-related errors. In most cases, any
 * exception thrown by the provider should simply be thrown back at
 * the client but, in some circumstances, the infrastructure may need
 * to handle cache-provider exceptions in a different way.
 *
 * <p>Typically, failing to retrieve an object from the cache with
 * a given id can be transparently managed as a cache miss by not
 * throwing back such exception.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface CacheErrorHandler {

  /**
   * Handle the given runtime exception thrown by the cache provider when
   * retrieving an item with the specified {@code key}, possibly
   * rethrowing it as a fatal exception.
   *
   * @param exception the exception thrown by the cache provider
   * @param cache the cache
   * @param key the key used to get the item
   * @see Cache#get(Object)
   */
  void handleCacheGetError(RuntimeException exception, Cache cache, Object key);

  /**
   * Handle the given runtime exception thrown by the cache provider when
   * updating an item with the specified {@code key} and {@code value},
   * possibly rethrowing it as a fatal exception.
   *
   * @param exception the exception thrown by the cache provider
   * @param cache the cache
   * @param key the key used to update the item
   * @param value the value to associate with the key
   * @see Cache#put(Object, Object)
   */
  void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value);

  /**
   * Handle the given runtime exception thrown by the cache provider when
   * clearing an item with the specified {@code key}, possibly rethrowing
   * it as a fatal exception.
   *
   * @param exception the exception thrown by the cache provider
   * @param cache the cache
   * @param key the key used to clear the item
   */
  void handleCacheEvictError(RuntimeException exception, Cache cache, Object key);

  /**
   * Handle the given runtime exception thrown by the cache provider when
   * clearing the specified {@link Cache}, possibly rethrowing it as a
   * fatal exception.
   *
   * @param exception the exception thrown by the cache provider
   * @param cache the cache to clear
   */
  void handleCacheClearError(RuntimeException exception, Cache cache);

}
