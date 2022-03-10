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

package cn.taketoday.cache.concurrent;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class ConcurrentMapCacheManagerTests {

  @Test
  public void testDynamicMode() {
    CacheManager cm = new ConcurrentMapCacheManager();
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1 instanceof ConcurrentMapCache).isTrue();
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2 instanceof ConcurrentMapCache).isTrue();
    Cache cache2again = cm.getCache("c2");
    assertThat(cache2).isSameAs(cache2again);
    Cache cache3 = cm.getCache("c3");
    assertThat(cache3 instanceof ConcurrentMapCache).isTrue();
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
  }

  @Test
  public void testStaticMode() {
    ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1 instanceof ConcurrentMapCache).isTrue();
    Cache cache1again = cm.getCache("c1");
    assertThat(cache1).isSameAs(cache1again);
    Cache cache2 = cm.getCache("c2");
    assertThat(cache2 instanceof ConcurrentMapCache).isTrue();
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
    assertThat(cache1x instanceof ConcurrentMapCache).isTrue();
    assertThat(cache1x != cache1).isTrue();
    Cache cache2x = cm.getCache("c2");
    assertThat(cache2x instanceof ConcurrentMapCache).isTrue();
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
  public void testChangeStoreByValue() {
    ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
    assertThat(cm.isStoreByValue()).isFalse();
    Cache cache1 = cm.getCache("c1");
    assertThat(cache1 instanceof ConcurrentMapCache).isTrue();
    assertThat(((ConcurrentMapCache) cache1).isStoreByValue()).isFalse();
    cache1.put("key", "value");

    cm.setStoreByValue(true);
    assertThat(cm.isStoreByValue()).isTrue();
    Cache cache1x = cm.getCache("c1");
    assertThat(cache1x instanceof ConcurrentMapCache).isTrue();
    assertThat(cache1x != cache1).isTrue();
    assertThat(cache1x.get("key")).isNull();
  }

}
