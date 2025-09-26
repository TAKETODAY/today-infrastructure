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
