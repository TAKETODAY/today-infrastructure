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

import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.util.ClassUtils;

abstract class AbstractMetadataReaderFactory implements MetadataReaderFactory {

  private final ResourceLoader resourceLoader;

  public AbstractMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
  }

  public AbstractMetadataReaderFactory(@Nullable ClassLoader classLoader) {
    this.resourceLoader =
            (classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
  }

  public AbstractMetadataReaderFactory() {
    this.resourceLoader = new DefaultResourceLoader();
  }

  /**
   * Return the ResourceLoader that this MetadataReaderFactory has been
   * constructed with.
   */
  @Override
  public ResourceLoader getResourceLoader() {
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

}
