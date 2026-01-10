/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.support;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import infra.cache.Cache;
import infra.lang.Assert;
import infra.util.function.ThrowingFunction;

/**
 * A no operation {@link Cache} implementation suitable for disabling caching.
 *
 * <p>Will simply accept any items into the cache not actually storing them.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/26 14:15
 */
public class NoOpCache implements Cache {

  private final String name;

  /**
   * Create a {@link NoOpCache} instance with the specified name.
   *
   * @param name the name of the cache
   */
  public NoOpCache(String name) {
    Assert.notNull(name, "Cache name is required");
    this.name = name;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Object getNativeCache() {
    return this;
  }

  @Override
  @Nullable
  public ValueWrapper get(Object key) {
    return null;
  }

  @Override
  @Nullable
  public <T> T get(Object key, @Nullable Class<T> type) {
    return null;
  }

  @Nullable
  @Override
  public <K, V> V get(K key, ThrowingFunction<? super K, ? extends V> valueLoader) {
    try {
      return valueLoader.applyWithException(key);
    }
    catch (Throwable ex) {
      throw new ValueRetrievalException(key, valueLoader, ex);
    }
  }

  @Override
  @Nullable
  public CompletableFuture<?> retrieve(Object key) {
    return null;
  }

  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    return valueLoader.get();
  }

  @Override
  public void put(Object key, @Nullable Object value) {
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    return null;
  }

  @Override
  public void evict(Object key) {
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return false;
  }

  @Override
  public void clear() {
  }

  @Override
  public boolean invalidate() {
    return false;
  }

}
