/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.type.classreading;

import java.io.FileNotFoundException;
import java.io.IOException;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Simple implementation of the {@link MetadataReaderFactory} interface,
 * creating a new ASM {@link cn.taketoday.core.bytecode.ClassReader} for every request.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleMetadataReaderFactory implements MetadataReaderFactory {

  private final ResourceLoader resourceLoader;

  /**
   * Create a new SimpleMetadataReaderFactory for the default class loader.
   */
  public SimpleMetadataReaderFactory() {
    this.resourceLoader = new DefaultResourceLoader();
  }

  /**
   * Create a new SimpleMetadataReaderFactory for the given resource loader.
   *
   * @param resourceLoader the ResourceLoader to use
   * (also determines the ClassLoader to use)
   */
  public SimpleMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
  }

  /**
   * Create a new SimpleMetadataReaderFactory for the given class loader.
   *
   * @param classLoader the ClassLoader to use
   */
  public SimpleMetadataReaderFactory(@Nullable ClassLoader classLoader) {
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
    return new SimpleMetadataReader(resource, this.resourceLoader.getClassLoader());
  }

}
