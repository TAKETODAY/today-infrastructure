/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
