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

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

import infra.core.io.Resource;
import infra.core.type.AnnotationMetadata;
import infra.core.type.ClassMetadata;

/**
 * {@link MetadataReader} implementation based on the {@link ClassFile} API.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
final class ClassFileMetadataReader implements MetadataReader {

  private final Resource resource;

  private final AnnotationMetadata annotationMetadata;

  ClassFileMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
    this.resource = resource;
    this.annotationMetadata = ClassFileClassMetadata.of(parseClassModel(resource), classLoader);
  }

  private static ClassModel parseClassModel(Resource resource) throws IOException {
    try (InputStream is = resource.getInputStream()) {
      byte[] bytes = is.readAllBytes();
      return ClassFile.of().parse(bytes);
    }
  }

  @Override
  public Resource getResource() {
    return this.resource;
  }

  @Override
  public ClassMetadata getClassMetadata() {
    return this.annotationMetadata;
  }

  @Override
  public AnnotationMetadata getAnnotationMetadata() {
    return this.annotationMetadata;
  }

}
