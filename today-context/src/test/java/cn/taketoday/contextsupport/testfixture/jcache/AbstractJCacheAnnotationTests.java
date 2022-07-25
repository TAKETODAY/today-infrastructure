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

package cn.taketoday.contextsupport.testfixture.jcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractJCacheAnnotationTests {

  public static final String DEFAULT_CACHE = "default";

  public static final String EXCEPTION_CACHE = "exception";

  protected String keyItem;

  protected ApplicationContext ctx;

  private JCacheableService<?> service;

  private CacheManager cacheManager;

  protected abstract ApplicationContext getApplicationContext();

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    this.keyItem = testInfo.getTestMethod().get().getName();
    this.ctx = getApplicationContext();
    this.service = this.ctx.getBean(JCacheableService.class);
    this.cacheManager = this.ctx.getBean("cacheManager", CacheManager.class);
  }

  @Test
  public void cache() {
    Object first = service.cache(this.keyItem);
    Object second = service.cache(this.keyItem);
    assertThat(second).isSameAs(first);
  }

  @Test
  public void cacheNull() {
    Cache cache = getCache(DEFAULT_CACHE);

    assertThat(cache.get(this.keyItem)).isNull();

    Object first = service.cacheNull(this.keyItem);
    Object second = service.cacheNull(this.keyItem);
    assertThat(second).isSameAs(first);

    Cache.ValueWrapper wrapper = cache.get(this.keyItem);
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isSameAs(first);
    assertThat(wrapper.get()).as("Cached value should be null").isNull();
  }

  @Test
  public void cacheException() {
    Cache cache = getCache(EXCEPTION_CACHE);

    Object key = createKey(this.keyItem);
    assertThat(cache.get(key)).isNull();

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.cacheWithException(this.keyItem, true));

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get().getClass()).isEqualTo(UnsupportedOperationException.class);
  }

  @Test
  public void cacheExceptionVetoed() {
    Cache cache = getCache(EXCEPTION_CACHE);

    Object key = createKey(this.keyItem);
    assertThat(cache.get(key)).isNull();

    assertThatNullPointerException().isThrownBy(() ->
            service.cacheWithException(this.keyItem, false));
    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void cacheCheckedException() {
    Cache cache = getCache(EXCEPTION_CACHE);

    Object key = createKey(this.keyItem);
    assertThat(cache.get(key)).isNull();
    assertThatIOException().isThrownBy(() ->
            service.cacheWithCheckedException(this.keyItem, true));

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get().getClass()).isEqualTo(IOException.class);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Test
  public void cacheExceptionRewriteCallStack() {
    long ref = service.exceptionInvocations();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    service.cacheWithException(this.keyItem, true))
            .satisfies(first -> {
              // Sanity check, this particular call has called the service
              // First call should not have been cached
              assertThat(service.exceptionInvocations()).isEqualTo(ref + 1);

              UnsupportedOperationException second = methodInCallStack(this.keyItem);
              // Sanity check, this particular call has *not* called the service
              // Second call should have been cached
              assertThat(service.exceptionInvocations()).isEqualTo(ref + 1);

              assertThat(first).hasCause(second.getCause());
              assertThat(first).hasMessage(second.getMessage());
              // Original stack must not contain any reference to methodInCallStack
              assertThat(contain(first, AbstractJCacheAnnotationTests.class.getName(), "methodInCallStack")).isFalse();
              assertThat(contain(second, AbstractJCacheAnnotationTests.class.getName(), "methodInCallStack")).isTrue();
            });
  }

  @Test
  public void cacheAlwaysInvoke() {
    Object first = service.cacheAlwaysInvoke(this.keyItem);
    Object second = service.cacheAlwaysInvoke(this.keyItem);
    assertThat(second).isNotSameAs(first);
  }

  @Test
  public void cacheWithPartialKey() {
    Object first = service.cacheWithPartialKey(this.keyItem, true);
    Object second = service.cacheWithPartialKey(this.keyItem, false);
    // second argument not used, see config
    assertThat(second).isSameAs(first);
  }

  @Test
  public void cacheWithCustomCacheResolver() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    service.cacheWithCustomCacheResolver(this.keyItem);

    // Cache in mock cache
    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void cacheWithCustomKeyGenerator() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    service.cacheWithCustomKeyGenerator(this.keyItem, "ignored");

    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void put() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();

    service.put(this.keyItem, value);

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  public void putWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.putWithException(this.keyItem, value, true));

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  public void putWithExceptionVetoPut() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();

    assertThatNullPointerException().isThrownBy(() ->
            service.putWithException(this.keyItem, value, false));
    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void earlyPut() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();

    service.earlyPut(this.keyItem, value);

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  public void earlyPutWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.earlyPutWithException(this.keyItem, value, true));

    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  public void earlyPutWithExceptionVetoPut() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    assertThat(cache.get(key)).isNull();
    assertThatNullPointerException().isThrownBy(() ->
            service.earlyPutWithException(this.keyItem, value, false));
    // This will be cached anyway as the earlyPut has updated the cache before
    Cache.ValueWrapper result = cache.get(key);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  public void remove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    service.remove(this.keyItem);

    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void removeWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.removeWithException(this.keyItem, true));

    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void removeWithExceptionVetoRemove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    assertThatNullPointerException().isThrownBy(() ->
            service.removeWithException(this.keyItem, false));
    Cache.ValueWrapper wrapper = cache.get(key);
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isEqualTo(value);
  }

  @Test
  public void earlyRemove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    service.earlyRemove(this.keyItem);

    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void earlyRemoveWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.earlyRemoveWithException(this.keyItem, true));
    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void earlyRemoveWithExceptionVetoRemove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    Object value = new Object();
    cache.put(key, value);

    assertThatNullPointerException().isThrownBy(() ->
            service.earlyRemoveWithException(this.keyItem, false));
    // This will be remove anyway as the earlyRemove has removed the cache before
    assertThat(cache.get(key)).isNull();
  }

  @Test
  public void removeAll() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    service.removeAll();

    assertThat(isEmpty(cache)).isTrue();
  }

  @Test
  public void removeAllWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.removeAllWithException(true));

    assertThat(isEmpty(cache)).isTrue();
  }

  @Test
  public void removeAllWithExceptionVetoRemove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    assertThatNullPointerException().isThrownBy(() ->
            service.removeAllWithException(false));
    assertThat(cache.get(key)).isNotNull();
  }

  @Test
  public void earlyRemoveAll() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    service.earlyRemoveAll();

    assertThat(isEmpty(cache)).isTrue();
  }

  @Test
  public void earlyRemoveAllWithException() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            service.earlyRemoveAllWithException(true));
    assertThat(isEmpty(cache)).isTrue();
  }

  @Test
  public void earlyRemoveAllWithExceptionVetoRemove() {
    Cache cache = getCache(DEFAULT_CACHE);

    Object key = createKey(this.keyItem);
    cache.put(key, new Object());

    assertThatNullPointerException().isThrownBy(() ->
            service.earlyRemoveAllWithException(false));
    // This will be remove anyway as the earlyRemove has removed the cache before
    assertThat(isEmpty(cache)).isTrue();
  }

  protected boolean isEmpty(Cache cache) {
    ConcurrentHashMap<?, ?> nativeCache = (ConcurrentHashMap<?, ?>) cache.getNativeCache();
    return nativeCache.isEmpty();
  }

  private Object createKey(Object... params) {
    return SimpleKeyGenerator.generateKey(params);
  }

  private Cache getCache(String name) {
    Cache cache = cacheManager.getCache(name);
    assertThat(cache).as("required cache " + name + " does not exist").isNotNull();
    return cache;
  }

  /**
   * The only purpose of this method is to invoke a particular method on the
   * service so that the call stack is different.
   */
  private UnsupportedOperationException methodInCallStack(String keyItem) {
    try {
      service.cacheWithException(keyItem, true);
      throw new IllegalStateException("Should have thrown an exception");
    }
    catch (UnsupportedOperationException e) {
      return e;
    }
  }

  private boolean contain(Throwable t, String className, String methodName) {
    for (StackTraceElement element : t.getStackTrace()) {
      if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
        return true;
      }
    }
    return false;
  }

}
