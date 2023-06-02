/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * Provides {@link AnnotationTypeMapping} information for a single source
 * annotation type. Performs a recursive breadth first crawl of all
 * meta-annotations to ultimately provide a quick way to map the attributes of
 * a root {@link Annotation}.
 *
 * <p>Supports convention based merging of meta-annotations as well as implicit
 * and explicit {@link AliasFor @AliasFor} aliases. Also provides information
 * about mirrored attributes.
 *
 * <p>This class is designed to be cached so that meta-annotations only need to
 * be searched once, regardless of how many times they are actually used.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationTypeMapping
 * @since 4.0
 */
final class AnnotationTypeMappings implements Iterable<AnnotationTypeMapping> {
  private static final IntrospectionFailureLogger failureLogger = IntrospectionFailureLogger.DEBUG;

  private static final ConcurrentReferenceHashMap<AnnotationFilter, Cache>
          standardRepeatablesCache = new ConcurrentReferenceHashMap<>();

  private static final ConcurrentReferenceHashMap<AnnotationFilter, Cache>
          noRepeatablesCache = new ConcurrentReferenceHashMap<>();

  private final AnnotationFilter filter;
  private final ArrayList<AnnotationTypeMapping> mappings;
  private final RepeatableContainers repeatableContainers;

  private AnnotationTypeMappings(RepeatableContainers repeatableContainers, AnnotationFilter filter,
          Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
    this.filter = filter;
    this.mappings = new ArrayList<>();
    this.repeatableContainers = repeatableContainers;
    addAllMappings(annotationType, visitedAnnotationTypes);
    for (AnnotationTypeMapping mapping : mappings) {
      mapping.afterAllMappingsSet();
    }
  }

  private void addAllMappings(Class<? extends Annotation> annotationType,
          Set<Class<? extends Annotation>> visitedAnnotationTypes) {

    ArrayDeque<AnnotationTypeMapping> queue = new ArrayDeque<>();
    addIfPossible(queue, null, annotationType, null, visitedAnnotationTypes);
    while (!queue.isEmpty()) {
      AnnotationTypeMapping mapping = queue.removeFirst();
      this.mappings.add(mapping);
      addMetaAnnotationsToQueue(queue, mapping);
    }
  }

  private void addMetaAnnotationsToQueue(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source) {
    Annotation[] metaAnnotations = AnnotationsScanner.getDeclaredAnnotations(source.annotationType, false);
    for (Annotation metaAnnotation : metaAnnotations) {
      if (isNotMappable(source, metaAnnotation)) {
        continue;
      }
      Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(metaAnnotation);
      if (repeatedAnnotations != null) {
        for (Annotation repeatedAnnotation : repeatedAnnotations) {
          if (isNotMappable(source, repeatedAnnotation)) {
            continue;
          }
          addIfPossible(queue, source, repeatedAnnotation);
        }
      }
      else {
        addIfPossible(queue, source, metaAnnotation);
      }
    }
  }

  private void addIfPossible(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source, Annotation ann) {
    addIfPossible(queue, source, ann.annotationType(), ann, new HashSet<>());
  }

  private void addIfPossible(Deque<AnnotationTypeMapping> queue, @Nullable AnnotationTypeMapping source,
          Class<? extends Annotation> annotationType, @Nullable Annotation ann,
          Set<Class<? extends Annotation>> visitedAnnotationTypes) {
    try {
      queue.addLast(new AnnotationTypeMapping(source, annotationType, ann, visitedAnnotationTypes));
    }
    catch (Exception ex) {
      AnnotationUtils.rethrowAnnotationConfigurationException(ex);
      if (failureLogger.isEnabled()) {
        failureLogger.log("Failed to introspect meta-annotation " + annotationType.getName(),
                (source != null ? source.annotationType : null), ex);
      }
    }
  }

  private boolean isNotMappable(AnnotationTypeMapping source, @Nullable Annotation metaAnnotation) {
    return metaAnnotation == null
            || filter.matches(metaAnnotation)
            || AnnotationFilter.PLAIN.matches(source.annotationType)
            || isAlreadyMapped(source, metaAnnotation);
  }

  private boolean isAlreadyMapped(AnnotationTypeMapping source, Annotation metaAnnotation) {
    Class<? extends Annotation> annotationType = metaAnnotation.annotationType();
    AnnotationTypeMapping mapping = source;
    while (mapping != null) {
      if (mapping.annotationType == annotationType) {
        return true;
      }
      mapping = mapping.source;
    }
    return false;
  }

  /**
   * Get the total number of contained mappings.
   *
   * @return the total number of mappings
   */
  int size() {
    return this.mappings.size();
  }

