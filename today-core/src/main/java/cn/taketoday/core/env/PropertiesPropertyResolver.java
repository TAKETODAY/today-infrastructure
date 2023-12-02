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

package cn.taketoday.core.env;

import java.util.Properties;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/3 15:44
 */
public class PropertiesPropertyResolver extends MapPropertyResolver {

  /**
   * Create a new resolver against the given Properties.
   *
   * @param keyValues the Map source (without {@code null} values in order to get
   * consistent {@link #getProperty} and {@link #containsProperty} behavior)
   */
  public PropertiesPropertyResolver(@Nullable Properties keyValues) {
    super(keyValues);
  }

}
