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
package cn.taketoday.core.bytecode.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

/**
 * @author TODAY <br>
 * 2019-09-01 22:04
 */
@SuppressWarnings({ "unchecked" })
final class LoadingCache<K, KK, V> {

  public final Function<K, V> loader;
  public final Function<K, KK> keyMapper;
  public final ConcurrentHashMap<KK, Object> map = new ConcurrentHashMap<>();

  public LoadingCache(Function<K, KK> keyMapper, Function<K, V> loader) {
    this.loader = loader;
    this.keyMapper = keyMapper;
  }

  public V get(K key) {
    final KK cacheKey = keyMapper.apply(key);
    Object v = map.get(cacheKey);
    if (v != null && !(v instanceof FutureTask)) {
      return (V) v;
    }

    return createEntry(key, cacheKey, v);
  }

  /**
   * Loads entry to the cache. If entry is missing, put {@link FutureTask} first
   * so other competing thread might wait for the result.
   *
   * @param key original key that would be used to load the instance
   * @param cacheKey key that would be used to store the entry in internal map
   * @param v null or {@link FutureTask<V>}
   * @return newly created instance
   */
  private V createEntry(final K key, KK cacheKey, Object v) {
    FutureTask<V> task;
    boolean created = false;
    if (v != null) {
      // Another thread is already loading an instance
      task = (FutureTask<V>) v;
    }
    else {
      task = new FutureTask<>(() -> loader.apply(key));

      Object prevTask = map.putIfAbsent(cacheKey, task);
      if (prevTask == null) {
        // creator does the load
        created = true;
        task.run();
      }
      else if (prevTask instanceof FutureTask) {
        task = (FutureTask<V>) prevTask;
      }
      else {
        return (V) prevTask;
      }
    }

    try {
      V result = task.get();
      if (created) {
        map.put(cacheKey, result);
      }
      return result;
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while loading cache item", e);
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw ((RuntimeException) cause);
      }
      throw new IllegalStateException("Unable to load cache item", cause);
    }
  }

}
