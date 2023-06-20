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

package cn.taketoday.cache.jcache.transaction;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.jcache.TransactionAwareCacheDecorator;
import cn.taketoday.transaction.support.TransactionTemplate;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class TransactionAwareCacheDecoratorTests {

  private final TransactionTemplate txTemplate = new TransactionTemplate(new CallCountingTransactionManager());

  @Test
  public void createWithNullTarget() {
    assertThatIllegalArgumentException().isThrownBy(() -> new TransactionAwareCacheDecorator(null));
  }

  @Test
  public void getTargetCache() {
    Cache target = new ConcurrentMapCache("testCache");
    TransactionAwareCacheDecorator cache = new TransactionAwareCacheDecorator(target);
    assertThat(cache.getTargetCache()).isSameAs(target);
  }

  @Test
  public void regularOperationsOnTarget() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    assertThat(cache.getName()).isEqualTo(target.getName());
    assertThat(cache.getNativeCache()).isEqualTo(target.getNativeCache());

    Object key = new Object();
    target.put(key, "123");
    assertThat(cache.get(key).get()).isEqualTo("123");
    assertThat(cache.get(key, String.class)).isEqualTo("123");

    cache.clear();
    assertThat(target.get(key)).isNull();
  }

  @Test
  public void putNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);

    Object key = new Object();
    cache.put(key, "123");
    assertThat(target.get(key, String.class)).isEqualTo("123");
  }

  @Test
  public void putTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();

    txTemplate.executeWithoutResult(s -> {
      cache.put(key, "123");
      assertThat(target.get(key)).isNull();
    });

    assertThat(target.get(key, String.class)).isEqualTo("123");
  }

  @Test
  public void putIfAbsentNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);

    Object key = new Object();
    assertThat(cache.putIfAbsent(key, "123")).isNull();
    assertThat(target.get(key, String.class)).isEqualTo("123");
    assertThat(cache.putIfAbsent(key, "456").get()).isEqualTo("123");
    // unchanged
    assertThat(target.get(key, String.class)).isEqualTo("123");
  }

  @Test
  public void putIfAbsentTransactional() {  // no transactional support for putIfAbsent
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();

    txTemplate.executeWithoutResult(s -> {
      assertThat(cache.putIfAbsent(key, "123")).isNull();
      assertThat(target.get(key, String.class)).isEqualTo("123");
      assertThat(cache.putIfAbsent(key, "456").get()).isEqualTo("123");
      // unchanged
      assertThat(target.get(key, String.class)).isEqualTo("123");
    });

    assertThat(target.get(key, String.class)).isEqualTo("123");
  }

  @Test
  public void evictNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    cache.evict(key);
    assertThat(target.get(key)).isNull();
  }

  @Test
  public void evictTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    txTemplate.executeWithoutResult(s -> {
      cache.evict(key);
      assertThat(target.get(key, String.class)).isEqualTo("123");
    });

    assertThat(target.get(key)).isNull();
  }

  @Test
  public void evictIfPresentNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    cache.evictIfPresent(key);
    assertThat(target.get(key)).isNull();
  }

  @Test
  public void evictIfPresentTransactional() {  // no transactional support for evictIfPresent
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    txTemplate.executeWithoutResult(s -> {
      cache.evictIfPresent(key);
      assertThat(target.get(key)).isNull();
    });

    assertThat(target.get(key)).isNull();
  }

  @Test
  public void clearNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    cache.clear();
    assertThat(target.get(key)).isNull();
  }

  @Test
  public void clearTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    txTemplate.executeWithoutResult(s -> {
      cache.clear();
      assertThat(target.get(key, String.class)).isEqualTo("123");
    });

    assertThat(target.get(key)).isNull();
  }

  @Test
  public void invalidateNonTransactional() {
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    cache.invalidate();
    assertThat(target.get(key)).isNull();
  }

  @Test
  public void invalidateTransactional() {  // no transactional support for invalidate
    Cache target = new ConcurrentMapCache("testCache");
    Cache cache = new TransactionAwareCacheDecorator(target);
    Object key = new Object();
    cache.put(key, "123");

    txTemplate.executeWithoutResult(s -> {
      cache.invalidate();
      assertThat(target.get(key)).isNull();
    });

    assertThat(target.get(key)).isNull();
  }

}
