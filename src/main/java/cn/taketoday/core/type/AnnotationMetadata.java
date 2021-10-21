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

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
   * Get the fully qualified class names of all annotation types that
   * are <em>present</em> on the underlying class.
   *
   * @return the annotation type names
   */
  default Set<String> getAnnotationTypes() {
    return getAnnotations().stream()
            .filter(MergedAnnotation::isDirectlyPresent)
            .map(annotation -> annotation.getType().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Get the fully qualified class names of all meta-annotation types that
   * are <em>present</em> on the given annotation type on the underlying class.
   *
   * @param annotationName the fully qualified class name of the meta-annotation
   * type to look for
   * @return the meta-annotation type names, or an empty set if none found
   */
  default Set<String> getMetaAnnotationTypes(String annotationName) {
    MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
    if (!annotation.isPresent()) {
      return Collections.emptySet();
    }
    return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream()
            .map(mergedAnnotation -> mergedAnnotation.getType().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Determine whether an annotation of the given type is <em>present</em> on
   * the underlying class.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return {@code true} if a matching annotation is present
   */
  default boolean hasAnnotation(String annotationName) {
    return getAnnotations().isDirectlyPresent(annotationName);
  }

  /**
   * Determine whether the underlying class has an annotation that is itself
   * annotated with the meta-annotation of the given type.
   *
   * @param metaAnnotationName the fully qualified class name of the
   * meta-annotation type to look for
   * @return {@code true} if a matching meta-annotation is present
   */
  default boolean hasMetaAnnotation(String metaAnnotationName) {
    return getAnnotations().get(metaAnnotationName,
            MergedAnnotation::isMetaPresent).isPresent();
  }

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
