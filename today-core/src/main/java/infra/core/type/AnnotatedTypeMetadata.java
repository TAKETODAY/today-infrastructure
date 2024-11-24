/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.type;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.core.annotation.AnnotationAttributes;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotation.Adapt;
import infra.core.annotation.MergedAnnotationCollectors;
import infra.core.annotation.AliasFor;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotationSelectors;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Nullable;
import infra.util.MultiValueMap;

/**
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require
 * class loading of the types being inspected. Note, however, that classes for
 * encountered annotations will be loaded.
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
   * Get annotation details based on the direct annotations and meta-annotations
   * of the underlying element.
   *
   * @return merged annotations based on the direct annotations and meta-annotations
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
   * @param annotationName the fully qualified class name of the annotation
   * type to look for meta-annotations on
   * @return the meta-annotation type names, or an empty set if none found
   */
  default Set<String> getMetaAnnotationTypes(String annotationName) {
    var annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
    if (annotation.isPresent()) {
      return MergedAnnotations.from(annotation.getType())
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
   * defined on the underlying element, as direct annotation or meta-annotation).
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationType the fully-qualified class of the annotation
   * type to look for
   * @return a {@link Map} of attributes, with each annotation attribute name
   * as map key (e.g. "location") and the attribute's value as map value; or
   * {@code null} if no matching annotation is found
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(Class<? extends Annotation> annotationType) {
    return getAnnotationAttributes(annotationType.getName(), false);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationName the fully-qualified class name of the annotation
   * type to look for
   * @return a {@link Map} of attributes, with each annotation attribute name
   * as map key (e.g. "location") and the attribute's value as map value; or
   * {@code null} if no matching annotation is found
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(String annotationName) {
    return getAnnotationAttributes(annotationName, false);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationType the fully-qualified class of the annotation
   * type to look for
   * @param classValuesAsString whether to convert class references to String
   * class names for exposure as values in the returned Map, instead of Class
   * references which might potentially have to be loaded first
   * @return a {@link Map} of attributes, with each annotation attribute name
   * as map key (e.g. "location") and the attribute's value as map value; or
   * {@code null} if no matching annotation is found
   */
  @Nullable
  default Map<String, Object> getAnnotationAttributes(Class<? extends Annotation> annotationType, boolean classValuesAsString) {
    return getAnnotationAttributes(annotationType.getName(), classValuesAsString);
  }

  /**
   * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   * defined on the underlying element, as direct annotation or meta-annotation).
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationName the fully-qualified class name of the annotation
   * type to look for
   * @param classValuesAsString whether to convert class references to String
   * class names for exposure as values in the returned Map, instead of Class
   * references which might potentially have to be loaded first
   * @return a {@link Map} of attributes, with each annotation attribute name
   * as map key (e.g. "location") and the attribute's value as map value; or
   * {@code null} if no matching annotation is found
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
  default MultiValueMap<String, Object> getAllAnnotationAttributes(Class<? extends Annotation> annotationType, boolean classValuesAsString) {
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
  default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    Adapt[] adaptations = Adapt.values(classValuesAsString, true);
    return getAnnotations().stream(annotationName)
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .map(MergedAnnotation::withNonMergedAttributes)
            .collect(MergedAnnotationCollectors.toMultiValueMap(map -> map.isEmpty() ? null : map, adaptations));
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link AnnotationAttributes}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @param classValuesAsString whether to convert class references to {@code String}
   * class names for exposure as values in the returned {@code AnnotationAttributes},
   * instead of {@code Class} references which might potentially have to be loaded
   * first
   * @return the set of all merged repeatable {@code AnnotationAttributes} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
   */
  default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(Class<? extends Annotation> annotationType,
          Class<? extends Annotation> containerType, boolean classValuesAsString) {

    return getMergedRepeatableAnnotationAttributes(annotationType, containerType, classValuesAsString, false);
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link AnnotationAttributes}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
   * the results will be sorted in {@link Comparator#reversed() reversed} order
   * based on each annotation's {@linkplain MergedAnnotation#getDistance()
   * meta distance}, which effectively orders meta-annotations before annotations
   * that are declared directly on the underlying element.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @param classValuesAsString whether to convert class references to {@code String}
   * class names for exposure as values in the returned {@code AnnotationAttributes},
   * instead of {@code Class} references which might potentially have to be loaded
   * first
   * @param sortByReversedMetaDistance {@code true} if the results should be
   * sorted in reversed order based on each annotation's meta distance
   * @return the set of all merged repeatable {@code AnnotationAttributes} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
   */
  default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(Class<? extends Annotation> annotationType,
          Class<? extends Annotation> containerType, boolean classValuesAsString, boolean sortByReversedMetaDistance) {

    return getMergedRepeatableAnnotationAttributes(annotationType, containerType, classValuesAsString, sortByReversedMetaDistance, null);
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link AnnotationAttributes}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   * <p>The supplied {@link Predicate} will be used to filter the results. For
   * example, supply {@code mergedAnnotation -> true} to include all annotations
   * in the results; supply {@code MergedAnnotation::isDirectlyPresent} to limit
   * the results to directly declared annotations, etc.
   * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
   * the results will be sorted in {@link Comparator#reversed() reversed} order
   * based on each annotation's {@linkplain MergedAnnotation#getDistance()
   * meta distance}, which effectively orders meta-annotations before annotations
   * that are declared directly on the underlying element.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @param predicate a {@code Predicate} to apply to each {@code MergedAnnotation}
   * to determine if it should be included in the results
   * @param classValuesAsString whether to convert class references to {@code String}
   * class names for exposure as values in the returned {@code AnnotationAttributes},
   * instead of {@code Class} references which might potentially have to be loaded
   * first
   * @param sortByReversedMetaDistance {@code true} if the results should be
   * sorted in reversed order based on each annotation's meta distance
   * @return the set of all merged repeatable {@code AnnotationAttributes} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
   * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
   */
  default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(Class<? extends Annotation> annotationType,
          Class<? extends Annotation> containerType, boolean classValuesAsString, boolean sortByReversedMetaDistance,
          @Nullable Predicate<MergedAnnotation<? extends Annotation>> predicate) {

    Adapt[] adaptations = Adapt.values(classValuesAsString, true);
    return stream(annotationType, containerType, sortByReversedMetaDistance, predicate)
            .map(annotation -> annotation.asAnnotationAttributes(adaptations))
            .flatMap(attributes -> {
              if (containerType.equals(attributes.annotationType())) {
                return Stream.of(attributes.getAnnotationArray(MergedAnnotation.VALUE));
              }
              return Stream.of(attributes);
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link MergedAnnotation}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @return the set of all merged repeatable {@code MergedAnnotation} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotation(Class, Class, boolean)
   */
  default <A extends Annotation> Set<MergedAnnotation<A>> getMergedRepeatableAnnotation(
          Class<A> annotationType, Class<? extends Annotation> containerType) {

    return getMergedRepeatableAnnotation(annotationType, containerType, false);
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link MergedAnnotation}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
   * the results will be sorted in {@link Comparator#reversed() reversed} order
   * based on each annotation's {@linkplain MergedAnnotation#getDistance()
   * meta distance}, which effectively orders meta-annotations before annotations
   * that are declared directly on the underlying element.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @param sortByReversedMetaDistance {@code true} if the results should be
   * sorted in reversed order based on each annotation's meta distance
   * @return the set of all merged repeatable {@code MergedAnnotation} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotation(Class, Class, boolean)
   */
  default <A extends Annotation> Set<MergedAnnotation<A>> getMergedRepeatableAnnotation(
          Class<A> annotationType, Class<? extends Annotation> containerType, boolean sortByReversedMetaDistance) {

    return getMergedRepeatableAnnotation(annotationType, containerType, sortByReversedMetaDistance, null);
  }

  /**
   * Retrieve all <em>repeatable annotations</em> of the given type within the
   * annotation hierarchy <em>above</em> the underlying element (as direct
   * annotation or meta-annotation); and for each annotation found, merge that
   * annotation's attributes with <em>matching</em> attributes from annotations
   * in lower levels of the annotation hierarchy and store the results in an
   * instance of {@link MergedAnnotation}.
   * <p>{@link AliasFor @AliasFor} semantics
   * are fully supported, both within a single annotation and within annotation
   * hierarchies.
   * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
   * the results will be sorted in {@link Comparator#reversed() reversed} order
   * based on each annotation's {@linkplain MergedAnnotation#getDistance()
   * meta distance}, which effectively orders meta-annotations before annotations
   * that are declared directly on the underlying element.
   *
   * @param annotationType the annotation type to find
   * @param containerType the type of the container that holds the annotations
   * @param sortByReversedMetaDistance {@code true} if the results should be
   * @param predicate a {@code Predicate} to apply to each {@code MergedAnnotation}
   * to determine if it should be included in the results
   * sorted in reversed order based on each annotation's meta distance
   * @return the set of all merged repeatable {@code MergedAnnotation} found,
   * or an empty set if none were found
   * @see #getMergedRepeatableAnnotation(Class, Class, boolean)
   */
  @SuppressWarnings("unchecked")
  default <A extends Annotation> Set<MergedAnnotation<A>> getMergedRepeatableAnnotation(
          Class<A> annotationType, Class<? extends Annotation> containerType, boolean sortByReversedMetaDistance,
          @Nullable Predicate<MergedAnnotation<? extends Annotation>> predicate) {

    return stream(annotationType, containerType, sortByReversedMetaDistance, predicate)
            .flatMap(annotation -> {
              if (containerType.equals(annotation.getType())) {
                return Stream.of(annotation.getAnnotationArray(MergedAnnotation.VALUE, annotationType));
              }
              return Stream.of((MergedAnnotation<A>) annotation);
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private <A extends Annotation> Stream<MergedAnnotation<Annotation>> stream(Class<A> annotationType,
          Class<? extends Annotation> containerType, boolean sortByReversedMetaDistance,
          @Nullable Predicate<MergedAnnotation<? extends Annotation>> predicate) {
    var stream = getAnnotations().stream();
    if (predicate != null) {
      stream = stream.filter(predicate);
    }
    stream = stream.filter(MergedAnnotationPredicates.typeIn(containerType, annotationType));
    if (sortByReversedMetaDistance) {
      return stream.sorted(reversedMetaDistance());
    }
    return stream;
  }

  private static Comparator<MergedAnnotation<Annotation>> reversedMetaDistance() {
    return Comparator.<MergedAnnotation<Annotation>>comparingInt(MergedAnnotation::getDistance).reversed();
  }
}
