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
 * The default implementation for {@link PropertySourceFactory},
 * wrapping every resource in a {@link ResourcePropertySource}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/28 17:35
 * @see PropertySourceFactory
 * @see ResourcePropertySource
 * @since 4.0
 */
public class DefaultPropertySourceFactory implements PropertySourceFactory {

  @Override
  public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
    return (name != null ? new ResourcePropertySource(name, resource) : new ResourcePropertySource(resource));
  }

}
