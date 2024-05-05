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

package cn.taketoday.core.io;

/**
 * {@link ResourceLoader} implementation that resolves plain paths as
 * file system resources rather than as class path resources
 * (the latter is {@link DefaultResourceLoader}'s default strategy).
 *
 * <p><b>NOTE:</b> Plain paths will always be interpreted as relative
 * to the current VM working directory, even if they start with a slash.
 * (This is consistent with the semantics in a Mock container.)
 * <b>Use an explicit "file:" prefix to enforce an absolute file path.</b>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultResourceLoader
 * @since 4.0 2022/2/20 16:39
 */
public class FileSystemResourceLoader extends DefaultResourceLoader {

  /**
   * Resolve resource paths as file system paths.
   * <p>Note: Even if a given path starts with a slash, it will get
   * interpreted as relative to the current VM working directory.
   *
   * @param path the path to the resource
   * @return the corresponding Resource handle
   * @see FileSystemResource
   * @see cn.taketoday.web.mock.support.MockContextResourceLoader#getResourceByPath
   */
  @Override
  protected Resource getResourceByPath(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return new FileSystemContextResource(path);
  }

  /**
   * FileSystemResource that explicitly expresses a context-relative path
   * through implementing the ContextResource interface.
   */
  private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

    public FileSystemContextResource(String path) {
      super(path);
    }

    @Override
    public String getPathWithinContext() {
      return getPath();
    }
  }

}
