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

package infra.app.env;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.env.MapPropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.origin.OriginTrackedValue;

/**
 * {@link OriginLookup} backed by a {@link Map} containing {@link OriginTrackedValue
 * OriginTrackedValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see OriginTrackedValue
 * @since 4.0
 */
public final class OriginTrackedMapPropertySource extends MapPropertySource implements OriginLookup<String> {

  private final boolean immutable;

  /**
   * Create a new {@link OriginTrackedMapPropertySource} instance.
   *
   * @param name the property source name
   * @param source the underlying map source
   */
  @SuppressWarnings("rawtypes")
  public OriginTrackedMapPropertySource(String name, Map source) {
    this(name, source, false);
  }

  /**
   * Create a new {@link OriginTrackedMapPropertySource} instance.
   *
   * @param name the property source name
   * @param source the underlying map source
   * @param immutable if the underlying source is immutable and guaranteed not to change
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public OriginTrackedMapPropertySource(String name, Map source, boolean immutable) {
    super(name, source);
    this.immutable = immutable;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    Object value = super.getProperty(name);
    if (value instanceof OriginTrackedValue) {
      return ((OriginTrackedValue) value).getValue();
    }
    return value;
  }

  @Nullable
  @Override
  public Origin getOrigin(String name) {
    Object value = super.getProperty(name);
    if (value instanceof OriginTrackedValue) {
      return ((OriginTrackedValue) value).getOrigin();
    }
    return null;
  }

  @Override
  public boolean isImmutable() {
    return this.immutable;
  }

}
