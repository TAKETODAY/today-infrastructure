/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.io;

import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * {@link ResourceLoader} implementation that interprets plain resource paths
 * as relative to a given {@code java.lang.Class}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Class#getResource(String)
 * @see ClassPathResource#ClassPathResource(String, Class)
 * @since 4.0 2021/12/30 21:46
 */
public class ClassRelativeResourceLoader extends DefaultResourceLoader {

  private final Class<?> clazz;

  /**
   * Create a new ClassRelativeResourceLoader for the given class.
   *
   * @param clazz the class to load resources through
   */
  public ClassRelativeResourceLoader(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    this.clazz = clazz;
    setClassLoader(clazz.getClassLoader());
  }

  @Override
  protected Resource getResourceByPath(String path) {
    return new ClassRelativeContextResource(path, this.clazz);
  }

  /**
   * ClassPathResource that explicitly expresses a context-relative path
   * through implementing the ContextResource interface.
   */
  private static class ClassRelativeContextResource extends ClassPathResource {

    private final Class<?> clazz;

    public ClassRelativeContextResource(String path, Class<?> clazz) {
      super(path, clazz);
      this.clazz = clazz;
    }

    @Override
    public Resource createRelative(String relativePath) {
      String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
      return new ClassRelativeContextResource(pathToUse, this.clazz);
    }

  }

}