  /**
   * Get an individual mapping from this instance.
   * <p>Index {@code 0} will always return the root mapping; higher indexes
   * will return meta-annotation mappings.
   *
   * @param index the index to return
   * @return the {@link AnnotationTypeMapping}
   * @throws IndexOutOfBoundsException if the index is out of range
   * (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  AnnotationTypeMapping get(int index) {
    return this.mappings.get(index);
  }

  @Override
  public Iterator<AnnotationTypeMapping> iterator() {
    return mappings.iterator();
  }

  @Override
  public void forEach(Consumer<? super AnnotationTypeMapping> action) {
    mappings.forEach(action);
  }

  @Override
  public Spliterator<AnnotationTypeMapping> spliterator() {
    return mappings.spliterator();
  }

  /**
   * Create {@link AnnotationTypeMappings} for the specified annotation type.
   *
   * @param annotationType the source annotation type
   * @return type mappings for the annotation type
   */
  static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType) {
    return forAnnotationType(annotationType, new HashSet<>());
  }

  /**
   * Create {@link AnnotationTypeMappings} for the specified annotation type.
   *
   * @param annotationType the source annotation type
   * @param visitedAnnotationTypes the set of annotations that we have already
   * visited; used to avoid infinite recursion for recursive annotations which
   * some JVM languages support (such as Kotlin)
   * @return type mappings for the annotation type
   */
  static AnnotationTypeMappings forAnnotationType(
          Class<? extends Annotation> annotationType,
          Set<Class<? extends Annotation>> visitedAnnotationTypes) {

    return forAnnotationType(
            annotationType, RepeatableContainers.standard(),
            AnnotationFilter.PLAIN, visitedAnnotationTypes);
  }

  /**
   * Create {@link AnnotationTypeMappings} for the specified annotation type.
   *
   * @param annotationType the source annotation type
   * @param repeatableContainers the repeatable containers that may be used by
   * the meta-annotations
   * @param annotationFilter the annotation filter used to limit which
   * annotations are considered
   * @return type mappings for the annotation type
   */
  static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
    return forAnnotationType(annotationType, repeatableContainers, annotationFilter, new HashSet<>());
  }

  /**
   * Create {@link AnnotationTypeMappings} for the specified annotation type.
   *
   * @param annotationType the source annotation type
   * @param repeatableContainers the repeatable containers that may be used by
   * the meta-annotations
   * @param annotationFilter the annotation filter used to limit which
   * annotations are considered
   * @param visitedAnnotationTypes the set of annotations that we have already
   * visited; used to avoid infinite recursion for recursive annotations which
   * some JVM languages support (such as Kotlin)
   * @return type mappings for the annotation type
   */
  private static AnnotationTypeMappings forAnnotationType(
          Class<? extends Annotation> annotationType, RepeatableContainers repeatableContainers,
          AnnotationFilter annotationFilter, Set<Class<? extends Annotation>> visitedAnnotationTypes) {
    if (repeatableContainers == RepeatableContainers.standard()) {
      return standardRepeatablesCache.computeIfAbsent(
                      annotationFilter, key -> new Cache(repeatableContainers, key))
              .get(annotationType, visitedAnnotationTypes);
    }
    if (repeatableContainers == RepeatableContainers.NONE) {
      return noRepeatablesCache.computeIfAbsent(
                      annotationFilter, key -> new Cache(repeatableContainers, key))
              .get(annotationType, visitedAnnotationTypes);
    }
    return new AnnotationTypeMappings(repeatableContainers,
            annotationFilter, annotationType, visitedAnnotationTypes);
  }

  static void clearCache() {
    standardRepeatablesCache.clear();
    noRepeatablesCache.clear();
  }

  /**
   * Cache created per {@link AnnotationFilter}.
   */
  private static class Cache {
    private final AnnotationFilter filter;
    private final RepeatableContainers repeatableContainers;
    private final ConcurrentReferenceHashMap<Class<? extends Annotation>, AnnotationTypeMappings> mappings;

    /**
     * Create a cache instance with the specified filter.
     *
     * @param filter the annotation filter
     */
    Cache(RepeatableContainers repeatableContainers, AnnotationFilter filter) {
      this.filter = filter;
      this.repeatableContainers = repeatableContainers;
      this.mappings = new ConcurrentReferenceHashMap<>();
    }

    /**
     * Get or create {@link AnnotationTypeMappings} for the specified annotation type.
     *
     * @param annotationType the annotation type
     * @param visitedAnnotationTypes the set of annotations that we have already
     * visited; used to avoid infinite recursion for recursive annotations which
     * some JVM languages support (such as Kotlin)
     * @return a new or existing {@link AnnotationTypeMappings} instance
     */
    AnnotationTypeMappings get(Class<? extends Annotation> annotationType,
            Set<Class<? extends Annotation>> visitedAnnotationTypes) {
      return mappings.computeIfAbsent(annotationType, key -> new AnnotationTypeMappings(
              repeatableContainers, filter, annotationType, visitedAnnotationTypes));
    }

  }

}
