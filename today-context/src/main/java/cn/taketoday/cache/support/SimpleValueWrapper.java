/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.cache.support;

import java.util.Objects;

import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Nullable;

/**
 * Straightforward implementation of {@link Cache.ValueWrapper},
 * simply holding the value as given at construction and returning it from {@link #get()}.
 *
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 20:48
 */
public class SimpleValueWrapper implements Cache.ValueWrapper {

  @Nullable
  private final Object value;

  /**
   * Create a new SimpleValueWrapper instance for exposing the given value.
   *
   * @param value the value to expose (may be {@code null})
   */
  public SimpleValueWrapper(@Nullable Object value) {
    this.value = value;
  }

  /**
   * Simply returns the value as given at construction time.
   */
  @Override
  @Nullable
  public Object get() {
    return this.value;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof Cache.ValueWrapper wrapper && Objects.equals(get(), wrapper.get())));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.value);
  }

  @Override
  public String toString() {
    return "ValueWrapper for [%s]".formatted(this.value);
  }

}
