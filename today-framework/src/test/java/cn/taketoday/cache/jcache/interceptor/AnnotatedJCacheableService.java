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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.contextsupport.testfixture.cache.TestableCacheKeyGenerator;
import cn.taketoday.contextsupport.testfixture.cache.TestableCacheResolverFactory;
import cn.taketoday.contextsupport.testfixture.jcache.JCacheableService;

/**
 * Repository sample with a @CacheDefaults annotation
 *
 * @author Stephane Nicoll
 */
@CacheDefaults(cacheName = "default")
public class AnnotatedJCacheableService implements JCacheableService<Long> {

  private final AtomicLong counter = new AtomicLong();

  private final AtomicLong exceptionCounter = new AtomicLong();

  private final Cache defaultCache;

  public AnnotatedJCacheableService(Cache defaultCache) {
    this.defaultCache = defaultCache;
  }

  @Override
  @CacheResult
  public Long cache(String id) {
    return counter.getAndIncrement();
  }

  @Override
  @CacheResult
  public Long cacheNull(String id) {
    return null;
  }

  @Override
  @CacheResult(exceptionCacheName = "exception", nonCachedExceptions = NullPointerException.class)
  public Long cacheWithException(@CacheKey String id, boolean matchFilter) {
    throwException(matchFilter);
    return 0L; // Never reached
  }

  @Override
  @CacheResult(exceptionCacheName = "exception", nonCachedExceptions = NullPointerException.class)
  public Long cacheWithCheckedException(@CacheKey String id, boolean matchFilter) throws IOException {
    throwCheckedException(matchFilter);
    return 0L; // Never reached
  }

  @Override
  @CacheResult(skipGet = true)
  public Long cacheAlwaysInvoke(String id) {
    return counter.getAndIncrement();
  }

  @Override
  @CacheResult
  public Long cacheWithPartialKey(@CacheKey String id, boolean notUsed) {
    return counter.getAndIncrement();
  }

  @Override
  @CacheResult(cacheResolverFactory = TestableCacheResolverFactory.class)
  public Long cacheWithCustomCacheResolver(String id) {
    return counter.getAndIncrement();
  }

  @Override
  @CacheResult(cacheKeyGenerator = TestableCacheKeyGenerator.class)
  public Long cacheWithCustomKeyGenerator(String id, String anotherId) {
    return counter.getAndIncrement();
  }

  @Override
  @CachePut
  public void put(String id, @CacheValue Object value) {
  }

  @Override
  @CachePut(cacheFor = UnsupportedOperationException.class)
  public void putWithException(@CacheKey String id, @CacheValue Object value, boolean matchFilter) {
    throwException(matchFilter);
  }

  @Override
  @CachePut(afterInvocation = false)
  public void earlyPut(String id, @CacheValue Object value) {
    Object key = SimpleKeyGenerator.generateKey(id);
    Cache.ValueWrapper valueWrapper = defaultCache.get(key);
    if (valueWrapper == null) {
      throw new AssertionError("Excepted value to be put in cache with key " + key);
    }
    Object actual = valueWrapper.get();
    if (value != actual) { // instance check on purpose
      throw new AssertionError("Wrong value set in cache with key " + key + ". " +
              "Expected=" + value + ", but got=" + actual);
    }
  }

  @Override
  @CachePut(afterInvocation = false)
  public void earlyPutWithException(@CacheKey String id, @CacheValue Object value, boolean matchFilter) {
    throwException(matchFilter);
  }

  @Override
  @CacheRemove
  public void remove(String id) {
  }

  @Override
  @CacheRemove(noEvictFor = NullPointerException.class)
  public void removeWithException(@CacheKey String id, boolean matchFilter) {
    throwException(matchFilter);
  }

  @Override
  @CacheRemove(afterInvocation = false)
  public void earlyRemove(String id) {
    Object key = SimpleKeyGenerator.generateKey(id);
    Cache.ValueWrapper valueWrapper = defaultCache.get(key);
    if (valueWrapper != null) {
      throw new AssertionError("Value with key " + key + " expected to be already remove from cache");
    }
  }

  @Override
  @CacheRemove(afterInvocation = false, evictFor = UnsupportedOperationException.class)
  public void earlyRemoveWithException(@CacheKey String id, boolean matchFilter) {
    throwException(matchFilter);
  }

  @Override
  @CacheRemoveAll
  public void removeAll() {
  }

  @Override
  @CacheRemoveAll(noEvictFor = NullPointerException.class)
  public void removeAllWithException(boolean matchFilter) {
    throwException(matchFilter);
  }

  @Override
  @CacheRemoveAll(afterInvocation = false)
  public void earlyRemoveAll() {
    ConcurrentHashMap<?, ?> nativeCache = (ConcurrentHashMap<?, ?>) defaultCache.getNativeCache();
    if (!nativeCache.isEmpty()) {
      throw new AssertionError("Cache was expected to be empty");
    }
  }

  @Override
  @CacheRemoveAll(afterInvocation = false, evictFor = UnsupportedOperationException.class)
  public void earlyRemoveAllWithException(boolean matchFilter) {
    throwException(matchFilter);
  }

  @Deprecated
  public void noAnnotation() {
  }

  @Override
  public long exceptionInvocations() {
    return exceptionCounter.get();
  }

  private void throwException(boolean matchFilter) {
    long count = exceptionCounter.getAndIncrement();
    if (matchFilter) {
      throw new UnsupportedOperationException("Expected exception (" + count + ")");
    }
    else {
      throw new NullPointerException("Expected exception (" + count + ")");
    }
  }

  private void throwCheckedException(boolean matchFilter) throws IOException {
    long count = exceptionCounter.getAndIncrement();
    if (matchFilter) {
      throw new IOException("Expected exception (" + count + ")");
    }
    else {
      throw new NullPointerException("Expected exception (" + count + ")");
    }
  }

}
