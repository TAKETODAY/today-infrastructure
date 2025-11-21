/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.cache.concurrent;

import org.junit.jupiter.api.Test;

import infra.cache.Cache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class ConcurrentMapCacheManagerTests {

  @Test
  void testDynamicMode() {
    ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager();
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2).isInstanceOf(ConcurrentMapCache.class);
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    assertThat(cache3).isInstanceOf(ConcurrentMapCache.class);
    Cache cache3again = cm.getCache("c3");
    assertThat(cache3).isSameAs(cache3again);

    cache1.put("key1", "value1");
    assertThat(cache1.get("key1").get()).isEqualTo("value1");
    cache1.put("key2", 2);
    assertThat(cache1.get("key2").get()).isEqualTo(2);
    cache1.put("key3", null);
    assertThat(cache1.get("key3").get()).isNull();
    cache1.put("key3", null);
    assertThat(cache1.get("key3").get()).isNull();
    cache1.evict("key3");
    assertThat(cache1.get("key3")).isNull();

    assertThat(cache1.putIfAbsent("key1", "value1x").get()).isEqualTo("value1");
    assertThat(cache1.get("key1").get()).isEqualTo("value1");
    assertThat(cache1.putIfAbsent("key2", 2.1).get()).isEqualTo(2);
    assertThat(cache1.putIfAbsent("key3", null)).isNull();
    assertThat(cache1.get("key3").get()).isNull();
    assertThat(cache1.putIfAbsent("key3", null).get()).isNull();
    assertThat(cache1.get("key3").get()).isNull();
    cache1.evict("key3");
    assertThat(cache1.get("key3")).isNull();

    cm.removeCache("c1");
    assertThat(cm.getCache("c1")).isNotSameAs(cache1);
    assertThat(cm.getCache("c2")).isSameAs(cache2);

    cm.resetCaches();
    assertThat(cm.getCache("c1")).isNotSameAs(cache1);
    assertThat(cm.getCache("c2")).isNotSameAs(cache2);
  }

  @Test
  void testStaticMode() {
    ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2).isInstanceOf(ConcurrentMapCache.class);
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
    assertThat(cache1x).isInstanceOf(ConcurrentMapCache.class);
    assertThat(cache1x).isNotSameAs(cache1);
    Cache cache2x = cm.getCache("c2");
    assertThat(cache2x).isInstanceOf(ConcurrentMapCache.class);
    assertThat(cache2x).isNotSameAs(cache2);
    Cache cache3x = cm.getCache("c3");
    assertThat(cache3x).isNull();

    cache1x.put("key1", "value1");
    assertThat(cache1x.get("key1").get()).isEqualTo("value1");
    cache1x.put("key2", 2);
    assertThat(cache1x.get("key2").get()).isEqualTo(2);

    cm.setAllowNullValues(true);
    Cache cache1y = cm.getCache("c1");
    Cache cache2y = cm.getCache("c2");

    cache1y.put("key3", null);
    assertThat(cache1y.get("key3").get()).isNull();
    cache1y.evict("key3");
    assertThat(cache1y.get("key3")).isNull();
    cache2y.put("key4", "value4");
    assertThat(cache2y.get("key4").get()).isEqualTo("value4");

    cm.removeCache("c1");
    assertThat(cm.getCache("c1")).isNull();
    assertThat(cm.getCache("c2")).isSameAs(cache2y);
    assertThat(cache2y.get("key4").get()).isEqualTo("value4");

    cm.resetCaches();
    assertThat(cm.getCache("c1")).isNull();
    assertThat(cm.getCache("c2")).isSameAs(cache2y);
    assertThat(cache2y.get("key4")).isNull();
  }

  @Test
  void testChangeStoreByValue() {
    ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
    assertThat(cm.isStoreByValue()).isFalse();
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
    assertThat(((ConcurrentMapCache) cache1).isStoreByValue()).isFalse();
    cache1.put("key", "value");

    cm.setStoreByValue(true);
    assertThat(cm.isStoreByValue()).isTrue();
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x).isInstanceOf(ConcurrentMapCache.class);
    assertThat(cache1x).isNotSameAs(cache1);
    assertThat(cache1x.get("key")).isNull();
  }

}
