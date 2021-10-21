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
import java.util.function.Predicate;

/**
 * {@link MergedAnnotationSelector} implementations that provide various options
 * for {@link MergedAnnotation} instances.
 *
 * @author Phillip Webb
 * @see MergedAnnotations#get(Class, Predicate, MergedAnnotationSelector)
 * @see MergedAnnotations#get(String, Predicate, MergedAnnotationSelector)
 * @since 4.0
 */
public abstract class MergedAnnotationSelectors {

  private static final MergedAnnotationSelector<?> NEAREST = new Nearest();
  private static final MergedAnnotationSelector<?> FIRST_DIRECTLY_DECLARED = new FirstDirectlyDeclared();

  /**
   * Select the nearest annotation, i.e. the one with the lowest distance.
   *
   * @return a selector that picks the annotation with the lowest distance
   */
  @SuppressWarnings("unchecked")
  public static <A extends Annotation> MergedAnnotationSelector<A> nearest() {
    return (MergedAnnotationSelector<A>) NEAREST;
  }

  /**
   * Select the first directly declared annotation when possible. If no direct
   * annotations are declared then the nearest annotation is selected.
   *
   * @return a selector that picks the first directly declared annotation whenever possible
   */
  @SuppressWarnings("unchecked")
  public static <A extends Annotation> MergedAnnotationSelector<A> firstDirectlyDeclared() {
    return (MergedAnnotationSelector<A>) FIRST_DIRECTLY_DECLARED;
  }

  /**
   * {@link MergedAnnotationSelector} to select the nearest annotation.
   */
  private static class Nearest implements MergedAnnotationSelector<Annotation> {

    @Override
    public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
      return annotation.getDistance() == 0;
    }

    @Override
    public MergedAnnotation<Annotation> select(
            MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

      if (candidate.getDistance() < existing.getDistance()) {
        return candidate;
      }
      return existing;
    }

  }


  /**
   * {@link MergedAnnotationSelector} to select the first directly declared
   * annotation.
   */
  private static class FirstDirectlyDeclared implements MergedAnnotationSelector<Annotation> {

    @Override
    public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
      return annotation.getDistance() == 0;
    }

    @Override
    public MergedAnnotation<Annotation> select(
            MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

      if (existing.getDistance() > 0 && candidate.getDistance() == 0) {
        return candidate;
      }
      return existing;
    }

  }

}
