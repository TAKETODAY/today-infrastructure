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

package infra.cache.jcache;

import org.jspecify.annotations.Nullable;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import infra.cache.support.AbstractValueAdaptingCache;
import infra.lang.Assert;
import infra.util.function.ThrowingFunction;

/**
 * {@link infra.cache.Cache} implementation on top of a
 * {@link Cache javax.cache.Cache} instance.
 *
 * <p>Note: This class has been updated for JCache 1.0.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JCacheCacheManager
 * @since 4.0
 */
public class JCacheCache extends AbstractValueAdaptingCache {

  private final Cache<Object, Object> cache;

  private final ValueLoaderEntryProcessor valueLoaderEntryProcessor;

  /**
   * Create a {@code JCacheCache} instance.
   *
   * @param jcache backing JCache Cache instance
   */
  public JCacheCache(Cache<Object, Object> jcache) {
    this(jcache, true);
  }

  /**
   * Create a {@code JCacheCache} instance.
   *
   * @param jcache backing JCache Cache instance
   * @param allowNullValues whether to accept and convert null values for this cache
   */
  public JCacheCache(Cache<Object, Object> jcache, boolean allowNullValues) {
    super(allowNullValues);
    Assert.notNull(jcache, "Cache is required");
    this.cache = jcache;
    this.valueLoaderEntryProcessor = new ValueLoaderEntryProcessor();
  }

  @Override
  public final String getName() {
    return this.cache.getName();
  }

  @Override
  public final Cache<Object, Object> getNativeCache() {
    return this.cache;
  }

  @Override
  @Nullable
  protected Object lookup(Object key) {
    return this.cache.get(key);
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <K, V> V get(K key, ThrowingFunction<? super K, ? extends V> valueLoader) {
    try {
      return (V) this.cache.invoke(key, this.valueLoaderEntryProcessor, valueLoader);
    }
    catch (EntryProcessorException ex) {
      throw new ValueRetrievalException(key, valueLoader, ex.getCause());
    }
  }

  @Override
  public void put(Object key, @Nullable Object value) {
    this.cache.put(key, toStoreValue(value));
  }

  @Override
  @Nullable
  public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
    Object previous = this.cache.invoke(key, PutIfAbsentEntryProcessor.INSTANCE, toStoreValue(value));
    return (previous != null ? toValueWrapper(previous) : null);
  }

  @Override
  public void evict(Object key) {
    this.cache.remove(key);
  }

  @Override
  public boolean evictIfPresent(Object key) {
    return this.cache.remove(key);
  }

  @Override
  public void clear() {
    this.cache.removeAll();
  }

  @Override
  public boolean invalidate() {
    boolean notEmpty = this.cache.iterator().hasNext();
    this.cache.removeAll();
    return notEmpty;
  }

  private static final class PutIfAbsentEntryProcessor implements EntryProcessor<Object, Object, Object> {

    private static final PutIfAbsentEntryProcessor INSTANCE = new PutIfAbsentEntryProcessor();

    @Override
    @Nullable
    public Object process(MutableEntry<Object, Object> entry, Object... arguments) throws EntryProcessorException {
      Object existingValue = entry.getValue();
      if (existingValue == null) {
        entry.setValue(arguments[0]);
      }
      return existingValue;
    }
  }

  private final class ValueLoaderEntryProcessor implements EntryProcessor<Object, Object, Object> {

    @Override
    @Nullable
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object process(MutableEntry<Object, Object> entry, Object... arguments) throws EntryProcessorException {
      if (entry.exists()) {
        return fromStoreValue(entry.getValue());
      }
      try {
        Object value = ((ThrowingFunction) arguments[0]).applyWithException(entry.getKey());
        entry.setValue(toStoreValue(value));
        return value;
      }
      catch (Throwable ex) {
        throw new EntryProcessorException("Value loader '%s' failed to compute value for key '%s'"
                .formatted(arguments[0], entry.getKey()), ex);
      }
    }
  }

}
