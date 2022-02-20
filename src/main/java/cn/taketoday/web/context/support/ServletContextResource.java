/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.context.support;

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
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.ServletContext;

/**
 * {@link cn.taketoday.core.io.Resource} implementation for
 * {@link jakarta.servlet.ServletContext} resources, interpreting
 * relative paths within the web application root directory.
 *
 * <p>Always supports stream access and URL access, but only allows
 * {@code java.io.File} access when the web application archive
 * is expanded.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see jakarta.servlet.ServletContext#getResourceAsStream
 * @see jakarta.servlet.ServletContext#getResource
 * @see jakarta.servlet.ServletContext#getRealPath
 * @since 4.0 2022/2/20 16:27
 */
public class ServletContextResource extends AbstractFileResolvingResource implements ContextResource {

  private final ServletContext servletContext;

  private final String path;

  /**
   * Create a new ServletContextResource.
   * <p>The Servlet spec requires that resource paths start with a slash,
   * even if many containers accept paths without leading slash too.
   * Consequently, the given path will be prepended with a slash if it
   * doesn't already start with one.
   *
   * @param servletContext the ServletContext to load from
   * @param path the path of the resource
   */
  public ServletContextResource(ServletContext servletContext, String path) {
    // check ServletContext
    Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
    this.servletContext = servletContext;

    // check path
    Assert.notNull(path, "Path is required");
    String pathToUse = StringUtils.cleanPath(path);
    if (!pathToUse.startsWith("/")) {
      pathToUse = "/" + pathToUse;
    }
    this.path = pathToUse;
  }

  /**
   * Return the ServletContext for this resource.
   */
  public final ServletContext getServletContext() {
    return this.servletContext;
  }

  /**
   * Return the path for this resource.
   */
  public final String getPath() {
    return this.path;
  }

  /**
   * This implementation checks {@code ServletContext.getResource}.
   *
   * @see jakarta.servlet.ServletContext#getResource(String)
   */
  @Override
  public boolean exists() {
    try {
      URL url = this.servletContext.getResource(this.path);
      return (url != null);
    }
    catch (MalformedURLException ex) {
      return false;
    }
  }

  /**
   * This implementation delegates to {@code ServletContext.getResourceAsStream},
   * which returns {@code null} in case of a non-readable resource (e.g. a directory).
   *
   * @see jakarta.servlet.ServletContext#getResourceAsStream(String)
   */
  @Override
  public boolean isReadable() {
    InputStream is = this.servletContext.getResourceAsStream(this.path);
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
      URL url = this.servletContext.getResource(this.path);
      if (url != null && ResourceUtils.isFileURL(url)) {
        return true;
      }
      else {
        return (this.servletContext.getRealPath(this.path) != null);
      }
    }
    catch (MalformedURLException ex) {
      return false;
    }
  }

  /**
   * This implementation delegates to {@code ServletContext.getResourceAsStream},
   * but throws a FileNotFoundException if no resource found.
   *
   * @see jakarta.servlet.ServletContext#getResourceAsStream(String)
   */
  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is = this.servletContext.getResourceAsStream(this.path);
    if (is == null) {
      throw new FileNotFoundException("Could not open " + this);
    }
    return is;
  }

  /**
   * This implementation delegates to {@code ServletContext.getResource},
   * but throws a FileNotFoundException if no resource found.
   *
   * @see jakarta.servlet.ServletContext#getResource(String)
   */
  @Override
  public URL getLocation() throws IOException {
    URL url = this.servletContext.getResource(this.path);
    if (url == null) {
      throw new FileNotFoundException(
              this + " cannot be resolved to URL because it does not exist");
    }
    return url;
  }

  /**
   * This implementation resolves "file:" URLs or alternatively delegates to
   * {@code ServletContext.getRealPath}, throwing a FileNotFoundException
   * if not found or not resolvable.
   *
   * @see jakarta.servlet.ServletContext#getResource(String)
   * @see jakarta.servlet.ServletContext#getRealPath(String)
   */
  @Override
  public File getFile() throws IOException {
    URL url = this.servletContext.getResource(this.path);
    if (url != null && ResourceUtils.isFileURL(url)) {
      // Proceed with file system resolution...
      return super.getFile();
    }
    else {
      String realPath = ServletUtils.getRealPath(this.servletContext, this.path);
      return new File(realPath);
    }
  }

  /**
   * This implementation creates a ServletContextResource, applying the given path
   * relative to the path of the underlying file of this resource descriptor.
   *
   * @see cn.taketoday.util.ResourceUtils#getRelativePath(String, String)
   */
  @Override
  public Resource createRelative(String relativePath) {
    String pathToUse = ResourceUtils.getRelativePath(this.path, relativePath);
    return new ServletContextResource(this.servletContext, pathToUse);
  }

  /**
   * This implementation returns the name of the file that this ServletContext
   * resource refers to.
   *
   * @see cn.taketoday.util.StringUtils#getFilename(String)
   */
  @Override
  @Nullable
  public String getName() {
    return StringUtils.getFilename(this.path);
  }

  /**
   * This implementation returns a description that includes the ServletContext
   * resource location.
   */
  @Override
  public String toString() {
    return "ServletContext resource [" + this.path + "]";
  }

  @Override
  public String getPathWithinContext() {
    return this.path;
  }

  /**
   * This implementation compares the underlying ServletContext resource locations.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ServletContextResource otherRes)) {
      return false;
    }
    return (this.servletContext.equals(otherRes.servletContext) && this.path.equals(otherRes.path));
  }

  /**
   * This implementation returns the hash code of the underlying
   * ServletContext resource location.
   */
  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

}

