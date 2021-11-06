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

import cn.taketoday.lang.Nullable;

/**
 * Callback interface used to process annotations.
 *
 * @param <C> the context type
 * @param <R> the result type
 * @author Phillip Webb
 * @see AnnotationsScanner
 * @see TypeMappedAnnotations
 * @since 4.0
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {

  /**
   * Called when an aggregate is about to be processed. This method may return
   * a {@code non-null} result to short-circuit any further processing.
   *
   * @param context the context information relevant to the processor
   * @param aggregateIndex the aggregate index about to be processed
   * @return a {@code non-null} result if no further processing is required
   */
  @Nullable
  default R doWithAggregate(C context, int aggregateIndex) {
    return null;
  }

  /**
   * Called when an array of annotations can be processed. This method may
   * return a {@code non-null} result to short-circuit any further processing.
   *
   * @param context the context information relevant to the processor
   * @param aggregateIndex the aggregate index of the provided annotations
   * @param source the original source of the annotations, if known
   * @param annotations the annotations to process (this array may contain
   * {@code null} elements)
   * @return a {@code non-null} result if no further processing is required
   */
  @Nullable
  R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

  /**
   * Get the final result to be returned. By default this method returns
   * the last process result.
   *
   * @param result the last early exit result, or {@code null} if none
   * @return the final result to be returned to the caller
   */
  @Nullable
  default R finish(@Nullable R result) {
    return result;
  }

}
