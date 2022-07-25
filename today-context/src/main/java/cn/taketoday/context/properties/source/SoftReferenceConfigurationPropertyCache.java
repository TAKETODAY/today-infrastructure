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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import cn.taketoday.lang.Nullable;

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

  private final boolean neverExpire;

  @Nullable
  private volatile Duration timeToLive;

  private volatile SoftReference<T> valueRef = new SoftReference<>(null);

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
    return this.valueRef.get();
  }

  protected void setValue(T value) {
    this.valueRef = new SoftReference<>(value);
  }

}
