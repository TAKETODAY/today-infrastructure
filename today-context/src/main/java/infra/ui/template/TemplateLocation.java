/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.ui.template;

import java.io.IOException;

import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;

/**
 * Contains a location that templates can be loaded from.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TemplateLocation {

  private final String path;

  public TemplateLocation(String path) {
    Assert.notNull(path, "Path is required");
    this.path = path;
  }

  /**
   * Determine if this template location exists using the specified
   * {@link PatternResourceLoader}.
   *
   * @param resolver the resolver used to test if the location exists
   * @return {@code true} if the location exists.
   */
  public boolean exists(PatternResourceLoader resolver) {
    Assert.notNull(resolver, "Resolver is required");
    if (resolver.getResource(this.path).exists()) {
      return true;
    }
    try {
      return anyExists(resolver);
    }
    catch (IOException ex) {
      return false;
    }
  }

  private boolean anyExists(PatternResourceLoader resolver) throws IOException {
    String searchPath = this.path;
    if (searchPath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
      searchPath = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX
              + searchPath.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length());
    }
    if (searchPath.startsWith(PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX)) {
      for (Resource resource : resolver.getResources(searchPath)) {
        if (resource.exists()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return this.path;
  }

}
