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
package cn.taketoday.cache.redisson;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.cache.support.SimpleValueWrapper;
import cn.taketoday.cache.Cache;
import cn.taketoday.lang.NullValue;

/**
 * @author Nikita Koksharov
 * @author TODAY
 * @since 2019-02-28 18:30
 */
public class RedissonCache implements Cache {

  private RMapCache<Object, Object> mapCache;

  private final RMap<Object, Object> map;

  private CacheConfig config;

  private final boolean allowNullValues;

  private final AtomicLong hits = new AtomicLong();

  private final AtomicLong puts = new AtomicLong();

  private final AtomicLong misses = new AtomicLong();

  public RedissonCache(RMapCache<Object, Object> mapCache, CacheConfig config, boolean allowNullValues) {
    this(mapCache, allowNullValues);
    this.mapCache = mapCache;
    this.config = config;
  }

  public RedissonCache(RMap<Object, Object> map, boolean allowNullValues) {
    this.map = map;
    this.allowNullValues = allowNullValues;
  }

  @Override
  public String getName() {
    return map.getName();
  }

  @Override
  public RMap<?, ?> getNativeCache() {
    return map;
  }

  @Override
  public ValueWrapper get(Object key) {
    Object value;
    if (mapCache != null && config.getMaxIdleTime() == 0 && config.getMaxSize() == 0) {
      value = mapCache.getWithTTLOnly(key);
    }
    else {
      value = map.get(key);
    }

    if (value == null) {
      addCacheMiss();
    }
    else {
      addCacheHit();
    }
    return toValueWrapper(value);
  }

  public <T> T get(Object key, Class<T> type) {
    Object value;
    if (mapCache != null && config.getMaxIdleTime() == 0 && config.getMaxSize() == 0) {
      value = mapCache.getWithTTLOnly(key);
    }
    else {
      value = map.get(key);
    }

    if (value == null) {
      addCacheMiss();
    }
    else {
      addCacheHit();
      if (value.getClass().getName().equals(NullValue.class.getName())) {
        return null;
      }
      if (type != null && !type.isInstance(value)) {
        throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
      }
    }
    return (T) fromStoreValue(value);
  }

  @Override
  public void put(Object key, Object value) {
    if (!allowNullValues && value == null) {
      map.remove(key);
      return;
    }

    value = toStoreValue(value);
    if (mapCache != null) {
      mapCache.fastPut(key, value, config.getTTL(), TimeUnit.MILLISECONDS, config.getMaxIdleTime(), TimeUnit.MILLISECONDS);
    }
    else {
      map.fastPut(key, value);
    }
    addCachePut();
  }

  public ValueWrapper putIfAbsent(Object key, Object value) {
    Object prevValue;
    if (!allowNullValues && value == null) {
      prevValue = map.get(key);
    }
    else {
      value = toStoreValue(value);
      if (mapCache != null) {
        prevValue = mapCache.putIfAbsent(key, value, config.getTTL(), TimeUnit.MILLISECONDS, config.getMaxIdleTime(), TimeUnit.MILLISECONDS);
      }
      else {
        prevValue = map.putIfAbsent(key, value);
      }
      if (prevValue == null) {
        addCachePut();
      }
    }

    return toValueWrapper(prevValue);
  }

  @Override
  public void evict(Object key) {
    map.fastRemove(key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  private ValueWrapper toValueWrapper(Object value) {
    if (value == null) {
      return null;
    }
    if (value.getClass().getName().equals(NullValue.class.getName())) {
      return NullValueWrapper.INSTANCE;
    }
    return new SimpleValueWrapper(value);
  }

  public <T> T get(Object key, Callable<T> valueLoader) {
    Object value;
    if (mapCache != null && config.getMaxIdleTime() == 0 && config.getMaxSize() == 0) {
      value = mapCache.getWithTTLOnly(key);
    }
    else {
      value = map.get(key);
    }

    if (value == null) {
      addCacheMiss();
      RLock lock = map.getLock(key);
      lock.lock();
      try {
        value = map.get(key);
        if (value == null) {
          value = putValue(key, valueLoader, value);
        }
      }
      finally {
        lock.unlock();
      }
    }
    else {
      addCacheHit();
    }

    return (T) fromStoreValue(value);
  }

  private <T> Object putValue(Object key, Callable<T> valueLoader, Object value) {
    try {
      value = valueLoader.call();
    }
    catch (Exception ex) {
      RuntimeException exception;
      try {
        Class<?> c = Class.forName("cn.taketoday.cache.Cache$ValueRetrievalException");
        Constructor<?> constructor = c.getConstructor(Object.class, Callable.class, Throwable.class);
        exception = (RuntimeException) constructor.newInstance(key, valueLoader, ex);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      throw exception;
    }
    put(key, value);
    return value;
  }

  protected Object fromStoreValue(Object storeValue) {
    if (storeValue instanceof NullValue) {
      return null;
    }
    return storeValue;
  }

  protected Object toStoreValue(Object userValue) {
    if (userValue == null) {
      return NullValue.INSTANCE;
    }
    return userValue;
  }

  /**
   * The number of get requests that were satisfied by the cache.
   *
   * @return the number of hits
   */
  long getCacheHits() {
    return hits.get();
  }

  /**
   * A miss is a get request that is not satisfied.
   *
   * @return the number of misses
   */
  long getCacheMisses() {
    return misses.get();
  }

  long getCachePuts() {
    return puts.get();
  }

  private void addCachePut() {
    puts.incrementAndGet();
  }

  private void addCacheHit() {
    hits.incrementAndGet();
  }

  private void addCacheMiss() {
    misses.incrementAndGet();
  }
}
