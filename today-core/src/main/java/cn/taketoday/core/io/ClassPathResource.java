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
public class ClassPathResource extends WritableResourceDecorator {

  private final String location;
  private Class<?> resourceClass;
  private ClassLoader classLoader;

  /**
   * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
   * A leading slash will be removed, as the ClassLoader resource access
   * methods will not accept it.
   * <p>The thread context class loader will be used for
   * loading the resource.
   *
   * @param location the absolute path within the class path
   * @see java.lang.ClassLoader#getResourceAsStream(String)
   * @see ClassUtils#getDefaultClassLoader() ()
   */
  public ClassPathResource(String location) {
    this(location, ClassUtils.getDefaultClassLoader());
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
    this.location = pathToUse;
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
    this.location = StringUtils.cleanPath(path);
    this.resourceClass = clazz;
  }

  @Override
  public final Resource getDelegate() {
    if (delegate == null) {
      URL url;
      if (resourceClass != null) {
        url = resourceClass.getResource(location);
      }
      else if (classLoader != null) {
        url = classLoader.getResource(location);
      }
      else {
        url = ClassLoader.getSystemResource(location);
      }
      if (url != null) {
        delegate = ResourceUtils.getResource(url);
      }
      else {
        delegate = new FileSystemResource(location);
      }
    }
    return delegate;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is;
    if (resourceClass != null) {
      is = resourceClass.getResourceAsStream(location);
    }
    else if (this.classLoader != null) {
      is = this.classLoader.getResourceAsStream(location);
    }
    else {
      is = ClassLoader.getSystemResourceAsStream(location);
    }
    if (is == null) {
      throw new FileNotFoundException(this + " cannot be opened because it does not exist");
    }
    return is;
  }

  @Override
  public String getName() {
    return StringUtils.getFilename(location);
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    final String pathToUse = ResourceUtils.getRelativePath(location, relativePath);
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
    return Objects.equals(location, that.location)
            && Objects.equals(resourceClass, that.resourceClass)
            && Objects.equals(classLoader, that.classLoader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, resourceClass, classLoader);
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
    return this.location;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("class path resource [");
    String pathToUse = this.location;
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
