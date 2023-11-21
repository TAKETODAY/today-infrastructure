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

package cn.taketoday.cache.jcache;

import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import cn.taketoday.cache.support.AbstractValueAdaptingCache;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.cache.Cache} implementation on top of a
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
    this.valueLoaderEntryProcessor = new ValueLoaderEntryProcessor(this::fromStoreValue, this::toStoreValue);
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

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T get(Object key, Callable<T> valueLoader) {
    try {
      return (T) this.cache.invoke(key, this.valueLoaderEntryProcessor, valueLoader);
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

  private static class PutIfAbsentEntryProcessor implements EntryProcessor<Object, Object, Object> {

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

  private static final class ValueLoaderEntryProcessor implements EntryProcessor<Object, Object, Object> {

    private final Function<Object, Object> fromStoreValue;

    private final Function<Object, Object> toStoreValue;

    private ValueLoaderEntryProcessor(Function<Object, Object> fromStoreValue,
            Function<Object, Object> toStoreValue) {

      this.fromStoreValue = fromStoreValue;
      this.toStoreValue = toStoreValue;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public Object process(MutableEntry<Object, Object> entry, Object... arguments) throws EntryProcessorException {
      Callable<Object> valueLoader = (Callable<Object>) arguments[0];
      if (entry.exists()) {
        return this.fromStoreValue.apply(entry.getValue());
      }
      else {
        Object value;
        try {
          value = valueLoader.call();
        }
        catch (Exception ex) {
          throw new EntryProcessorException("Value loader '" + valueLoader + "' failed " +
                  "to compute value for key '" + entry.getKey() + "'", ex);
        }
        entry.setValue(this.toStoreValue.apply(value));
        return value;
      }
    }
  }

}
