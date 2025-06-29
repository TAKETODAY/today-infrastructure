/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.type.classreading;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;

/**
 * Caching implementation of the {@link MetadataReaderFactory} interface,
 * caching a {@link MetadataReader} instance per {@link Resource} handle
 * (i.e. per ".class" file).
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CachingMetadataReaderFactory extends AbstractMetadataReaderFactory {

  /** Default maximum number of entries for a local MetadataReader cache: 256. */
  public static final int DEFAULT_CACHE_LIMIT = 256;

  private final MetadataReaderFactory delegate;

  /** MetadataReader cache: either local or shared at the ResourceLoader level. */
  private @Nullable Map<Resource, MetadataReader> metadataReaderCache;

  /**
   * Create a new CachingMetadataReaderFactory for the default class loader,
   * using a local resource cache.
   */
  public CachingMetadataReaderFactory() {
    this(MetadataReaderFactory.create((ClassLoader) null));
  }

  /**
   * Create a new CachingMetadataReaderFactory for the given {@link ClassLoader},
   * using a local resource cache.
   *
   * @param classLoader the ClassLoader to use
   */
  public CachingMetadataReaderFactory(@Nullable ClassLoader classLoader) {
    this(MetadataReaderFactory.create(classLoader));
  }

  /**
   * Create a new CachingMetadataReaderFactory for the given {@link ResourceLoader},
   * using a shared resource cache if supported or a local resource cache otherwise.
   *
   * @param resourceLoader the Spring ResourceLoader to use
   * (also determines the ClassLoader to use)
   * @see DefaultResourceLoader#getResourceCache
   */
  public CachingMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
    this(MetadataReaderFactory.create(resourceLoader));
  }

  CachingMetadataReaderFactory(MetadataReaderFactory delegate) {
    super(delegate.getResourceLoader());
    this.delegate = delegate;
    if (getResourceLoader() instanceof DefaultResourceLoader defaultResourceLoader) {
      this.metadataReaderCache = defaultResourceLoader.getResourceCache(MetadataReader.class);
    }
    else {
      setCacheLimit(DEFAULT_CACHE_LIMIT);
    }
  }

  /**
   * Specify the maximum number of entries for the MetadataReader cache.
   * <p>Default is 256 for a local cache, whereas a shared cache is
   * typically unbounded. This method enforces a local resource cache,
   * even if the {@link ResourceLoader} supports a shared resource cache.
   */
  public void setCacheLimit(int cacheLimit) {
    if (cacheLimit <= 0) {
      this.metadataReaderCache = null;
    }
    else if (metadataReaderCache instanceof LocalResourceCache lrc) {
      lrc.setCacheLimit(cacheLimit);
    }
    else {
      this.metadataReaderCache = new LocalResourceCache(cacheLimit);
    }
  }

  /**
   * Return the maximum number of entries for the MetadataReader cache.
   */
  public int getCacheLimit() {
    if (metadataReaderCache instanceof LocalResourceCache lrc) {
      return lrc.getCacheLimit();
    }
    else {
      return (metadataReaderCache != null ? Integer.MAX_VALUE : 0);
    }
  }

  @Override
  public MetadataReader getMetadataReader(Resource resource) throws IOException {
    if (metadataReaderCache instanceof ConcurrentMap) {
      // No synchronization necessary...
      MetadataReader metadataReader = metadataReaderCache.get(resource);
      if (metadataReader == null) {
        metadataReader = delegate.getMetadataReader(resource);
        metadataReaderCache.put(resource, metadataReader);
      }
      return metadataReader;
    }
    else if (metadataReaderCache != null) {
      synchronized(metadataReaderCache) {
        MetadataReader metadataReader = metadataReaderCache.get(resource);
        if (metadataReader == null) {
          metadataReader = delegate.getMetadataReader(resource);
          metadataReaderCache.put(resource, metadataReader);
        }
        return metadataReader;
      }
    }
    else {
      return delegate.getMetadataReader(resource);
    }
  }

  /**
   * Clear the local MetadataReader cache, if any, removing all cached class metadata.
   */
  public void clearCache() {
    if (metadataReaderCache instanceof LocalResourceCache) {
      synchronized(metadataReaderCache) {
        metadataReaderCache.clear();
      }
    }
    else if (metadataReaderCache != null) {
      // Shared resource cache -> reset to local cache.
      setCacheLimit(DEFAULT_CACHE_LIMIT);
    }
  }

  @SuppressWarnings("serial")
  private static class LocalResourceCache extends LinkedHashMap<Resource, MetadataReader> {

    private volatile int cacheLimit;

    public LocalResourceCache(int cacheLimit) {
      super(cacheLimit, 0.75f, true);
      this.cacheLimit = cacheLimit;
    }

    public void setCacheLimit(int cacheLimit) {
      this.cacheLimit = cacheLimit;
    }

    public int getCacheLimit() {
      return this.cacheLimit;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
      return size() > this.cacheLimit;
    }
  }

}
