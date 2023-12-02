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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Default implementation of the {@link ResourceLoader} interface.
 *
 * <p>Will return a {@link UrlResource} if the location value is a URL,
 * and a {@link ClassPathResource} if it is a non-URL path or a
 * "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/7 17:06
 */
public class DefaultResourceLoader implements ResourceLoader {

  @Nullable
  private ClassLoader classLoader;

  @Nullable
  private LinkedHashSet<ProtocolResolver> protocolResolvers;

  private final ConcurrentHashMap<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);

  /**
   * Create a new DefaultResourceLoader.
   * <p>ClassLoader access will happen using the thread context class loader
   * at the time of actual resource access. For more control, pass
   * a specific ClassLoader to {@link #DefaultResourceLoader(ClassLoader)}.
   *
   * @see java.lang.Thread#getContextClassLoader()
   */
  public DefaultResourceLoader() { }

  /**
   * Create a new DefaultResourceLoader.
   *
   * @param classLoader the ClassLoader to load class path resources with, or {@code null}
   * for using the thread context class loader at the time of actual resource access
   */
  public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Specify the ClassLoader to load class path resources with, or {@code null}
   * for using the thread context class loader at the time of actual resource access.
   * <p>The default is that ClassLoader access will happen using the thread context
   * class loader at the time of actual resource access (since 4.0).
   */
  public void setClassLoader(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Return the ClassLoader to load class path resources with.
   * <p>Will get passed to ClassPathResource's constructor for all
   * ClassPathResource objects created by this resource loader.
   *
   * @see ClassPathResource
   */
  @Override
  @Nullable
  public ClassLoader getClassLoader() {
    return this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader();
  }

  /**
   * Register the given resolver with this resource loader, allowing for
   * additional protocols to be handled.
   * <p>Any such resolver will be invoked ahead of this loader's standard
   * resolution rules. It may therefore also override any default rules.
   *
   * @see #getProtocolResolvers()
   */
  public void addProtocolResolver(ProtocolResolver resolver) {
    Assert.notNull(resolver, "ProtocolResolver is required");
    if (protocolResolvers == null) {
      this.protocolResolvers = new LinkedHashSet<>(4);
    }
    protocolResolvers.add(resolver);
  }

  /**
   * Return the collection of currently registered protocol resolvers,
   * allowing for introspection as well as modification.
   *
   * @see #addProtocolResolver(ProtocolResolver)
   */
  @Nullable
  public Collection<ProtocolResolver> getProtocolResolvers() {
    return this.protocolResolvers;
  }

  /**
   * Obtain a cache for the given value type, keyed by {@link Resource}.
   *
   * @param valueType the value type, e.g. an ASM {@code MetadataReader}
   * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
   */
  @SuppressWarnings("unchecked")
  public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
    return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
  }

  /**
   * Clear all resource caches in this resource loader.
   *
   * @see #getResourceCache
   */
  public void clearResourceCaches() {
    this.resourceCaches.clear();
  }

  @Override
  public Resource getResource(String location) {
    Assert.notNull(location, "Location is required");

    if (protocolResolvers != null) {
      for (ProtocolResolver protocolResolver : protocolResolvers) {
        Resource resource = protocolResolver.resolve(location, this);
        if (resource != null) {
          return resource;
        }
      }
    }

    if (location.startsWith("/")) {
      return getResourceByPath(location);
    }
    else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
      String path = URLDecoder.decode(
              location.substring(CLASSPATH_URL_PREFIX.length()), StandardCharsets.UTF_8);
      return new ClassPathResource(path, getClassLoader());
    }
    else {
      try {
        // Try to parse the location as a URL...
        URL url = ResourceUtils.toURL(location);
        return ResourceUtils.isFileURL(url)
               ? new FileUrlResource(url)
               : new UrlResource(url);
      }
      catch (MalformedURLException ex) {
        // No URL -> resolve as resource path.
        return getResourceByPath(location);
      }
    }
  }

  /**
   * Return a Resource handle for the resource at the given path.
   * <p>The default implementation supports class path locations. This should
   * be appropriate for standalone implementations but can be overridden,
   * e.g. for implementations targeted at a Servlet container.
   *
   * @param path the path to the resource
   * @return the corresponding Resource handle
   * @see ClassPathResource
   */
  protected Resource getResourceByPath(String path) {
    return new ClassPathContextResource(path, getClassLoader());
  }

  /**
   * ClassPathResource that explicitly expresses a context-relative path
   * through implementing the ContextResource interface.
   */
  protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

    public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
      super(path, classLoader);
    }

    @Override
    public String getPathWithinContext() {
      return getPath();
    }

    @Override
    public Resource createRelative(String relativePath) {
      String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
      return new ClassPathContextResource(pathToUse, getClassLoader());
    }

  }

}

