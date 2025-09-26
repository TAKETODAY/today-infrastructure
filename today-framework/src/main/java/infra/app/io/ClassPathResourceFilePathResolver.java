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

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

/**
 * {@link FilePathResolver} for {@link ClassPathResource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class ClassPathResourceFilePathResolver implements FilePathResolver {

  @Nullable
  @Override
  public String resolveFilePath(String location, Resource resource) {
    return (resource instanceof ClassPathResource && !isClassPathUrl(location)) ? location : null;
  }

  private boolean isClassPathUrl(String location) {
    return location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
  }

}
