/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.concurrent.CompletableFuture;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.support.CaffeineCache;
import cn.taketoday.cache.support.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class CaffeineCacheManagerTests {

  @Test
  @SuppressWarnings("cast")
  void dynamicMode() {
    CacheManager cm = new CaffeineCacheManager();

    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(CaffeineCache.class);
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2).isInstanceOf(CaffeineCache.class);
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    assertThat(cache3).isInstanceOf(CaffeineCache.class);
    Cache cache3again = cm.getCache("c3");
    assertThat(cache3).isSameAs(cache3again);

    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key1"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key2"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key3"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key3",
            () -> CompletableFuture.completedFuture("value3")));

    cache1.put("key1", "value1");
    assertThat(cache1.get("key1").get()).isEqualTo("value1");
    cache1.put("key2", 2);
    assertThat(cache1.get("key2").get()).isEqualTo(2);
    cache1.put("key3", null);
    assertThat(cache1.get("key3").get()).isNull();
    cache1.evict("key3");
    assertThat(cache1.get("key3")).isNull();
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    cache1.evict("key3");
    assertThat(cache1.get("key3", () -> (String) null)).isNull();
    assertThat(cache1.get("key3", () -> (String) null)).isNull();
  }

  @Test
  @SuppressWarnings("cast")
  void staticMode() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1", "c2");

    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(CaffeineCache.class);
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2).isInstanceOf(CaffeineCache.class);
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
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    cache1.evict("key3");
    assertThat(cache1.get("key3", () -> (String) null)).isNull();
    assertThat(cache1.get("key3", () -> (String) null)).isNull();

    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key1"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key2"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key3"));
    assertThatIllegalStateException().isThrownBy(() -> cache1.retrieve("key3",
            () -> CompletableFuture.completedFuture("value3")));

    cm.setAllowNullValues(false);
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isInstanceOf(CaffeineCache.class);
    assertThat(cache1x).isNotSameAs(cache1);
    Cache cache2x = cm.getCache("c2");
    assertThat(cache2x).isInstanceOf(CaffeineCache.class);
    assertThat(cache2x).isNotSameAs(cache2);
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
  @SuppressWarnings("cast")
  void asyncMode() {
    CaffeineCacheManager cm = new CaffeineCacheManager();
    cm.setAsyncCacheMode(true);

    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(CaffeineCache.class);
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2).isInstanceOf(CaffeineCache.class);
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    assertThat(cache3).isInstanceOf(CaffeineCache.class);
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
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    assertThat(cache1.get("key3", () -> "value3")).isEqualTo("value3");
    cache1.evict("key3");
    assertThat(cache1.get("key3", () -> (String) null)).isNull();
    assertThat(cache1.get("key3", () -> (String) null)).isNull();

    assertThat(cache1.retrieve("key1").join()).isEqualTo("value1");
    assertThat(cache1.retrieve("key2").join()).isEqualTo(2);
    assertThat(cache1.retrieve("key3").join()).isNull();
    cache1.evict("key3");
    assertThat(cache1.retrieve("key3")).isNull();
    assertThat(cache1.retrieve("key3", () -> CompletableFuture.completedFuture("value3")).join())
            .isEqualTo("value3");
    assertThat(cache1.retrieve("key3", () -> CompletableFuture.completedFuture("value3")).join())
            .isEqualTo("value3");
    cache1.evict("key3");
    assertThat(cache1.retrieve("key3")).isNull();
    assertThat(cache1.retrieve("key3", () -> CompletableFuture.completedFuture(null)).join()).isNull();
    assertThat(cache1.retrieve("key3").join()).isNull();
    assertThat(cache1.retrieve("key3", () -> CompletableFuture.completedFuture(null)).join()).isNull();
  }

  @Test
  void changeCaffeineRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(10);
    cm.setCaffeine(caffeine);
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isNotSameAs(cache1);

    cm.setCaffeine(caffeine);  // Set same instance
    Cache cache1xx = cm.getCache("c1");
    assertThat(cache1xx).isSameAs(cache1x);
  }

  @Test
  void changeCaffeineSpecRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    cm.setCaffeineSpec(CaffeineSpec.parse("maximumSize=10"));
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isNotSameAs(cache1);
  }

  @Test
  void changeCacheSpecificationRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    cm.setCacheSpecification("maximumSize=10");
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isNotSameAs(cache1);
  }

  @Test
  void changeCacheLoaderRecreateCache() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    Cache cache1 = cm.getCache("c1");

    @SuppressWarnings("unchecked")
    CacheLoader<Object, Object> loader = mock();

    cm.setCacheLoader(loader);
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isNotSameAs(cache1);

    cm.setCacheLoader(loader);  // Set same instance
    Cache cache1xx = cm.getCache("c1");
    assertThat(cache1xx).isSameAs(cache1x);
  }

  @Test
  void setCacheNameNullRestoreDynamicMode() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    assertThat(cm.getCache("someCache")).isNull();
    cm.setCacheNames(null);
    assertThat(cm.getCache("someCache")).isNotNull();
  }

  @Test
  void cacheLoaderUseLoadingCache() {
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
  void customCacheRegistration() {
    CaffeineCacheManager cm = new CaffeineCacheManager("c1");
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nc = Caffeine.newBuilder().build();
    cm.registerCustomCache("c2", nc);

    Cache cache1 = cm.getCache("c1");
    Cache cache2 = cm.getCache("c2");
    assertThat(nc).isSameAs(cache2.getNativeCache());

    cm.setCaffeine(Caffeine.newBuilder().maximumSize(10));
    assertThat(cm.getCache("c1")).isNotSameAs(cache1);
    assertThat(cm.getCache("c2")).isSameAs(cache2);
  }

}
