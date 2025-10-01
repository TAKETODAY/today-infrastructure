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

import infra.bytecode.ClassReader;
import infra.core.io.Resource;
import infra.core.type.AnnotationMetadata;
import infra.core.type.ClassMetadata;

/**
 * {@link MetadataReader} implementation based on an ASM
 * {@link ClassReader}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SimpleMetadataReader implements MetadataReader {

  private static final int PARSING_OPTIONS =
          ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES;

  private final Resource resource;
  private final AnnotationMetadata annotationMetadata;

  SimpleMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
    var visitor = new SimpleAnnotationMetadataReadingVisitor(classLoader);
    getClassReader(resource).accept(visitor, PARSING_OPTIONS);
    this.resource = resource;
    this.annotationMetadata = visitor.getMetadata();
  }

  private static ClassReader getClassReader(Resource resource) throws IOException {
    try (InputStream is = resource.getInputStream()) {
      try {
        return new ClassReader(is);
      }
      catch (IllegalArgumentException ex) {
        throw new ClassFormatException("ASM ClassReader failed to parse class file - " +
                "probably due to a new Java class file version that is not supported yet. " +
                "Consider compiling with a lower '-target' or upgrade your framework version. " +
                "Affected class: " + resource, ex);
      }
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
