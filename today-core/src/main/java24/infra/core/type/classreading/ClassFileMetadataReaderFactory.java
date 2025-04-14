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

import java.io.FileNotFoundException;
import java.io.IOException;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Implementation of the {@link MetadataReaderFactory} interface,
 * using the {@link java.lang.classfile.ClassFile} API for parsing the bytecode.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ClassFileMetadataReaderFactory implements MetadataReaderFactory {

  private final ResourceLoader resourceLoader;

  /**
   * Create a new ClassFileMetadataReaderFactory for the default class loader.
   */
  public ClassFileMetadataReaderFactory() {
    this.resourceLoader = new DefaultResourceLoader();
  }

  /**
   * Create a new ClassFileMetadataReaderFactory for the given resource loader.
   *
   * @param resourceLoader the Spring ResourceLoader to use
   * (also determines the ClassLoader to use)
   */
  public ClassFileMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
  }

  /**
   * Create a new ClassFileMetadataReaderFactory for the given class loader.
   *
   * @param classLoader the ClassLoader to use
   */
  public ClassFileMetadataReaderFactory(@Nullable ClassLoader classLoader) {
    this.resourceLoader =
            (classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
  }

  /**
   * Return the ResourceLoader that this MetadataReaderFactory has been
   * constructed with.
   */
  public final ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  @Override
  public MetadataReader getMetadataReader(String className) throws IOException {
    try {
      String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
              ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
      Resource resource = this.resourceLoader.getResource(resourcePath);
      return getMetadataReader(resource);
    }
    catch (FileNotFoundException ex) {
      // Maybe an inner class name using the dot name syntax? Need to use the dollar syntax here...
      // ClassUtils.forName has an equivalent check for resolution into Class references later on.
      int lastDotIndex = className.lastIndexOf('.');
      if (lastDotIndex != -1) {
        String innerClassName =
                className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
        String innerClassResourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(innerClassName) + ClassUtils.CLASS_FILE_SUFFIX;
        Resource innerClassResource = this.resourceLoader.getResource(innerClassResourcePath);
        if (innerClassResource.exists()) {
          return getMetadataReader(innerClassResource);
        }
      }
      throw ex;
    }
  }

  @Override
  public MetadataReader getMetadataReader(Resource resource) throws IOException {
    return new ClassFileMetadataReader(resource, this.resourceLoader.getClassLoader());
  }
}
