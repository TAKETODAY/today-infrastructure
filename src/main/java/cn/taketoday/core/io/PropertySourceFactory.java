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
package cn.taketoday.core.io;

import java.io.IOException;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/28 17:34
 * @see DefaultPropertySourceFactory
 * @since 4.0
 */
public interface PropertySourceFactory {

  /**
   * Create a {@link PropertySource} that wraps the given resource.
   *
   * @param name the name of the property source
   * (can be {@code null} in which case the factory implementation
   * will have to generate a name based on the given resource)
   * @param resource the resource (potentially encoded) to wrap
   * @return the new {@link PropertySource} (never {@code null})
   * @throws IOException if resource resolution failed
   */
  PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
