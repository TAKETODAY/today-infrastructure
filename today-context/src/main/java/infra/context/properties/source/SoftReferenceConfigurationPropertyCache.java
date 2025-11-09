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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Simple cache that uses a {@link SoftReference} to cache a value for as long as
 * possible.
 *
 * @param <T> the value type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SoftReferenceConfigurationPropertyCache<T> implements ConfigurationPropertyCaching {

  private static final Duration UNLIMITED = Duration.ZERO;

  static final CacheOverride NO_OP_OVERRIDE = () -> {
  };

  private final boolean neverExpire;

  @Nullable
  private volatile Duration timeToLive;

  private volatile SoftReference<T> value = new SoftReference<>(null);

  @Nullable
  private volatile Instant lastAccessed = now();

  SoftReferenceConfigurationPropertyCache(boolean neverExpire) {
    this.neverExpire = neverExpire;
  }

  @Override
  public void enable() {
    this.timeToLive = UNLIMITED;
  }

  @Override
  public void disable() {
    this.timeToLive = null;
  }

  @Override
  public void setTimeToLive(@Nullable Duration timeToLive) {
    this.timeToLive = (timeToLive == null || timeToLive.isZero()) ? null : timeToLive;
  }

  @Override
  public void clear() {
    this.lastAccessed = null;
  }

  @Override
  public CacheOverride override() {
    if (this.neverExpire) {
      return NO_OP_OVERRIDE;
    }
    ActiveCacheOverride override = new ActiveCacheOverride(this);
    if (override.timeToLive() == null) {
      // Ensure we don't use stale data on the first access
      clear();
    }
    this.timeToLive = UNLIMITED;
    return override;
  }

  void restore(ActiveCacheOverride override) {
    this.timeToLive = override.timeToLive();
    this.lastAccessed = override.lastAccessed();
  }

  /**
   * Get a value from the cache, creating it if necessary.
   *
   * @param factory a factory used to create the item if there is no reference to it.
   * @param refreshAction action called to refresh the value if it has expired
   * @return the value from the cache
   */
  T get(Supplier<T> factory, UnaryOperator<T> refreshAction) {
    T value = getValue();
    if (value == null) {
      value = refreshAction.apply(factory.get());
      setValue(value);
    }
    else if (hasExpired()) {
      value = refreshAction.apply(value);
      setValue(value);
    }
    if (!this.neverExpire) {
      this.lastAccessed = now();
    }
    return value;
  }

  private boolean hasExpired() {
    if (this.neverExpire) {
      return false;
    }
    Duration timeToLive = this.timeToLive;
    Instant lastAccessed = this.lastAccessed;
    if (timeToLive == null || lastAccessed == null) {
      return true;
    }
    return !UNLIMITED.equals(timeToLive) && now().isAfter(lastAccessed.plus(timeToLive));
  }

  protected Instant now() {
    return Instant.now();
  }

  @Nullable
  protected T getValue() {
    return this.value.get();
  }

  protected void setValue(T value) {
    this.value = new SoftReference<>(value);
  }

  /**
   * An active {@link CacheOverride} with a stored time-to-live.
   */
  private record ActiveCacheOverride(SoftReferenceConfigurationPropertyCache<?> cache,
          @Nullable Duration timeToLive, @Nullable Instant lastAccessed, AtomicBoolean active) implements CacheOverride {

    ActiveCacheOverride(SoftReferenceConfigurationPropertyCache<?> cache) {
      this(cache, cache.timeToLive, cache.lastAccessed, new AtomicBoolean());
    }

    @Override
    public void close() {
      if (active().compareAndSet(false, true)) {
        this.cache.restore(this);
      }
    }

  }

}
