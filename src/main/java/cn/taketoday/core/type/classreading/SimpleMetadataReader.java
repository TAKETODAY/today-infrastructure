/*
 * Copyright 2002-2019 the original author or authors.
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

package cn.taketoday.core.type.classreading;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.core.NestedIOException;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.lang.Nullable;

/**
 * {@link MetadataReader} implementation based on an ASM
 * {@link cn.taketoday.core.bytecode.ClassReader}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.5
 */
final class SimpleMetadataReader implements MetadataReader {

  private static final int PARSING_OPTIONS = ClassReader.SKIP_DEBUG
          | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES;

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
        throw new NestedIOException(
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
