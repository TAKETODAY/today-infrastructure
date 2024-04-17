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

package cn.taketoday.framework.io;

import cn.taketoday.core.io.ContextResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * A {@link DefaultResourceLoader}. Plain paths without a
 * qualifier will resolve to file system resources. This is different from
 * {@code DefaultResourceLoader}, which resolves unqualified paths to classpath resources.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationResourceLoader extends DefaultResourceLoader {

  /**
   * Create a new {@code ApplicationResourceLoader}.
   */
  public ApplicationResourceLoader() {
    this(null);
  }

  /**
   * Create a new {@code ApplicationResourceLoader}.
   *
   * @param classLoader the {@link ClassLoader} to load class path resources with, or
   * {@code null} for using the thread context class loader at the time of actual
   * resource access
   */
  public ApplicationResourceLoader(@Nullable ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  protected Resource getResourceByPath(String path) {
    return new FileSystemContextResource(path);
  }

  private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

    FileSystemContextResource(String path) {
      super(path);
    }

    @Override
    public String getPathWithinContext() {
      return getPath();
    }

  }

}
