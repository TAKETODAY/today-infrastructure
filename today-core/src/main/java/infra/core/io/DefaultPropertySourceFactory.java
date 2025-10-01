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
package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.env.PropertySource;

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
