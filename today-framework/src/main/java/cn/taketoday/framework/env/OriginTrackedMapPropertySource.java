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

package cn.taketoday.framework.env;

import java.util.Map;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;
import cn.taketoday.origin.OriginTrackedValue;

/**
 * {@link OriginLookup} backed by a {@link Map} containing {@link OriginTrackedValue
 * OriginTrackedValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
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

  @Override
  public Object getProperty(String name) {
    Object value = super.getProperty(name);
    if (value instanceof OriginTrackedValue) {
      return ((OriginTrackedValue) value).getValue();
    }
    return value;
  }

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
