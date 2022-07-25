/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.validation.beanvalidation;

import java.lang.annotation.Annotation;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * A filter for excluding types from method validation.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FilteredMethodValidationPostProcessor
 * @since 4.0
 */
public interface MethodValidationExcludeFilter {

  /**
   * Evaluate whether to exclude the given {@code type} from method validation.
   *
   * @param type the type to evaluate
   * @return {@code true} to exclude the type from method validation, otherwise
   * {@code false}.
   */
  boolean isExcluded(Class<?> type);

  /**
   * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
   * classes by annotation.
   *
   * @param annotationType the annotation to check
   * @return a {@link MethodValidationExcludeFilter} instance
   */
  static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType) {
    return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
  }

  /**
   * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
   * classes by annotation.
   *
   * @param annotationType the annotation to check
   * @param searchStrategy the annotation search strategy
   * @return a {@link MethodValidationExcludeFilter} instance
   */
  static MethodValidationExcludeFilter byAnnotation(
          Class<? extends Annotation> annotationType, SearchStrategy searchStrategy) {
    return type -> MergedAnnotations.from(type, searchStrategy).isPresent(annotationType);
  }

}
