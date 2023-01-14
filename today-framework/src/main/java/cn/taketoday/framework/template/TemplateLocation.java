/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.template;

import java.io.IOException;

import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;

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
    Assert.notNull(path, "Path must not be null");
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
    Assert.notNull(resolver, "Resolver must not be null");
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
