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

package infra.context.annotation;

import infra.core.type.filter.AnnotationTypeFilter;
import infra.core.type.filter.AspectJTypeFilter;
import infra.core.type.filter.AssignableTypeFilter;
import infra.core.type.filter.RegexPatternTypeFilter;
import infra.core.type.filter.TypeFilter;

/**
 * Enumeration of the type filters that may be used in conjunction with
 * {@link ComponentScan @ComponentScan}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ComponentScan
 * @see ComponentScan#includeFilters()
 * @see ComponentScan#excludeFilters()
 * @see TypeFilter
 * @since 4.0
 */
public enum FilterType {

  /**
   * Filter candidates marked with a given annotation.
   *
   * @see AnnotationTypeFilter
   */
  ANNOTATION,

  /**
   * Filter candidates assignable to a given type.
   *
   * @see AssignableTypeFilter
   */
  ASSIGNABLE_TYPE,

  /**
   * Filter candidates matching a given regex pattern.
   *
   * @see RegexPatternTypeFilter
   */
  REGEX,
  /**
   * Filter candidates matching a given AspectJ type pattern expression.
   *
   * @see AspectJTypeFilter
   */
  ASPECTJ,

  /**
   * Filter candidates using a given custom
   * {@link TypeFilter} implementation.
   */
  CUSTOM

}
