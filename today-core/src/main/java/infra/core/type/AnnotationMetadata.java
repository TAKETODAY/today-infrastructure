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

package infra.core.type;

import java.util.Set;

import infra.core.type.classreading.MetadataReader;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * <p><strong>WARNING</strong>: If an annotation cannot be loaded because one of
 * its attributes references a {@link Class} or {@link Enum}
 * {@linkplain TypeNotPresentException that is not present in the classpath}, that
 * annotation will not be accessible via the {@code AnnotationMetadata} API.
 * To assist with diagnosing such scenarios, you can set the log level for
 * {@code "infra.core.annotation.MergedAnnotation"} to {@code DEBUG},
 * {@code INFO}, or {@code WARN}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardAnnotationMetadata
 * @see MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 * @since 4.0
 */
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

  /**
   * Determine whether the underlying class has any methods that are
   * annotated (or meta-annotated) with the given annotation type.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   */
  default boolean hasAnnotatedMethods(String annotationName) {
    return !getAnnotatedMethods(annotationName).isEmpty();
  }

  /**
   * Retrieve the method metadata for all methods that are annotated
   * (or meta-annotated) with the given annotation type.
   * <p>For any returned method, {@link MethodMetadata#isAnnotated} will
   * return {@code true} for the given annotation type.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return a set of {@link MethodMetadata} for methods that have a matching
   * annotation. The return value will be an empty set if no methods match
   * the annotation type.
   */
  Set<MethodMetadata> getAnnotatedMethods(String annotationName);

  /**
   * Factory method to create a new {@link AnnotationMetadata} instance
   * for the given class using standard reflection.
   *
   * @param type the class to introspect
   * @return a new {@link AnnotationMetadata} instance
   */
  static AnnotationMetadata introspect(Class<?> type) {
    return StandardAnnotationMetadata.from(type);
  }

}
