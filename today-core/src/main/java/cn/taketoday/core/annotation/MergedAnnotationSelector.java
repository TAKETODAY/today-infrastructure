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

/**
 * Strategy interface used to select between two {@link MergedAnnotation}
 * instances.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @see MergedAnnotationSelectors
 * @since 4.0
 */
@FunctionalInterface
public interface MergedAnnotationSelector<A extends Annotation> {

  /**
   * Determine if the existing annotation is known to be the best
   * candidate and any subsequent selections may be skipped.
   *
   * @param annotation the annotation to check
   * @return {@code true} if the annotation is known to be the best candidate
   */
  default boolean isBestCandidate(MergedAnnotation<A> annotation) {
    return false;
  }

  /**
   * Select the annotation that should be used.
   *
   * @param existing an existing annotation returned from an earlier result
   * @param candidate a candidate annotation that may be better suited
   * @return the most appropriate annotation from the {@code existing} or
   * {@code candidate}
   */
  MergedAnnotation<A> select(MergedAnnotation<A> existing, MergedAnnotation<A> candidate);

}
