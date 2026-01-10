/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

/**
 * @param <V> value
 * @param <K> key
 * @param <KK> key map type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-01 22:04
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
   * @param v {@code null} or {@link FutureTask}
   * @return newly created instance
   */
  private V createEntry(final K key, KK cacheKey, @Nullable Object v) {
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
