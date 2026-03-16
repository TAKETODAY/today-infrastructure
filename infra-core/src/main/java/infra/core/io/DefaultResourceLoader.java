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

import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;
import infra.util.ResourceUtils;
import infra.util.StringUtils;
import infra.util.function.IOConsumer;

import static infra.core.io.PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX;

/**
 * Default implementation of the {@link ResourceLoader} interface.
 *
 * <p>Will return a {@link UrlResource} if the location value is a URL,
 * and a {@link ClassPathResource} if it is a non-URL path or a
 * "classpath:" pseudo-URL.
 *
 * <p> All the {@link ProtocolResolver ProtocolResolvers} registered in
 * a {@code today.strategies} file applied to it.
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

  @Nullable
  private ConcurrentHashMap<Class<?>, Map<Resource, ?>> resourceCaches;

  /**
   * Create a new DefaultResourceLoader.
   * <p>ClassLoader access will happen using the thread context class loader
   * at the time of actual resource access. For more control, pass
   * a specific ClassLoader to {@link #DefaultResourceLoader(ClassLoader)}.
   *
   * @see java.lang.Thread#getContextClassLoader()
   */
  public DefaultResourceLoader() {
    this(null);
  }

  /**
   * Create a new DefaultResourceLoader.
   *
   * @param classLoader the ClassLoader to load class path resources with, or {@code null}
   * for using the thread context class loader at the time of actual resource access
   */
  public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
    List<ProtocolResolver> resolvers = TodayStrategies.find(ProtocolResolver.class, classLoader);
    if (!resolvers.isEmpty()) {
      this.protocolResolvers = new LinkedHashSet<>(resolvers);
    }
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
    ConcurrentHashMap<Class<?>, Map<Resource, ?>> resourceCaches = this.resourceCaches;
    if (resourceCaches == null) {
      resourceCaches = new ConcurrentHashMap<>(4);
      this.resourceCaches = resourceCaches;
    }
    return (Map<Resource, T>) resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
  }

  /**
   * Clear all resource caches in this resource loader.
   *
   * @see #getResourceCache
   */
  public void clearResourceCaches() {
    if (resourceCaches != null) {
      resourceCaches.clear();
    }
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
    else if (location.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
      return new ClassPathAllResource(location.substring(CLASSPATH_ALL_URL_PREFIX.length()), getClassLoader());
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
   * e.g. for implementations targeted at a Web container.
   *
   * @param path the path to the resource
   * @return the corresponding Resource handle
   * @see ClassPathResource
   */
  protected Resource getResourceByPath(String path) {
    return new ClassPathContextResource(path, getClassLoader());
  }

  /**
   * A multi-content ClassPathResource handle that can expose the content
   * from all matching resources in the classpath.
   *
   * @since 5.0
   */
  protected static class ClassPathAllResource extends ClassPathResource {

    public ClassPathAllResource(String path, @Nullable ClassLoader classLoader) {
      super(path, classLoader);
    }

    @Override
    public boolean isFile() {
      return false;
    }

    @Override
    public URL getURL() throws IOException {
      throw new FileNotFoundException(
              this + " cannot be resolved to single URL or File - use 'classpath:' instead");
    }

    @Override
    public long contentLength() throws IOException {
      long combinedLength = 0;
      ClassLoader cl = getClassLoader();
      Enumeration<URL> urls = cl != null ? cl.getResources(getPath()) : ClassLoader.getSystemResources(getPath());
      while (urls.hasMoreElements()) {
        URLConnection con = urls.nextElement().openConnection();
        long length = con.getContentLengthLong();
        if (length < 0) {
          return -1;
        }
        combinedLength += length;
      }
      return combinedLength;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      List<InputStream> streams = new ArrayList<>();
      ClassLoader cl = getClassLoader();
      Enumeration<URL> urls = (cl != null ? cl.getResources(getPath()) : ClassLoader.getSystemResources(getPath()));
      while (urls.hasMoreElements()) {
        try {
          streams.add(urls.nextElement().openStream());
        }
        catch (IOException ex) {
          streams.forEach(stream -> {
            try {
              stream.close();
            }
            catch (IOException ex2) {
              ex.addSuppressed(ex2);
            }
          });
          throw ex;
        }
      }
      return switch (streams.size()) {
        case 0 -> InputStream.nullInputStream();
        case 1 -> streams.get(0);
        default -> new SequenceInputStream(Collections.enumeration(streams));
      };
    }

    @Override
    public void consumeContent(IOConsumer<InputStream> consumer) throws IOException {
      ClassLoader cl = getClassLoader();
      Enumeration<URL> urls = (cl != null ? cl.getResources(getPath()) : ClassLoader.getSystemResources(getPath()));
      while (urls.hasMoreElements()) {
        try (InputStream inputStream = urls.nextElement().openStream()) {
          consumer.accept(inputStream);
        }
      }
    }

    @Override
    public Resource createRelative(String relativePath) {
      String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
      return new ClassPathAllResource(pathToUse, getClassLoader());
    }

    @Override
    public String toString() {
      return "'classpath*:' resource [%s]".formatted(getPath());
    }

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

