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

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.lang.Nullable;

/**
 * {@link MetadataReader} implementation based on an ASM
 * {@link ClassReader}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 */
final class SimpleMetadataReader implements MetadataReader {

  private static final int PARSING_OPTIONS =
          ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES;

  private final Resource resource;
  private final AnnotationMetadata annotationMetadata;

  SimpleMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
    SimpleAnnotationMetadataReadingVisitor visitor = new SimpleAnnotationMetadataReadingVisitor(classLoader);
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
        throw new IOException(
                "ASM ClassReader failed to parse class file - " +
                        "probably due to a new Java class file version that isn't supported yet: " + resource, ex);
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
