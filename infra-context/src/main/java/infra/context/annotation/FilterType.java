/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
