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
package cn.taketoday.cache;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;

import java.util.concurrent.TimeUnit;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;

/**
 * @author TODAY <br>
 * 2019-02-28 18:30
 */
public class RedissonCache extends Cache {

  private final CacheConfig cacheConfig;
  private final RMap<Object, Object> cache;

  public RedissonCache(RMap<Object, Object> cache) {
    this(cache, Constant.DEFAULT, null);
  }

  public RedissonCache(RMap<Object, Object> cache, String name) {
    this(cache, name, null);
  }

  public RedissonCache(RMap<Object, Object> cache, CacheConfig cacheConfig) {
    this(cache, cacheConfig.cacheName(), cacheConfig);
  }

  public RedissonCache(RMap<Object, Object> cache, String name, CacheConfig cacheConfig) {
    Assert.notNull(name, "name must not be null");
    Assert.notNull(cache, "cache must not be null");
    this.cache = cache;
    this.cacheConfig = cacheConfig;
    setName(name);
  }

  @Override
  public void evict(final Object key) {
    cache.fastRemove(key);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  protected <T> Object compute(Object key, CacheCallback<T> valueLoader) throws Throwable {
    final RLock lock = cache.getLock(key);
    try {
      lock.lock();
      Object value = cache.get(key);
      if (value == null) {
        final Object newValue = valueLoader.call();
        put(key, newValue);
        return newValue;
      }
      else {
        return value;
      }
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  protected Object doGet(Object key) {
    return cache.get(key);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void doPut(Object key, Object value) {
    if (cacheConfig != null && cache instanceof RMapCache) {
      final TimeUnit timeUnit = cacheConfig.timeUnit();
      ((RMapCache) cache).fastPut(
              key,
              value,
              cacheConfig.expire(),
              timeUnit,
              cacheConfig.maxIdleTime(),
              timeUnit
      );
    }
    else {
      cache.fastPut(key, value);
    }
  }

}
