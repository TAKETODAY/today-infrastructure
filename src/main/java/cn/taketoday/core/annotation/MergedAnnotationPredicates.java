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
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Predicate implementations that provide various test operations for
 * {@link MergedAnnotation MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class MergedAnnotationPredicates {

  /**
   * Create a new {@link Predicate} that evaluates to {@code true} if the name of the
   * {@linkplain MergedAnnotation#getType() merged annotation type} is contained in
   * the specified array.
   *
   * @param <A> the annotation type
   * @param typeNames the names that should be matched
   * @return a {@link Predicate} to test the annotation type
   */
  public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(String... typeNames) {
    return annotation -> ObjectUtils.containsElement(typeNames, annotation.getType().getName());
  }

  /**
   * Create a new {@link Predicate} that evaluates to {@code true} if the
   * {@linkplain MergedAnnotation#getType() merged annotation type} is contained in
   * the specified array.
   *
   * @param <A> the annotation type
   * @param types the types that should be matched
   * @return a {@link Predicate} to test the annotation type
   */
  public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Class<?>... types) {
    return annotation -> ObjectUtils.containsElement(types, annotation.getType());
  }

  /**
   * Create a new {@link Predicate} that evaluates to {@code true} if the
   * {@linkplain MergedAnnotation#getType() merged annotation type} is contained in
   * the specified collection.
   *
   * @param <A> the annotation type
   * @param types the type names or classes that should be matched
   * @return a {@link Predicate} to test the annotation type
   */
  public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(Collection<?> types) {
    return annotation -> types.stream()
            .map(type -> type instanceof Class ? ((Class<?>) type).getName() : type.toString())
            .anyMatch(typeName -> typeName.equals(annotation.getType().getName()));
  }

  /**
   * Create a new stateful, single use {@link Predicate} that matches only
   * the first run of an extracted value. For example,
   * {@code MergedAnnotationPredicates.firstRunOf(MergedAnnotation::distance)}
   * will match the first annotation, and any subsequent runs that have the
   * same distance.
   * <p>NOTE: This predicate only matches the first run. Once the extracted
   * value changes, the predicate always returns {@code false}. For example,
   * if you have a set of annotations with distances {@code [1, 1, 2, 1]} then
   * only the first two will match.
   *
   * @param valueExtractor function used to extract the value to check
   * @return a {@link Predicate} that matches the first run of the extracted
   * values
   */
  public static <A extends Annotation> Predicate<MergedAnnotation<A>> firstRunOf(
          Function<? super MergedAnnotation<A>, ?> valueExtractor) {

    return new FirstRunOfPredicate<>(valueExtractor);
  }

  /**
   * Create a new stateful, single use {@link Predicate} that matches
   * annotations that are unique based on the extracted key. For example
   * {@code MergedAnnotationPredicates.unique(MergedAnnotation::getType)} will
   * match the first time a unique type is encountered.
   *
   * @param keyExtractor function used to extract the key used to test for
   * uniqueness
   * @return a {@link Predicate} that matches a unique annotation based on the
   * extracted key
   */
  public static <A extends Annotation, K> Predicate<MergedAnnotation<A>> unique(
          Function<? super MergedAnnotation<A>, K> keyExtractor) {

    return new UniquePredicate<>(keyExtractor);
  }


  /**
   * {@link Predicate} implementation used for
   * {@link MergedAnnotationPredicates#firstRunOf(Function)}.
   */
  private static class FirstRunOfPredicate<A extends Annotation> implements Predicate<MergedAnnotation<A>> {

    private final Function<? super MergedAnnotation<A>, ?> valueExtractor;

    private boolean hasLastValue;

    @Nullable
    private Object lastValue;

    FirstRunOfPredicate(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
      Assert.notNull(valueExtractor, "Value extractor must not be null");
      this.valueExtractor = valueExtractor;
    }

    @Override
    public boolean test(@Nullable MergedAnnotation<A> annotation) {
      if (!this.hasLastValue) {
        this.hasLastValue = true;
        this.lastValue = this.valueExtractor.apply(annotation);
      }
      Object value = this.valueExtractor.apply(annotation);
      return ObjectUtils.nullSafeEquals(value, this.lastValue);
    }
  }

  /**
   * {@link Predicate} implementation used for
   * {@link MergedAnnotationPredicates#unique(Function)}.
   */
  private static class UniquePredicate<A extends Annotation, K> implements Predicate<MergedAnnotation<A>> {

    private final Function<? super MergedAnnotation<A>, K> keyExtractor;

    private final HashSet<K> seen = new HashSet<>();

    UniquePredicate(Function<? super MergedAnnotation<A>, K> keyExtractor) {
      Assert.notNull(keyExtractor, "Key extractor must not be null");
      this.keyExtractor = keyExtractor;
    }

    @Override
    public boolean test(@Nullable MergedAnnotation<A> annotation) {
      K key = this.keyExtractor.apply(annotation);
      return this.seen.add(key);
    }
  }

}
