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
import java.util.Properties;

import cn.taketoday.lang.NonNull;

/**
 * {@link PropertySource} implementation that extracts properties from a
 * {@link Properties} object.
 *
 * <p>Note that because a {@code Properties} object is technically an
 * {@code <Object, Object>} {@link java.util.Hashtable Hashtable}, one may contain
 * non-{@code String} keys or values. This implementation, however is restricted to
 * accessing only {@code String}-based keys and values, in the same fashion as
 * {@link Properties#getProperty} and {@link Properties#setProperty}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class PropertiesPropertySource extends MapPropertySource {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public PropertiesPropertySource(String name, Properties source) {
    super(name, (Map) source);
  }

  protected PropertiesPropertySource(String name, Map<String, Object> source) {
    super(name, source);
  }

  @NonNull
  @Override
  public String[] getPropertyNames() {
    synchronized(this.source) {
      return super.getPropertyNames();
    }
  }

}
