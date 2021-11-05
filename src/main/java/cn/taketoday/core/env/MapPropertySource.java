/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.env;

import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * {@link PropertySource} that reads keys and values from a {@code Map} object.
 * The underlying map should not contain any {@code null} values in order to
 * comply with {@link #getProperty} and {@link #containsProperty} semantics.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see PropertiesPropertySource
 * @since 4.0
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

  /**
   * Create a new {@code MapPropertySource} with the given name and {@code Map}.
   *
   * @param name the associated name
   * @param source the Map source (without {@code null} values in order to get
   * consistent {@link #getProperty} and {@link #containsProperty} behavior)
   */
  public MapPropertySource(String name, Map<String, Object> source) {
    super(name, source);
  }

  @Override
  @Nullable
  public Object getProperty(String name) {
    return this.source.get(name);
  }

  @Override
  public boolean containsProperty(String name) {
    return this.source.containsKey(name);
  }

  @NonNull
  @Override
  public Set<String> getPropertyNames() {
    return this.source.keySet();
  }

}
