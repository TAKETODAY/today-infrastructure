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
import java.util.Map;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.util.ConcurrentReferenceHashMap;

/**
 * Caching implementation of the {@link MetadataReaderFactory} interface backed by a
 * {@link ConcurrentReferenceHashMap}, caching {@link MetadataReader} per Framework
 * {@link Resource} handle (i.e. per ".class" file).
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/26 21:51</a>
 * @see CachingMetadataReaderFactory
 * @since 4.0
 */
public class ConcurrentReferenceCachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

  private final Map<String, MetadataReader> classNameCache = new ConcurrentReferenceHashMap<>();

  private final Map<Resource, MetadataReader> resourceCache = new ConcurrentReferenceHashMap<>();

  /**
   * Create a new {@link ConcurrentReferenceCachingMetadataReaderFactory} instance for
   * the default class loader.
   */
  public ConcurrentReferenceCachingMetadataReaderFactory() {
  }

  /**
   * Create a new {@link ConcurrentReferenceCachingMetadataReaderFactory} instance for
   * the given resource loader.
   *
   * @param resourceLoader the Spring ResourceLoader to use (also determines the
   * ClassLoader to use)
   */
  public ConcurrentReferenceCachingMetadataReaderFactory(ResourceLoader resourceLoader) {
    super(resourceLoader);
  }

  /**
   * Create a new {@link ConcurrentReferenceCachingMetadataReaderFactory} instance for
   * the given class loader.
   *
   * @param classLoader the ClassLoader to use
   */
  public ConcurrentReferenceCachingMetadataReaderFactory(ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  public MetadataReader getMetadataReader(String className) throws IOException {
    MetadataReader metadataReader = this.classNameCache.get(className);
    if (metadataReader == null) {
      metadataReader = super.getMetadataReader(className);
      this.classNameCache.put(className, metadataReader);
    }
    return metadataReader;
  }

  @Override
  public MetadataReader getMetadataReader(Resource resource) throws IOException {
    MetadataReader metadataReader = this.resourceCache.get(resource);
    if (metadataReader == null) {
      metadataReader = createMetadataReader(resource);
      this.resourceCache.put(resource, metadataReader);
    }
    return metadataReader;
  }

  /**
   * Create the meta-data reader.
   *
   * @param resource the source resource.
   * @return the meta-data reader
   * @throws IOException on error
   */
  protected MetadataReader createMetadataReader(Resource resource) throws IOException {
    return super.getMetadataReader(resource);
  }

  /**
   * Clear the entire MetadataReader cache, removing all cached class metadata.
   */
  public void clearCache() {
    this.classNameCache.clear();
    this.resourceCache.clear();
  }

}
