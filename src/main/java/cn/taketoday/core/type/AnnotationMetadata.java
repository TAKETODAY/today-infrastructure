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

package cn.taketoday.core.type;

import java.util.Set;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @see StandardAnnotationMetadata
 * @see cn.taketoday.core.type.classreading.MetadataReader#getAnnotationMetadata()
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
