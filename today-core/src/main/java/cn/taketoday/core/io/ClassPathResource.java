/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
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
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ClassLoader#getResourceAsStream(String)
 * @see ClassLoader#getResource(String)
 * @see Class#getResourceAsStream(String)
 * @see Class#getResource(String)
 * @since 2019-05-14 21:47
 */
public class ClassPathResource extends AbstractFileResolvingResource {

  /**
   * Internal representation of the original path supplied by the user,
   * used for creating relative paths and resolving URLs and InputStreams.
   */
  private final String path;

  private final String absolutePath;

  private Class<?> resourceClass;
  private ClassLoader classLoader;

  protected Resource delegate;

  /**
   * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
   * <p>A leading slash will be removed, as the {@code ClassLoader} resource
   * access methods will not accept it.
   * <p>The default class loader will be used for loading the resource.
   *
   * @param path the absolute path within the class path
   * @see ClassUtils#getDefaultClassLoader()
   */
  public ClassPathResource(String path) {
    this(path, ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
   * <p>A leading slash will be removed, as the {@code ClassLoader} resource
   * access methods will not accept it.
   * <p>If the supplied {@code ClassLoader} is {@code null}, the default class
   * loader will be used for loading the resource.
   *
   * @param path the absolute path within the class path
   * @param classLoader the class loader to load the resource with
   * @see ClassUtils#getDefaultClassLoader()
   */
  public ClassPathResource(String path, ClassLoader classLoader) {
    Assert.notNull(path, "Path is required");
    String pathToUse = StringUtils.cleanPath(path);
    if (pathToUse.startsWith("/")) {
      pathToUse = pathToUse.substring(1);
    }
    this.path = pathToUse;
    this.absolutePath = pathToUse;
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new {@code ClassPathResource} for {@code Class} usage.
   * <p>The path can be relative to the given class, or absolute within
   * the class path via a leading slash.
   * <p>If the supplied {@code Class} is {@code null}, the default class
   * loader will be used for loading the resource.
   * <p>This is also useful for resource access within the module system,
   * loading a resource from the containing module of a given {@code Class}.
   * See {@link ModuleResource} and its javadoc.
   *
   * @param path relative or absolute path within the class path
   * @param clazz the class to load resources with
   * @see ClassUtils#getDefaultClassLoader()
   * @see ModuleResource
   */
  public ClassPathResource(String path, Class<?> clazz) {
    Assert.notNull(path, "Path is required");
    this.path = StringUtils.cleanPath(path);

    String absolutePath = this.path;
    if (clazz != null && !absolutePath.startsWith("/")) {
      absolutePath = ClassUtils.classPackageAsResourcePath(clazz) + "/" + absolutePath;
    }
    else if (absolutePath.startsWith("/")) {
      absolutePath = absolutePath.substring(1);
    }
    this.absolutePath = absolutePath;
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
   * @see ClassLoader#getResource(String)
   * @see Class#getResource(String)
   */
  @Override
  public boolean exists() {
    return resolveURL() != null;
  }

  /**
   * This implementation checks for the resolution of a resource URL upfront,
   * then proceeding with {@link AbstractFileResolvingResource}'s length check.
   *
   * @see ClassLoader#getResource(String)
   * @see Class#getResource(String)
   */
  @Override
  public boolean isReadable() {
    URL url = resolveURL();
    return url != null && checkReadable(url);
  }

  /**
   * This implementation opens an {@link InputStream} for the underlying class
   * path resource, if available.
   *
   * @see ClassLoader#getResourceAsStream(String)
   * @see Class#getResourceAsStream(String)
   * @see ClassLoader#getSystemResourceAsStream(String)
   */
  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is;
    if (resourceClass != null) {
      is = resourceClass.getResourceAsStream(path);
    }
    else if (classLoader != null) {
      is = classLoader.getResourceAsStream(absolutePath);
    }
    else {
      is = ClassLoader.getSystemResourceAsStream(absolutePath);
    }
    if (is == null) {
      throw new FileNotFoundException(this + " cannot be opened because it does not exist");
    }
    return is;
  }

  /**
   * Resolves a {@link URL} for the underlying class path resource.
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
        return this.classLoader.getResource(this.absolutePath);
      }
      else {
        return ClassLoader.getSystemResource(this.absolutePath);
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
    return StringUtils.getFilename(this.absolutePath);
  }

  @Override
  public Resource createRelative(String relativePath) {
    final String pathToUse = StringUtils.applyRelativePath(path, relativePath);
    return resourceClass != null
           ? new ClassPathResource(pathToUse, resourceClass)
           : new ClassPathResource(pathToUse, classLoader);
  }

  /**
   * This implementation compares the underlying class path locations and
   * associated class loaders.
   *
   * @see #getPath()
   * @see #getClassLoader()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    return (obj instanceof ClassPathResource other)
            && this.absolutePath.equals(other.absolutePath)
            && ObjectUtils.nullSafeEquals(getClassLoader(), other.getClassLoader());
  }

  /**
   * This implementation returns the hash code of the underlying class path location.
   *
   * @see #getPath()
   */
  @Override
  public int hashCode() {
    return this.absolutePath.hashCode();
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
   * Return the {@link ClassLoader} that this resource will be obtained from.
   *
   * @since 4.0
   */
  @Nullable
  public final ClassLoader getClassLoader() {
    return resourceClass != null ? resourceClass.getClassLoader() : classLoader;
  }

  /**
   * Return the <em>absolute path</em> for this resource, as a
   * {@linkplain StringUtils#cleanPath(String) cleaned} resource path within
   * the class path.
   * <p>The path returned by this method does not have a leading slash and is
   * suitable for use with {@link ClassLoader#getResource(String)}.
   *
   * @since 4.0
   */
  public final String getPath() {
    return this.absolutePath;
  }

  /**
   * This implementation returns a description that includes the absolute
   * class path location.
   */
  @Override
  public String toString() {
    return "class path resource [" + this.absolutePath + ']';
  }

}
