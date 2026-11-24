/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cache.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.cache.Cache;
import infra.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CompositeCacheManagerTests {

  @Test
  void looksUpCachesInDelegateOrderAndReturnsDistinctNames() {
    var first = new ConcurrentMapCacheManager("first", "shared");
    var second = new ConcurrentMapCacheManager("shared", "second");
    var manager = new CompositeCacheManager(first);
    manager.setCacheManagers(List.of(second));
    manager.afterPropertiesSet();

    assertThat(manager.getCache("shared")).isSameAs(first.getCache("shared"));
    assertThat(manager.getCache("second")).isSameAs(second.getCache("second"));
    assertThat(manager.getCache("unknown")).isNull();
    var names = manager.getCacheNames();
    assertThat(names).containsExactlyInAnyOrder("first", "shared", "second").endsWith("second");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> names.add("unknown"));
  }

  @Test
  void fallsBackToANonStoringCacheOnlyForUnknownNames() {
    var delegate = new ConcurrentMapCacheManager("known");
    var manager = new CompositeCacheManager();
    manager.setCacheManagers(List.of(delegate));
    manager.setFallbackToNoOpCache(true);
    manager.afterPropertiesSet();

    assertThat(manager.getCache("known")).isSameAs(delegate.getCache("known"));
    Cache fallback = manager.getCache("unknown");
    assertThat(fallback).isInstanceOf(NoOpCache.class);
    fallback.put("key", "value");
    assertThat(fallback.get("key")).isNull();
    assertThat(manager.getCache("unknown")).isSameAs(fallback);
    assertThat(manager.getCacheNames()).containsExactly("known", "unknown");
  }

  @Test
  void resetsCachesInEveryDelegate() {
    var first = new ConcurrentMapCacheManager();
    var second = new ConcurrentMapCacheManager();
    var manager = new CompositeCacheManager(first, second);
    Cache firstCache = first.getCache("first");
    Cache secondCache = second.getCache("second");
    firstCache.put("key", "value");
    secondCache.put("key", "value");

    manager.resetCaches();

    assertThat(first.getCache("first").get("key")).isNull();
    assertThat(second.getCache("second").get("key")).isNull();
  }

}
