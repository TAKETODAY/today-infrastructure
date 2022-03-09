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

package cn.taketoday.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.support.CaffeineCache;
import cn.taketoday.cache.support.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class CaffeineCacheManagerTests {

  @Test
  public void testDynamicMode() {
    CacheManager cm = new CaffeineCacheManager();
    Cache cache1 = cm.getCache("c1");
    boolean condition2 = cache1 instanceof CaffeineCache;
    assertThat(condition2).isTrue();
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    boolean condition1 = cache2 instanceof CaffeineCache;
    assertThat(condition1).isTrue();
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    boolean condition = cache3 instanceof CaffeineCache;
    assertThat(condition).isTrue();
    Cache cache3again = cm.getCache("c3");
    assertThat(cache3).isSameAs(cache3again);

    cache1.put("key1", "value1");
    assertThat(cache1.get("key1").get()).isEqualTo("value1");
    cache1.put("key2", 2);
    assertThat(cache1.get("key2").get()).isEqualTo(2);
    cache1.put("key3", null);
    assertThat(cache1.get("key3").get()).isNull();
    cache1.evict("key3");
    assertThat(cache1.get("key3")).isNull();
  }

  @Test
  public void testStaticMode() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1", "c2");
    Cache cache1 = cm.getCache("c1");
    boolean condition3 = cache1 instanceof CaffeineCache;
    assertThat(condition3).isTrue();
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    boolean condition2 = cache2 instanceof CaffeineCache;
    assertThat(condition2).isTrue();
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    assertThat(cache3).isNull();

    cache1.put("key1", "value1");
    assertThat(cache1.get("key1").get()).isEqualTo("value1");
    cache1.put("key2", 2);
    assertThat(cache1.get("key2").get()).isEqualTo(2);
    cache1.put("key3", null);
    assertThat(cache1.get("key3").get()).isNull();
    cache1.evict("key3");
    assertThat(cache1.get("key3")).isNull();

    cm.setAllowNullValues(false);
    Cache cache1x = cm.getCache("c1");
    boolean condition1 = cache1x instanceof CaffeineCache;
    assertThat(condition1).isTrue();
    assertThat(cache1x != cache1).isTrue();
    Cache cache2x = cm.getCache("c2");
    boolean condition = cache2x instanceof CaffeineCache;
    assertThat(condition).isTrue();
    assertThat(cache2x != cache2).isTrue();
    Cache cache3x = cm.getCache("c3");
    assertThat(cache3x).isNull();

    cache1x.put("key1", "value1");
    assertThat(cache1x.get("key1").get()).isEqualTo("value1");
    cache1x.put("key2", 2);
    assertThat(cache1x.get("key2").get()).isEqualTo(2);

    cm.setAllowNullValues(true);
    Cache cache1y = cm.getCache("c1");

    cache1y.put("key3", null);
    assertThat(cache1y.get("key3").get()).isNull();
    cache1y.evict("key3");
    assertThat(cache1y.get("key3")).isNull();
  }

  @Test
  public void changeCaffeineRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(10);
    cm.setCaffeine(caffeine);
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x != cache1).isTrue();

    cm.setCaffeine(caffeine);  // Set same instance
    Cache cache1xx = cm.getCache("c1");
    assertThat(cache1xx).isSameAs(cache1x);
  }

  @Test
  public void changeCaffeineSpecRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    cm.setCaffeineSpec(CaffeineSpec.parse("maximumSize=10"));
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x != cache1).isTrue();
  }

  @Test
  public void changeCacheSpecificationRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    cm.setCacheSpecification("maximumSize=10");
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x != cache1).isTrue();
  }

  @Test
  public void changeCacheLoaderRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    @SuppressWarnings("unchecked")
    CacheLoader<Object, Object> loader = mock(CacheLoader.class);

    cm.setCacheLoader(loader);
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x != cache1).isTrue();

    cm.setCacheLoader(loader);  // Set same instance
    Cache cache1xx = cm.getCache("c1");
    assertThat(cache1xx).isSameAs(cache1x);
  }

  @Test
  public void setCacheNameNullRestoreDynamicMode() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    assertThat(cm.getCache("someCache")).isNull();
    cm.setCacheNames(null);
    assertThat(cm.getCache("someCache")).isNotNull();
  }

  @Test
  public void cacheLoaderUseLoadingCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    cm.setCacheLoader(key -> {
      if ("ping".equals(key)) {
        return "pong";
      }
      throw new IllegalArgumentException("I only know ping");
    });
    Cache cache1 = cm.getCache("c1");
    Cache.ValueWrapper value = cache1.get("ping");
    assertThat(value).isNotNull();
    assertThat(value.get()).isEqualTo("pong");

    assertThatIllegalArgumentException().isThrownBy(() -> assertThat(cache1.get("foo")).isNull())
            .withMessageContaining("I only know ping");
  }

  @Test
  public void customCacheRegistration() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nc = Caffeine.newBuilder().build();
    cm.registerCustomCache("c2", nc);

    Cache cache1 = cm.getCache("c1");
    Cache cache2 = cm.getCache("c2");
    assertThat(nc == cache2.getNativeCache()).isTrue();

    cm.setCaffeine(Caffeine.newBuilder().maximumSize(10));
    assertThat(cm.getCache("c1") != cache1).isTrue();
    assertThat(cm.getCache("c2") == cache2).isTrue();
  }

}
