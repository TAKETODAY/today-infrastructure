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
package cn.taketoday.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Resource} implementation for class path resources. Uses either a
 * given {@link ClassLoader} or a given {@link Class} for loading resources.
 *
 * <p>Supports resolution as {@code java.io.File} if the class path
 * resource resides in the file system, but not for resources in a JAR.
 * Always supports resolution as URL.
 *
 * @author TODAY
 * @since 2019-05-14 21:47
 */
public class ClassPathResource extends AbstractFileResolvingResource {

  private final String path;
  private Class<?> resourceClass;
  private ClassLoader classLoader;

  protected Resource delegate;

  /**
   * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
   * A leading slash will be removed, as the ClassLoader resource access
   * methods will not accept it.
   * <p>The thread context class loader will be used for
   * loading the resource.
   *
   * @param path the absolute path within the class path
   * @see java.lang.ClassLoader#getResourceAsStream(String)
   * @see ClassUtils#getDefaultClassLoader() ()
   */
  public ClassPathResource(String path) {
    this(path, ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
   * A leading slash will be removed, as the ClassLoader resource access
   * methods will not accept it.
   *
   * @param path the absolute path within the classpath
   * @param classLoader the class loader to load the resource with,
   * or {@code null} for the thread context class loader
   * @see ClassLoader#getResourceAsStream(String)
   */
  public ClassPathResource(String path, ClassLoader classLoader) {
    Assert.notNull(path, "Path must not be null");
    String pathToUse = StringUtils.cleanPath(path);
    if (pathToUse.startsWith("/")) {
      pathToUse = pathToUse.substring(1);
    }
    this.path = pathToUse;
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new {@code ClassPathResource} for {@code Class} usage.
   * The path can be relative to the given class, or absolute within
   * the classpath via a leading slash.
   *
   * @param path relative or absolute path within the class path
   * @param clazz the class to load resources with
   * @see java.lang.Class#getResourceAsStream
   */
  public ClassPathResource(String path, Class<?> clazz) {
    Assert.notNull(path, "Path must not be null");
    this.path = StringUtils.cleanPath(path);
    this.resourceClass = clazz;
  }

  public final Resource getDelegate() {
    if (delegate == null) {
      URL url;
      if (resourceClass != null) {
        url = resourceClass.getResource(path);
      }
      else if (classLoader != null) {
        url = classLoader.getResource(path);
      }
      else {
        url = ClassLoader.getSystemResource(path);
      }
      if (url != null) {
        delegate = ResourceUtils.getResource(url);
      }
      else {
        delegate = new FileSystemResource(path);
      }
    }
    return delegate;
  }

  /**
   * This implementation returns a URL for the underlying class path resource,
   * if available.
   *
   * @see java.lang.ClassLoader#getResource(String)
   * @see java.lang.Class#getResource(String)
   */
  @Override
  public URL getURL() throws IOException {
    URL url = resolveURL();
    if (url == null) {
      throw new FileNotFoundException(this + " cannot be resolved to URL because it does not exist");
    }
    return url;
  }

  /**
   * This implementation checks for the resolution of a resource URL.
   *
   * @see java.lang.ClassLoader#getResource(String)
   * @see java.lang.Class#getResource(String)
   */
  @Override
  public boolean exists() {
    return resolveURL() != null;
  }

  /**
   * This implementation checks for the resolution of a resource URL upfront,
   * then proceeding with {@link AbstractFileResolvingResource}'s length check.
   *
   * @see java.lang.ClassLoader#getResource(String)
   * @see java.lang.Class#getResource(String)
   */
  @Override
  public boolean isReadable() {
    URL url = resolveURL();
    return url != null && checkReadable(url);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is;
    if (resourceClass != null) {
      is = resourceClass.getResourceAsStream(path);
    }
    else if (this.classLoader != null) {
      is = this.classLoader.getResourceAsStream(path);
    }
    else {
      is = ClassLoader.getSystemResourceAsStream(path);
    }
    if (is == null) {
      throw new FileNotFoundException(this + " cannot be opened because it does not exist");
    }
    return is;
  }

  /**
   * Resolves a URL for the underlying class path resource.
   *
   * @return the resolved URL, or {@code null} if not resolvable
   */
  @Nullable
  protected URL resolveURL() {
    try {
      if (resourceClass != null) {
        return resourceClass.getResource(path);
      }
      else if (this.classLoader != null) {
        return this.classLoader.getResource(path);
      }
      else {
        return ClassLoader.getSystemResource(path);
      }
    }
    catch (IllegalArgumentException ex) {
      // Should not happen according to the JDK's contract:
      // see https://github.com/openjdk/jdk/pull/2662
      return null;
    }
  }

  @Override
  public boolean isDirectory() throws IOException {
    return getDelegate().isDirectory();
  }

  @Override
  public Resource[] list(ResourceFilter filter) throws IOException {
    return getDelegate().list(filter);
  }

  @Override
  public String[] list() throws IOException {
    return getDelegate().list();
  }

  @Override
  public String getName() {
    return StringUtils.getFilename(path);
  }

  @Override
  public Resource createRelative(String relativePath) {
    final String pathToUse = ResourceUtils.getRelativePath(path, relativePath);
    return resourceClass != null
           ? new ClassPathResource(pathToUse, resourceClass)
           : new ClassPathResource(pathToUse, classLoader);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final ClassPathResource that))
      return false;
    return Objects.equals(path, that.path)
            && Objects.equals(resourceClass, that.resourceClass)
            && Objects.equals(classLoader, that.classLoader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, resourceClass, classLoader);
  }

  /**
   * Get Original {@link Resource}
   *
   * @return Original {@link Resource}
   */
  public final Resource getOriginalResource() {
    return getDelegate();
  }

  /**
   * Return the ClassLoader that this resource will be obtained from.
   *
   * @since 4.0
   */
  @Nullable
  public final ClassLoader getClassLoader() {
    return resourceClass != null ? resourceClass.getClassLoader() : classLoader;
  }

  /**
   * Return the path for this resource (as resource path within the class path).
   *
   * @since 4.0
   */
  public final String getPath() {
    return this.path;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("class path resource [");
    String pathToUse = this.path;
    if (this.resourceClass != null && !pathToUse.startsWith("/")) {
      builder.append(ClassUtils.classPackageAsResourcePath(resourceClass));
      builder.append('/');
    }
    if (pathToUse.startsWith("/")) {
      pathToUse = pathToUse.substring(1);
    }
    builder.append(pathToUse);
    builder.append(']');
    return builder.toString();
  }

}
