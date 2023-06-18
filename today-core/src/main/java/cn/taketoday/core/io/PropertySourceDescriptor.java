/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.io;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;

/**
 * Describe a {@link PropertySource}.
 *
 * @param locations the locations to consider
 * @param ignoreResourceNotFound whether to fail if a location does not exist
 * @param name the name of the property source, or {@code null} to infer one
 * @param propertySourceFactory the {@link PropertySourceFactory} to use, or
 * {@code null} to use the default
 * @param encoding the encoding, or {@code null} to use the default encoding
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public record PropertySourceDescriptor(
        List<String> locations, boolean ignoreResourceNotFound, @Nullable String name,
        @Nullable Class<? extends PropertySourceFactory> propertySourceFactory, @Nullable String encoding) {

  /**
   * Create a descriptor with the specified locations.
   *
   * @param locations the locations to consider
   */
  public PropertySourceDescriptor(String... locations) {
    this(Arrays.asList(locations), false, null, null, null);
  }

}
