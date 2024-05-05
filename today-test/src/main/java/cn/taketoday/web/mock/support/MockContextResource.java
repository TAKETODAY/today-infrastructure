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

package cn.taketoday.web.mock.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import cn.taketoday.core.io.AbstractFileResolvingResource;
import cn.taketoday.core.io.ContextResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.MockUtils;

/**
 * {@link cn.taketoday.core.io.Resource} implementation for
 * {@link MockContext} resources, interpreting
 * relative paths within the web application root directory.
 *
 * <p>Always supports stream access and URL access, but only allows
 * {@code java.io.File} access when the web application archive
 * is expanded.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockContext#getResourceAsStream
 * @see MockContext#getResource
 * @see MockContext#getRealPath
 * @since 4.0 2022/2/20 16:27
 */
public class MockContextResource extends AbstractFileResolvingResource implements ContextResource {

  private final MockContext mockContext;

  private final String path;

  /**
   * Create a new MockContextResource.
   * <p>The Servlet spec requires that resource paths start with a slash,
   * even if many containers accept paths without leading slash too.
   * Consequently, the given path will be prepended with a slash if it
   * doesn't already start with one.
   *
   * @param mockContext the MockContext to load from
   * @param path the path of the resource
   */
  public MockContextResource(MockContext mockContext, String path) {
    // check MockContext
    Assert.notNull(mockContext, "Cannot resolve MockContextResource without MockContext");
    this.mockContext = mockContext;

    // check path
    Assert.notNull(path, "Path is required");
    String pathToUse = StringUtils.cleanPath(path);
    if (!pathToUse.startsWith("/")) {
      pathToUse = "/" + pathToUse;
    }
    this.path = pathToUse;
  }

  /**
   * Return the MockContext for this resource.
   */
  public final MockContext getMockContext() {
    return this.mockContext;
  }

  /**
   * Return the path for this resource.
   */
  public final String getPath() {
    return this.path;
  }

  /**
   * This implementation checks {@code MockContext.getResource}.
   *
   * @see MockContext#getResource(String)
   */
  @Override
  public boolean exists() {
    try {
      return mockContext.getResource(path) != null;
    }
    catch (MalformedURLException ex) {
      return false;
    }
  }

  /**
   * This implementation delegates to {@code MockContext.getResourceAsStream},
   * which returns {@code null} in case of a non-readable resource (e.g. a directory).
   *
   * @see MockContext#getResourceAsStream(String)
   */
  @Override
  public boolean isReadable() {
    InputStream is = mockContext.getResourceAsStream(path);
    if (is != null) {
      try {
        is.close();
      }
      catch (IOException ex) {
        // ignore
      }
      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public boolean isFile() {
    try {
      URL url = mockContext.getResource(path);
      if (url != null && ResourceUtils.isFileURL(url)) {
        return true;
      }
      else {
        String realPath = mockContext.getRealPath(path);
        if (realPath == null) {
          return false;
        }
        File file = new File(realPath);
        return file.exists() && file.isFile();
      }
    }
    catch (IOException ex) {
      return false;
    }
  }

  /**
   * This implementation delegates to {@code MockContext.getResourceAsStream},
   * but throws a FileNotFoundException if no resource found.
   *
   * @see MockContext#getResourceAsStream(String)
   */
  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is = mockContext.getResourceAsStream(path);
    if (is == null) {
      throw new FileNotFoundException("Could not open " + this);
    }
    return is;
  }

  /**
   * This implementation delegates to {@code MockContext.getResource},
   * but throws a FileNotFoundException if no resource found.
   *
   * @see MockContext#getResource(String)
   */
  @Override
  public URL getURL() throws IOException {
    URL url = mockContext.getResource(path);
    if (url == null) {
      throw new FileNotFoundException(
              this + " cannot be resolved to URL because it does not exist");
    }
    return url;
  }

  /**
   * This implementation resolves "file:" URLs or alternatively delegates to
   * {@code MockContext.getRealPath}, throwing a FileNotFoundException
   * if not found or not resolvable.
   *
   * @see MockContext#getResource(String)
   * @see MockContext#getRealPath(String)
   */
  @Override
  public File getFile() throws IOException {
    URL url = mockContext.getResource(path);
    if (url != null && ResourceUtils.isFileURL(url)) {
      // Proceed with file system resolution...
      return super.getFile();
    }
    else {
      String realPath = MockUtils.getRealPath(mockContext, path);
      return new File(realPath);
    }
  }

  /**
   * This implementation creates a MockContextResource, applying the given path
   * relative to the path of the underlying file of this resource descriptor.
   *
   * @see cn.taketoday.util.StringUtils#applyRelativePath(String, String)
   */
  @Override
  public Resource createRelative(String relativePath) {
    String pathToUse = StringUtils.applyRelativePath(path, relativePath);
    return new MockContextResource(mockContext, pathToUse);
  }

  /**
   * This implementation returns the name of the file that this MockContext
   * resource refers to.
   *
   * @see cn.taketoday.util.StringUtils#getFilename(String)
   */
  @Override
  @Nullable
  public String getName() {
    return StringUtils.getFilename(path);
  }

  /**
   * This implementation returns a description that includes the MockContext
   * resource location.
   */
  @Override
  public String toString() {
    return "MockContext resource [" + path + "]";
  }

  @Override
  public String getPathWithinContext() {
    return this.path;
  }

  /**
   * This implementation compares the underlying MockContext resource locations.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MockContextResource otherRes)) {
      return false;
    }
    return mockContext.equals(otherRes.mockContext) && path.equals(otherRes.path);
  }

  /**
   * This implementation returns the hash code of the underlying
   * MockContext resource location.
   */
  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

}

