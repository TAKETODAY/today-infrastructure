/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotation.Adapt;
import cn.taketoday.core.annotation.MergedAnnotationCollectors;
import cn.taketoday.core.annotation.MergedAnnotationPredicates;
import cn.taketoday.core.annotation.MergedAnnotationSelectors;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require the
 * class-loading.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationMetadata
 * @see MethodMetadata
 * @since 4.0
 */
public interface AnnotatedTypeMetadata {

  /**
   * Return annotation details based on the direct annotations of the
   * underlying element.
   *
   * @return merged annotations based on the direct annotations
   */
  MergedAnnotations getAnnotations();

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the annotation type to get
   * @return a {@link MergedAnnotation} instance
   */
  default <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annotationType) {
    return getAnnotations().get(annotationType);
  }

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to get
   * @return a {@link MergedAnnotation} instance
   */
  default <A extends Annotation> MergedAnnotation<A> getAnnotation(String annotationType) {
    return getAnnotations().get(annotationType);
  }

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
   * @param annotationType the class of the meta-annotation type to look for
   * @return the meta-annotation type names, or an empty set if none found
   */
  default Set<String> getMetaAnnotationTypes(Class<? extends Annotation> annotationType) {
    return getMetaAnnotationTypes(annotationType.getName());
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
    var annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
    if (annotation.isPresent()) {
      return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS)
              .stream()
              .map(mergedAnnotation -> mergedAnnotation.getType().getName())
              .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return Collections.emptySet();
  }

  /**
   * Determine whether an annotation of the given type is <em>present</em> on
   * the underlying class.
   *
   * @param annotationType the class of the annotation type to look for
   * @return {@code true} if a matching annotation is present
   */
  default boolean hasAnnotation(Class<? extends Annotation> annotationType) {
    return hasAnnotation(annotationType.getName());
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
   * @param metaAnnotationType the class of the meta-annotation type to look for
   * @return {@code true} if a matching meta-annotation is present
   */
  default boolean hasMetaAnnotation(Class<? extends Annotation> metaAnnotationType) {
    return hasMetaAnnotation(metaAnnotationType.getName());
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
    return getAnnotations()
            .get(metaAnnotationName, MergedAnnotation::isMetaPresent)
            .isPresent();
  }

  /**
   * Determine whether the underlying element has an annotation or meta-annotation
   * of the given type defined.
   * <p>If this method returns {@code true}, then
   * {@link #getAnnotationAttributes} will return a non-null Map.
   *
   * @param annotationType the class of the annotation type to look for
   * @return whether a matching annotation is defined
   */
  default boolean isAnnotated(Class<? extends Annotation> annotationType) {
    return isAnnotated(annotationType.getName());
  }

  /**
   * Determine whether the underlying element has an annotation or meta-annotation
   * of the given type defined.
   * <p>If this method returns {@code true}, then
   * {@link #getAnnotationAttributes} will return a non-null Map.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return whether a matching annotation is defined
   */
  default boolean isAnnotated(String annotationName) {
    return getAnnotations().isPresent(annotationName);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation),
   * also taking attribute overrides on composed annotations into account.
   *
   * @param annotationType the class of the annotation type to look for
   * @return a Map of attributes, with the attribute name as key (e.g. "value")
   * and the defined attribute value as Map value. This return value will be
   * {@code null} if no matching annotation is defined.
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(Class<? extends Annotation> annotationType) {
    return getAnnotationAttributes(annotationType.getName(), false);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation),
   * also taking attribute overrides on composed annotations into account.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return a Map of attributes, with the attribute name as key (e.g. "value")
   * and the defined attribute value as Map value. This return value will be
   * {@code null} if no matching annotation is defined.
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(String annotationName) {
    return getAnnotationAttributes(annotationName, false);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation),
   * also taking attribute overrides on composed annotations into account.
   *
   * @param annotationType the class of the annotation type to look for
   * @param classValuesAsString whether to convert class references to String
   * class names for exposure as values in the returned Map, instead of Class
   * references which might potentially have to be loaded first
   * @return a Map of attributes, with the attribute name as key (e.g. "value")
   * and the defined attribute value as Map value. This return value will be
   * {@code null} if no matching annotation is defined.
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(
          Class<? extends Annotation> annotationType, boolean classValuesAsString) {
    return getAnnotationAttributes(annotationType.getName(), classValuesAsString);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation),
   * also taking attribute overrides on composed annotations into account.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @param classValuesAsString whether to convert class references to String
   * class names for exposure as values in the returned Map, instead of Class
   * references which might potentially have to be loaded first
   * @return a Map of attributes, with the attribute name as key (e.g. "value")
   * and the defined attribute value as Map value. This return value will be
   * {@code null} if no matching annotation is defined.
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    MergedAnnotation<Annotation> annotation = getAnnotations().get(
            annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
    if (!annotation.isPresent()) {
      return null;
    }
    return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
  }

  /**
   * Retrieve all attributes of all annotations of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * Note that this variant does <i>not</i> take attribute overrides into account.
   *
   * @param annotationType the class of the annotation type to look for
   * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
   * and a list of the defined attribute values as Map value. This return value will
   * be {@code null} if no matching annotation is defined.
   * @see #getAllAnnotationAttributes(String, boolean)
   */
  @Nullable
  default MultiValueMap<String, Object> getAllAnnotationAttributes(Class<? extends Annotation> annotationType) {
    return getAllAnnotationAttributes(annotationType.getName());
  }

  /**
   * Retrieve all attributes of all annotations of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * Note that this variant does <i>not</i> take attribute overrides into account.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
   * and a list of the defined attribute values as Map value. This return value will
   * be {@code null} if no matching annotation is defined.
   * @see #getAllAnnotationAttributes(String, boolean)
   */
  @Nullable
  default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
    return getAllAnnotationAttributes(annotationName, false);
  }

  /**
   * Retrieve all attributes of all annotations of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * Note that this variant does <i>not</i> take attribute overrides into account.
   *
   * @param annotationType the class of the annotation type to look for
   * @param classValuesAsString whether to convert class references to String
   * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
   * and a list of the defined attribute values as Map value. This return value will
   * be {@code null} if no matching annotation is defined.
   * @see #getAllAnnotationAttributes(String)
   */
  @Nullable
  default MultiValueMap<String, Object> getAllAnnotationAttributes(
          Class<? extends Annotation> annotationType, boolean classValuesAsString) {
    return getAllAnnotationAttributes(annotationType.getName(), classValuesAsString);
  }

  /**
   * Retrieve all attributes of all annotations of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * Note that this variant does <i>not</i> take attribute overrides into account.
   *
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @param classValuesAsString whether to convert class references to String
   * @return a MultiMap of attributes, with the attribute name as key (e.g. "value")
   * and a list of the defined attribute values as Map value. This return value will
   * be {@code null} if no matching annotation is defined.
   * @see #getAllAnnotationAttributes(String)
   */
  @Nullable
  default MultiValueMap<String, Object> getAllAnnotationAttributes(
          String annotationName, boolean classValuesAsString) {

    Adapt[] adaptations = Adapt.values(classValuesAsString, true);
    return getAnnotations().stream(annotationName)
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .map(MergedAnnotation::withNonMergedAttributes)
            .collect(MergedAnnotationCollectors.toMultiValueMap(map -> map.isEmpty() ? null : map, adaptations));
  }

}
