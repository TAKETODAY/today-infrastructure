/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.testfixture.cache;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.cache.Cache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheTests<T extends Cache> {

  protected final static String CACHE_NAME = "testCache";

  protected abstract T getCache();

  protected abstract Object getNativeCache();

  @Test
  public void testCacheName() throws Exception {
    assertThat(getCache().getName()).isEqualTo(CACHE_NAME);
  }

  @Test
  public void testNativeCache() throws Exception {
    assertThat(getCache().getNativeCache()).isSameAs(getNativeCache());
  }

  @Test
  public void testCachePut() throws Exception {
    T cache = getCache();

    String key = createRandomKey();
    Object value = "george";

    assertThat((Object) cache.get(key)).isNull();
    assertThat(cache.get(key, String.class)).isNull();
    assertThat(cache.get(key, Object.class)).isNull();

    cache.put(key, value);
    assertThat(cache.get(key).get()).isEqualTo(value);
    assertThat(cache.get(key, String.class)).isEqualTo(value);
    assertThat(cache.get(key, Object.class)).isEqualTo(value);
    assertThat(cache.get(key, (Class<?>) null)).isEqualTo(value);

    cache.put(key, null);
    assertThat(cache.get(key)).isNotNull();
    assertThat(cache.get(key).get()).isNull();
    assertThat(cache.get(key, String.class)).isNull();
    assertThat(cache.get(key, Object.class)).isNull();
  }

  @Test
  public void testCachePutIfAbsent() throws Exception {
    T cache = getCache();

    String key = createRandomKey();
    Object value = "initialValue";

    assertThat(cache.get(key)).isNull();
    assertThat(cache.putIfAbsent(key, value)).isNull();
    assertThat(cache.get(key).get()).isEqualTo(value);
    assertThat(cache.putIfAbsent(key, "anotherValue").get()).isEqualTo("initialValue");
    // not changed
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

  @Test
  public void testCacheRemove() throws Exception {
    T cache = getCache();

    String key = createRandomKey();
    Object value = "george";

    assertThat((Object) cache.get(key)).isNull();
    cache.put(key, value);
  }

  @Test
  public void testCacheClear() throws Exception {
    T cache = getCache();

    assertThat((Object) cache.get("enescu")).isNull();
    cache.put("enescu", "george");
    assertThat((Object) cache.get("vlaicu")).isNull();
    cache.put("vlaicu", "aurel");
    cache.clear();
    assertThat((Object) cache.get("vlaicu")).isNull();
    assertThat((Object) cache.get("enescu")).isNull();
  }

  @Test
  public void testCacheGetCallable() {
    doTestCacheGetCallable("test");
  }

  @Test
  public void testCacheGetCallableWithNull() {
    doTestCacheGetCallable(null);
  }

  private void doTestCacheGetCallable(Object returnValue) {
    T cache = getCache();

    String key = createRandomKey();

    assertThat((Object) cache.get(key)).isNull();
    Object value = cache.get(key, () -> returnValue);
    assertThat(value).isEqualTo(returnValue);
    assertThat(cache.get(key).get()).isEqualTo(value);
  }

  @Test
  public void testCacheGetCallableNotInvokedWithHit() {
    doTestCacheGetCallableNotInvokedWithHit("existing");
  }

  @Test
  public void testCacheGetCallableNotInvokedWithHitNull() {
    doTestCacheGetCallableNotInvokedWithHit(null);
  }

  private void doTestCacheGetCallableNotInvokedWithHit(Object initialValue) {
    T cache = getCache();

    String key = createRandomKey();
    cache.put(key, initialValue);

    Object value = cache.get(key, () -> {
      throw new IllegalStateException("Should not have been invoked");
    });
    assertThat(value).isEqualTo(initialValue);
  }

  @Test
  public void testCacheGetCallableFail() {
    T cache = getCache();

    String key = createRandomKey();
    assertThat((Object) cache.get(key)).isNull();

    try {
      cache.get(key, () -> {
        throw new UnsupportedOperationException("Expected exception");
      });
    }
    catch (Cache.ValueRetrievalException ex) {
      assertThat(ex.getCause()).isNotNull();
      assertThat(ex.getCause().getClass()).isEqualTo(UnsupportedOperationException.class);
    }
  }

  /**
   * Test that a call to get with a Callable concurrently properly synchronize the
   * invocations.
   */
  @Test
  public void testCacheGetSynchronized() throws InterruptedException {
    T cache = getCache();
    final AtomicInteger counter = new AtomicInteger();
    final List<Object> results = new CopyOnWriteArrayList<>();
    final CountDownLatch latch = new CountDownLatch(10);

    String key = createRandomKey();
    Runnable run = () -> {
      try {
        Integer value = cache.get(key, () -> {
          Thread.sleep(50); // make sure the thread will overlap
          return counter.incrementAndGet();
        });
        results.add(value);
      }
      finally {
        latch.countDown();
      }
    };

    for (int i = 0; i < 10; i++) {
      new Thread(run).start();
    }
    latch.await();

    assertThat(results).hasSize(10);
    results.forEach(r -> assertThat(r).isEqualTo(1)); // Only one method got invoked
  }

  protected String createRandomKey() {
    return UUID.randomUUID().toString();
  }

}
