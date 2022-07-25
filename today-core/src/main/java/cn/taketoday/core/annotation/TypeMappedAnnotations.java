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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cn.taketoday.lang.Nullable;

/**
 * {@link MergedAnnotations} implementation that searches for and adapts
 * annotations and meta-annotations using {@link AnnotationTypeMappings}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
final class TypeMappedAnnotations implements MergedAnnotations {

  /**
   * Shared instance that can be used when there are no annotations.
   */
  static final MergedAnnotations NONE = new TypeMappedAnnotations(
          null, new Annotation[0], RepeatableContainers.none(), AnnotationFilter.ALL);

  @Nullable
  private final Object source;

  @Nullable
  private final AnnotatedElement element;

  @Nullable
  private final SearchStrategy searchStrategy;

  @Nullable
  private final Annotation[] annotations;

  private final RepeatableContainers repeatableContainers;

  private final AnnotationFilter annotationFilter;

  @Nullable
  private volatile List<Aggregate> aggregates;

  private TypeMappedAnnotations(
          @Nullable AnnotatedElement element, @Nullable SearchStrategy searchStrategy,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

    this.source = element;
    this.element = element;
    this.searchStrategy = searchStrategy;
    this.annotations = null;
    this.repeatableContainers = repeatableContainers;
    this.annotationFilter = annotationFilter;
  }

  private TypeMappedAnnotations(
          @Nullable Object source, @Nullable Annotation[] annotations,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
    this.source = source;
    this.element = null;
    this.searchStrategy = null;
    this.annotations = annotations;
    this.repeatableContainers = repeatableContainers;
    this.annotationFilter = annotationFilter;
  }

  @Override
  public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
    if (this.annotationFilter.matches(annotationType)) {
      return false;
    }
    return Boolean.TRUE.equals(scan(
            annotationType, IsPresent.get(repeatableContainers, annotationFilter, false)));
  }

  @Override
  public boolean isPresent(String annotationType) {
    if (annotationFilter.matches(annotationType)) {
      return false;
    }
    return Boolean.TRUE.equals(scan(
            annotationType, IsPresent.get(repeatableContainers, annotationFilter, false)));
  }

  @Override
  public <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType) {
    if (annotationFilter.matches(annotationType)) {
      return false;
    }
    return Boolean.TRUE.equals(
            scan(annotationType, IsPresent.get(repeatableContainers, annotationFilter, true)));
  }

  @Override
  public boolean isDirectlyPresent(String annotationType) {
    if (annotationFilter.matches(annotationType)) {
      return false;
    }
    return Boolean.TRUE.equals(scan(
            annotationType, IsPresent.get(repeatableContainers, annotationFilter, true)));
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
    return get(annotationType, null, null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(
          Class<A> annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate) {

    return get(annotationType, predicate, null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(
          Class<A> annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate,
          @Nullable MergedAnnotationSelector<A> selector) {

    if (annotationFilter.matches(annotationType)) {
      return MergedAnnotation.missing();
    }
    MergedAnnotation<A> result = scan(
            annotationType, new MergedAnnotationFinder<>(annotationType, predicate, selector));
    return (result != null ? result : MergedAnnotation.missing());
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
    return get(annotationType, null, null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(
          String annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate) {
    return get(annotationType, predicate, null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> get(
          String annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate,
          @Nullable MergedAnnotationSelector<A> selector) {

    if (annotationFilter.matches(annotationType)) {
      return MergedAnnotation.missing();
    }
    MergedAnnotation<A> result = scan(annotationType,
            new MergedAnnotationFinder<>(annotationType, predicate, selector));
    return (result != null ? result : MergedAnnotation.missing());
  }

  @Override
  public <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType) {
    if (annotationFilter == AnnotationFilter.ALL) {
      return Stream.empty();
    }
    return StreamSupport.stream(spliterator(annotationType), false);
  }

  @Override
  public <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType) {
    if (annotationFilter == AnnotationFilter.ALL) {
      return Stream.empty();
    }
    return StreamSupport.stream(spliterator(annotationType), false);
  }

  @Override
  public Stream<MergedAnnotation<Annotation>> stream() {
    if (annotationFilter == AnnotationFilter.ALL) {
      return Stream.empty();
    }
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  public <A extends Annotation> AnnotationAttributes[] getAttributes(Class<A> annotationType) {
    if (annotationFilter == AnnotationFilter.ALL) {
      return AnnotationAttributes.EMPTY_ARRAY;
    }
    return StreamSupport.stream(spliterator(annotationType), false)
            .map(MergedAnnotation::asAnnotationAttributes)
            .toArray(AnnotationAttributes[]::new);
  }

  @Override
  public Iterator<MergedAnnotation<Annotation>> iterator() {
    if (annotationFilter == AnnotationFilter.ALL) {
      return Collections.emptyIterator();
    }
    return Spliterators.iterator(spliterator());
  }

  @Override
  public Spliterator<MergedAnnotation<Annotation>> spliterator() {
    if (annotationFilter == AnnotationFilter.ALL) {
      return Spliterators.emptySpliterator();
    }
    return spliterator(null);
  }

  private <A extends Annotation> Spliterator<MergedAnnotation<A>> spliterator(@Nullable Object annotationType) {
    return new AggregatesSpliterator<>(annotationType, getAggregates());
  }

  private List<Aggregate> getAggregates() {
    List<Aggregate> aggregates = this.aggregates;
    if (aggregates == null) {
      aggregates = scan(this, new AggregatesCollector());
      if (aggregates == null || aggregates.isEmpty()) {
        aggregates = Collections.emptyList();
      }
      this.aggregates = aggregates;
    }
    return aggregates;
  }

  @Nullable
  private <C, R> R scan(C criteria, AnnotationsProcessor<C, R> processor) {
    if (annotations != null) {
      R result = processor.doWithAnnotations(criteria, 0, source, annotations);
      return processor.finish(result);
    }
    if (element != null && searchStrategy != null) {
      return AnnotationsScanner.scan(criteria, element, this.searchStrategy, processor);
    }
    return null;
  }

  static MergedAnnotations from(
          AnnotatedElement element, SearchStrategy searchStrategy,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

    if (AnnotationsScanner.isKnownEmpty(element, searchStrategy)) {
      return NONE;
    }
    return new TypeMappedAnnotations(element, searchStrategy, repeatableContainers, annotationFilter);
  }

  static MergedAnnotations from(
          @Nullable Object source, Annotation[] annotations,
          RepeatableContainers repeatableContainers, AnnotationFilter filter) {

    if (annotations.length == 0) {
      return NONE;
    }
    return new TypeMappedAnnotations(source, annotations, repeatableContainers, filter);
  }

  private static boolean isMappingForType(
          AnnotationTypeMapping mapping, AnnotationFilter filter, @Nullable Object requiredType) {

    Class<? extends Annotation> actualType = mapping.getAnnotationType();
    return (!filter.matches(actualType) &&
            (requiredType == null || actualType == requiredType || actualType.getName().equals(requiredType)));
  }

  /**
   * {@link AnnotationsProcessor} used to detect if an annotation is directly
   * present or meta-present.
   */
  private static final class IsPresent implements AnnotationsProcessor<Object, Boolean> {

    /**
     * Shared instances that save us needing to create a new processor for
     * the common combinations.
     */
    private static final IsPresent[] SHARED;

    static {
      SHARED = new IsPresent[4];
      SHARED[0] = new IsPresent(RepeatableContainers.none(), AnnotationFilter.PLAIN, true);
      SHARED[1] = new IsPresent(RepeatableContainers.none(), AnnotationFilter.PLAIN, false);
      SHARED[2] = new IsPresent(RepeatableContainers.standard(), AnnotationFilter.PLAIN, true);
      SHARED[3] = new IsPresent(RepeatableContainers.standard(), AnnotationFilter.PLAIN, false);
    }

    private final boolean directOnly;
    private final AnnotationFilter annotationFilter;
    private final RepeatableContainers repeatableContainers;

    private IsPresent(
            RepeatableContainers repeatableContainers, AnnotationFilter filter, boolean directOnly) {

      this.directOnly = directOnly;
      this.annotationFilter = filter;
      this.repeatableContainers = repeatableContainers;
    }

    @Override
    @Nullable
    public Boolean doWithAnnotations(
            Object requiredType, int aggregateIndex,
            @Nullable Object source, Annotation[] annotations) {

      for (Annotation annotation : annotations) {
        if (annotation != null) {
          Class<? extends Annotation> type = annotation.annotationType();
          if (type != null && !annotationFilter.matches(type)) {
            if (type == requiredType || type.getName().equals(requiredType)) {
              return Boolean.TRUE;
            }
            Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(annotation);
            if (repeatedAnnotations != null) {
              Boolean result = doWithAnnotations(
                      requiredType, aggregateIndex, source, repeatedAnnotations);
              if (result != null) {
                return result;
              }
            }
            if (!directOnly) {
              AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(type);
              for (int i = 0; i < mappings.size(); i++) {
                AnnotationTypeMapping mapping = mappings.get(i);
                if (isMappingForType(mapping, annotationFilter, requiredType)) {
                  return Boolean.TRUE;
                }
              }
            }
          }
        }
      }
      return null;
    }

    static IsPresent get(
            RepeatableContainers repeatableContainers,
            AnnotationFilter annotationFilter, boolean directOnly) {
      // Use a single shared instance for common combinations
      if (annotationFilter == AnnotationFilter.PLAIN) {
        if (repeatableContainers == RepeatableContainers.none()) {
          return SHARED[directOnly ? 0 : 1];
        }
        if (repeatableContainers == RepeatableContainers.standard()) {
          return SHARED[directOnly ? 2 : 3];
        }
      }
      return new IsPresent(repeatableContainers, annotationFilter, directOnly);
    }
  }

  /**
   * {@link AnnotationsProcessor} that finds a single {@link MergedAnnotation}.
   */
  private class MergedAnnotationFinder<A extends Annotation>
          implements AnnotationsProcessor<Object, MergedAnnotation<A>> {

    private final Object requiredType;
    private final MergedAnnotationSelector<A> selector;
    @Nullable
    private final Predicate<? super MergedAnnotation<A>> predicate;

    @Nullable
    private MergedAnnotation<A> result;

    MergedAnnotationFinder(
            Object requiredType,
            @Nullable Predicate<? super MergedAnnotation<A>> predicate,
            @Nullable MergedAnnotationSelector<A> selector) {
      this.predicate = predicate;
      this.requiredType = requiredType;
      this.selector = selector != null ? selector : MergedAnnotationSelectors.nearest();
    }

    @Override
    @Nullable
    public MergedAnnotation<A> doWithAggregate(Object context, int aggregateIndex) {
      return result;
    }

    @Override
    @Nullable
    public MergedAnnotation<A> doWithAnnotations(
            Object type, int aggregateIndex, @Nullable Object source, Annotation[] annotations) {

      for (Annotation annotation : annotations) {
        if (annotation != null && !annotationFilter.matches(annotation)) {
          MergedAnnotation<A> result = process(type, aggregateIndex, source, annotation);
          if (result != null) {
            return result;
          }
        }
      }
      return null;
    }

    @Nullable
    private MergedAnnotation<A> process(
            Object type, int aggregateIndex, @Nullable Object source, Annotation annotation) {

      Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(annotation);
      if (repeatedAnnotations != null) {
        return doWithAnnotations(type, aggregateIndex, source, repeatedAnnotations);
      }
      AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
              annotation.annotationType(), repeatableContainers, annotationFilter);
      for (int i = 0; i < mappings.size(); i++) {
        AnnotationTypeMapping mapping = mappings.get(i);
        if (isMappingForType(mapping, annotationFilter, requiredType)) {
          MergedAnnotation<A> candidate = TypeMappedAnnotation.createIfPossible(
                  mapping, source, annotation, aggregateIndex, IntrospectionFailureLogger.INFO);
          if (candidate != null && (predicate == null || predicate.test(candidate))) {
            if (selector.isBestCandidate(candidate)) {
              return candidate;
            }
            updateLastResult(candidate);
          }
        }
      }
      return null;
    }

    private void updateLastResult(MergedAnnotation<A> candidate) {
      MergedAnnotation<A> lastResult = result;
      this.result = (lastResult != null ? selector.select(lastResult, candidate) : candidate);
    }

    @Override
    @Nullable
    public MergedAnnotation<A> finish(@Nullable MergedAnnotation<A> result) {
      return (result != null ? result : this.result);
    }
  }

  /**
   * {@link AnnotationsProcessor} that collects {@link Aggregate} instances.
   */
  private class AggregatesCollector implements AnnotationsProcessor<Object, List<Aggregate>> {

    private final ArrayList<Aggregate> aggregates = new ArrayList<>();

    @Override
    @Nullable
    public List<Aggregate> doWithAnnotations(
            Object criteria, int aggregateIndex, @Nullable Object source, Annotation[] annotations) {
      aggregates.add(createAggregate(aggregateIndex, source, annotations));
      return null;
    }

    private Aggregate createAggregate(int aggregateIndex, @Nullable Object source, Annotation[] annotations) {
      ArrayList<Annotation> aggregateAnnotations = getAggregateAnnotations(annotations);
      return new Aggregate(aggregateIndex, source, aggregateAnnotations);
    }

    private ArrayList<Annotation> getAggregateAnnotations(Annotation[] annotations) {
      ArrayList<Annotation> result = new ArrayList<>(annotations.length);
      addAggregateAnnotations(result, annotations);
      return result;
    }

    private void addAggregateAnnotations(List<Annotation> aggregateAnnotations, Annotation[] annotations) {
      for (Annotation annotation : annotations) {
        if (annotation != null && !annotationFilter.matches(annotation)) {
          Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(annotation);
          if (repeatedAnnotations != null) {
            addAggregateAnnotations(aggregateAnnotations, repeatedAnnotations);
          }
          else {
            aggregateAnnotations.add(annotation);
          }
        }
      }
    }

    @Override
    public List<Aggregate> finish(@Nullable List<Aggregate> processResult) {
      return aggregates;
    }
  }

  private static class Aggregate {

    private final int aggregateIndex;

    @Nullable
    private final Object source;

    private final ArrayList<Annotation> annotations;

    private final AnnotationTypeMappings[] mappings;

    Aggregate(int aggregateIndex, @Nullable Object source, ArrayList<Annotation> annotations) {
      this.aggregateIndex = aggregateIndex;
      this.source = source;
      this.annotations = annotations;
      int size = annotations.size();
      this.mappings = new AnnotationTypeMappings[size];
      for (int i = 0; i < size; i++) {
        this.mappings[i] = AnnotationTypeMappings.forAnnotationType(annotations.get(i).annotationType());
      }
    }

    int size() {
      return annotations.size();
    }

    @Nullable
    AnnotationTypeMapping getMapping(int annotationIndex, int mappingIndex) {
      AnnotationTypeMappings mappings = getMappings(annotationIndex);
      return (mappingIndex < mappings.size() ? mappings.get(mappingIndex) : null);
    }

    AnnotationTypeMappings getMappings(int annotationIndex) {
      return mappings[annotationIndex];
    }

    @Nullable
    <A extends Annotation> MergedAnnotation<A> createMergedAnnotationIfPossible(
            int annotationIndex, int mappingIndex, IntrospectionFailureLogger logger) {

      return TypeMappedAnnotation.createIfPossible(
              mappings[annotationIndex].get(mappingIndex), source,
              annotations.get(annotationIndex), aggregateIndex, logger);
    }
  }

  /**
   * {@link Spliterator} used to consume merged annotations from the
   * aggregates in distance fist order.
   */
  private class AggregatesSpliterator<A extends Annotation> implements Spliterator<MergedAnnotation<A>> {

    @Nullable
    private final Object requiredType;

    private final List<Aggregate> aggregates;

    private int aggregateCursor;

    @Nullable
    private int[] mappingCursors;

    AggregatesSpliterator(@Nullable Object requiredType, List<Aggregate> aggregates) {
      this.requiredType = requiredType;
      this.aggregates = aggregates;
      this.aggregateCursor = 0;
    }

    @Override
    public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
      while (aggregateCursor < aggregates.size()) {
        Aggregate aggregate = aggregates.get(aggregateCursor);
        if (tryAdvance(aggregate, action)) {
          return true;
        }
        aggregateCursor++;
        mappingCursors = null;
      }
      return false;
    }

    private boolean tryAdvance(Aggregate aggregate, Consumer<? super MergedAnnotation<A>> action) {
      if (mappingCursors == null) {
        this.mappingCursors = new int[aggregate.size()];
      }
      int lowestDistance = Integer.MAX_VALUE;
      int annotationResult = -1;
      for (int annotationIndex = 0; annotationIndex < aggregate.size(); annotationIndex++) {
        AnnotationTypeMapping mapping = getNextSuitableMapping(aggregate, annotationIndex);
        if (mapping != null && mapping.getDistance() < lowestDistance) {
          annotationResult = annotationIndex;
          lowestDistance = mapping.getDistance();
        }
        if (lowestDistance == 0) {
          break;
        }
      }
      if (annotationResult != -1) {
        MergedAnnotation<A> mergedAnnotation = aggregate.createMergedAnnotationIfPossible(
                annotationResult, mappingCursors[annotationResult],
                requiredType != null ? IntrospectionFailureLogger.INFO : IntrospectionFailureLogger.DEBUG);
        mappingCursors[annotationResult]++;
        if (mergedAnnotation == null) {
          return tryAdvance(aggregate, action);
        }
        action.accept(mergedAnnotation);
        return true;
      }
      return false;
    }

    @Nullable
    private AnnotationTypeMapping getNextSuitableMapping(Aggregate aggregate, int annotationIndex) {
      int[] cursors = this.mappingCursors;
      if (cursors != null) {
        AnnotationTypeMapping mapping;
        do {
          mapping = aggregate.getMapping(annotationIndex, cursors[annotationIndex]);
          if (mapping != null && isMappingForType(mapping, annotationFilter, requiredType)) {
            return mapping;
          }
          cursors[annotationIndex]++;
        }
        while (mapping != null);
      }
      return null;
    }

    @Override
    @Nullable
    public Spliterator<MergedAnnotation<A>> trySplit() {
      return null;
    }

    @Override
    public long estimateSize() {
      int size = 0;
      for (int aggregateIndex = this.aggregateCursor;
           aggregateIndex < aggregates.size(); aggregateIndex++) {
        Aggregate aggregate = aggregates.get(aggregateIndex);
        for (int annotationIndex = 0; annotationIndex < aggregate.size(); annotationIndex++) {
          AnnotationTypeMappings mappings = aggregate.getMappings(annotationIndex);
          int numberOfMappings = mappings.size();
          if (aggregateIndex == aggregateCursor && mappingCursors != null) {
            numberOfMappings -= Math.min(mappingCursors[annotationIndex], mappings.size());
          }
          size += numberOfMappings;
        }
      }
      return size;
    }

    @Override
    public int characteristics() {
      return NONNULL | IMMUTABLE;
    }
  }

}
