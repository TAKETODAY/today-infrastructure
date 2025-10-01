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

package infra.app.io;

import org.jspecify.annotations.Nullable;

import infra.core.io.FileSystemResource;
import infra.core.io.Resource;

/**
 * Strategy interface registered in {@code spring.factories} and used by
 * {@link ApplicationResourceLoader} to determine the file path of loaded resource
 * when it can also be represented as a {@link FileSystemResource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/20 15:39
 */
public interface FilePathResolver {

  /**
   * Return the {@code path} of the given resource if it can also be represented as
   * a {@link FileSystemResource}.
   *
   * @param location the location used to create the resource
   * @param resource the resource to check
   * @return the file path of the resource or {@code null} if the it is not possible
   * to represent the resource as a {@link FileSystemResource}.
   */
  @Nullable
  String resolveFilePath(String location, Resource resource);

}

